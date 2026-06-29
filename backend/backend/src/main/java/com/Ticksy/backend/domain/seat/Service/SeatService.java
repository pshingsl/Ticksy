package com.Ticksy.backend.domain.seat.Service;

import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import com.Ticksy.backend.domain.concert.Entity.SectionEntity;
import com.Ticksy.backend.domain.concert.Repository.EventScheduleRepository;
import com.Ticksy.backend.domain.seat.DTO.Response.SeatHoldResponse;
import com.Ticksy.backend.domain.seat.DTO.Response.SeatItemResponse;
import com.Ticksy.backend.domain.seat.DTO.Response.SeatLayoutResponse;
import com.Ticksy.backend.domain.seat.DTO.Response.SectionWithSeatsResponse;
import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import com.Ticksy.backend.domain.seat.Repository.SeatRepository;
import com.Ticksy.backend.domain.seat.Repository.SectionRepository;
import com.Ticksy.backend.domain.seat.enums.SeatStatus;
import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public SeatLayoutResponse getSeatLayout(Long concertId, Long scheduleId) {

        EventScheduleEntity schedule = eventScheduleRepository.
                findByScheduleIdAndConcert_ConcertId(scheduleId, concertId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_SCHEDULE));

        // 예매 오픈 여부 확인
        if (!schedule.isBookingOpen()) {
            throw new CustomException(ErrorCode.BOOKING_NOT_OPEN_YET);
        }

        // schedule.getSections() 대신 fetch join으로 직접 조회
        List<SectionEntity> sections =
                sectionRepository.findWithSeatsByScheduleId(scheduleId);

        List<SectionWithSeatsResponse> sectionResponses =
                schedule.getSections().stream()
                        .map(section -> buildSectionResponse(scheduleId, section))
                        .toList();

        return SeatLayoutResponse.builder()
                .scheduleId(scheduleId)
                .bookingOpenAt(schedule.getBookingOpenAt())
                .sections(sectionResponses)
                .build();
    }

    private SectionWithSeatsResponse buildSectionResponse(Long scheduleId, SectionEntity section) {
        List<SeatItemResponse> seatItemResponses = section.getSeats().stream()
                .map(seat -> {
                    boolean isHolding = seatHoldService.isHolding(
                            scheduleId, seat.getSeatId()
                    );
                    return SeatItemResponse.of(seat, isHolding);
                })
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
                        new CustomException(ErrorCode.NOT_FOUND_USER));

        // 예매 오픈 여부 확인
        if (!schedule.isBookingOpen()) {
            new CustomException(ErrorCode.BOOKING_NOT_OPEN_YET);
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
