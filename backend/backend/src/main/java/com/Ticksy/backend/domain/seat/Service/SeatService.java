package com.Ticksy.backend.domain.seat.Service;

import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import com.Ticksy.backend.domain.concert.Entity.SectionEntity;
import com.Ticksy.backend.domain.concert.Repository.EventScheduleRepository;
import com.Ticksy.backend.domain.concert.Repository.SectionRepository;
import com.Ticksy.backend.domain.seat.DTO.Response.*;
import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import com.Ticksy.backend.domain.seat.Repository.SeatRepository;
import com.Ticksy.backend.domain.seat.enums.SeatStatus;
import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import com.Ticksy.backend.domain.seat.DTO.Response.SeatHoldResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatService {
    private final EventScheduleRepository eventScheduleRepository;
    private final SeatRepository seatRepository;
    private final SectionRepository sectionRepository;
    private final SeatHoldService seatHoldService;

    private static final int MAX_SEAT_COUNT = 4;

    // 좌석 배치도 조회
    public SeatLayoutResponse getSeatLayout(
            Long concertId, Long scheduleId, Long sectionId
    ) {
        // [PERF] 구간별 시간 측정 (동작은 기존과 동일, 측정 끝나면 제거하거나 Micrometer로 교체)
        StopWatch sw = new StopWatch("seatLayout");

        sw.start("1. 회차 조회 (DB)");
        EventScheduleEntity schedule = eventScheduleRepository
                .findByScheduleIdAndConcert_ConcertId(scheduleId, concertId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_SCHEDULE));
        sw.stop();

        if (!schedule.isBookingOpen()) {
            throw new CustomException(ErrorCode.BOOKING_NOT_OPEN_YET);
        }

        sw.start("2. 구역+좌석 조회 (DB, fetch join)");
        List<SectionEntity> sections;

        if (sectionId != null) {
            // 방식1: 특정 구역만 조회
            sections = sectionRepository
                    .findWithSeatsBySectionId(scheduleId, sectionId);
        } else {
            // 기존: 전체 구역 조회 (하위 호환)
            sections = sectionRepository
                    .findWithSeatsByScheduleId(scheduleId);
        }
        int seatCount = sections.stream().mapToInt(s -> s.getSeats().size()).sum();
        sw.stop();

        // 선점 상태를 MGET으로 한 번에 조회 (기존: 좌석마다 hasKey 1번 → 좌석 1만 개면 왕복 1만 번)
        sw.start("3. 선점 상태 조회 (Redis, MGET 1회)");
        List<Long> seatIds = sections.stream()
                .flatMap(section -> section.getSeats().stream())
                .map(SeatEntity::getSeatId)
                .toList();
        Map<Long, Boolean> holdingBySeatId =
                seatHoldService.getHoldingStatus(scheduleId, seatIds);
        sw.stop();

        sw.start("4. DTO 변환");
        List<SectionWithSeatsResponse> sectionResponses = sections.stream()
                .map(section -> buildSectionResponse(section, holdingBySeatId))
                .toList();
        sw.stop();

        // 전체 구역 목록 (탭 표시용 - 좌석 없이 구역 정보만)
        sw.start("5. 탭용 구역 목록 조회 (DB)");
        List<SectionEntity> allSections =
                sectionRepository.findBySchedule_ScheduleId(scheduleId);
        sw.stop();

        log.info("[PERF] seatLayout scheduleId={} sectionId={} seats={} redisCalls={} serviceMs={}\n{}",
                scheduleId, sectionId, seatCount, seatIds.isEmpty() ? 0 : 1,
                sw.getTotalTimeMillis(), sw.prettyPrint(TimeUnit.MILLISECONDS));

        return SeatLayoutResponse.builder()
                .scheduleId(scheduleId)
                .bookingOpenAt(schedule.getBookingOpenAt())
                .sections(sectionResponses)
                .allSections(allSections.stream()
                        .map(s -> SectionTabResponse.of(s))
                        .toList())
                .build();
    }

    private SectionWithSeatsResponse buildSectionResponse(
            SectionEntity section, Map<Long, Boolean> holdingBySeatId
    ) {
        List<SeatItemResponse> seatItemResponses = section.getSeats().stream()
                .map(seat -> SeatItemResponse.of(
                        seat,
                        holdingBySeatId.getOrDefault(seat.getSeatId(), false)
                ))
                .toList();

        return SectionWithSeatsResponse.builder()
                .sectionId(section.getSectionId())
                .name(section.getName())
                .grade(section.getGrade())
                .price(section.getPrice())
                .seats(seatItemResponses)
                .build();
    }

    // 좌석 선점 요청
    @Transactional
    public SeatHoldResponse holdSeats(
            Long scheduleId, List<Long> seatIds, Long userId
    ) {
        // 1인당 최대 4석까지 허용
        if (seatIds.size() > MAX_SEAT_COUNT) {
            throw new CustomException(ErrorCode.EXCEED_MAX_SEAT_COUNT);
        }

        EventScheduleEntity schedule = eventScheduleRepository
                .findById(scheduleId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_SCHEDULE));

        // 예매 오픈 여부 확인
        if (!schedule.isBookingOpen()) {
            throw new CustomException(ErrorCode.BOOKING_NOT_OPEN_YET);
        }

        List<SeatEntity> seats = seatRepository.findBySeatIdIn(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new CustomException(ErrorCode.NOT_FOUND_SEAT);
        }

        // 이미 예매 완료된 좌석 확인
        for (SeatEntity seat : seats) {
            if (seat.getStatus() == SeatStatus.RESERVED) {
                throw new CustomException(ErrorCode.ALREADY_RESERVED_SEAT);
            }
        }

        List<Long> heldSeatIds = new ArrayList<>();

        try {
            for (Long seatId : seatIds) {
                boolean success = seatHoldService.tryHoldSeat(
                        scheduleId, seatId, userId
                );

                if (!success) {
                    // 실패시 지금까지 선점한 좌석 롤백
                    rollbackHolds(scheduleId, heldSeatIds);
                    throw new CustomException(ErrorCode.ALREADY_HELD_SEAT);
                }

                heldSeatIds.add(seatId);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            rollbackHolds(scheduleId, heldSeatIds);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(5);

        log.info("좌석 선점 성공: schedule={}, seats={}, user={}",
                scheduleId, heldSeatIds, userId);

        return SeatHoldResponse.builder()
                .heldSeatId(heldSeatIds)
                .expiredAt(expiredAt)
                .build();
    }

    // 선점 실패 시 이미 선점된 좌석들 롤백
    private void rollbackHolds(Long scheduleId, List<Long> heldSeatIds) {
        for (Long seatId : heldSeatIds) {
            seatHoldService.cancelHold(scheduleId, seatId);
        }
    }

    // 좌석 선점 취소
    public void cancelHold(Long scheduleId, List<Long> seatIds, Long userId) {
        for (Long seatId : seatIds) {
            // 본인이 선점한 좌석만 취소 가능
            if (seatHoldService.isHeldByUser(scheduleId, seatId, userId)) {
                seatHoldService.cancelHold(scheduleId, seatId);
            }
        }
        log.info("좌석 선점 취소: schedule={}, seats={}, user={}",
                scheduleId, seatIds, userId);
    }

}
