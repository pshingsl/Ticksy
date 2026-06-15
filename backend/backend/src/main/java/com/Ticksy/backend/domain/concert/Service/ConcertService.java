package com.Ticksy.backend.domain.concert.Service;

import com.Ticksy.backend.domain.concert.DTO.Response.*;
import com.Ticksy.backend.domain.concert.Entity.ConcertEntity;
import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import com.Ticksy.backend.domain.concert.Entity.SectionEntity;
import com.Ticksy.backend.domain.concert.Repository.ConcertRepository;
import com.Ticksy.backend.domain.concert.Repository.EventScheduleRepository;
import com.Ticksy.backend.domain.concert.Repository.VenueRepository;
import com.Ticksy.backend.domain.seat.enums.SeatStatus;
import com.Ticksy.backend.global.exception.CustomException;
import com.Ticksy.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private ConcertRepository concertRepository;
    private EventScheduleRepository eventScheduleRepository;
    private VenueRepository venueRepository;

    // 공연 목록 조회
    public ConcertListPageResponse getConcertList(
            LocalDate date, String region, Pageable pageable
    ) {
        Page<ConcertEntity> concertPage = concertRepository.findAllWithFilters(date, region, pageable);

        Page<ConcertListResponse> responsePage = concertPage.map(concert -> {

            // 가장 빠른 회차
            LocalDate earliestDate = concert.getSchedules().stream()
                    .map(EventScheduleEntity::getEventDate)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            // 가격 범위(구역 가격 기준)
            List<Integer> prices = concert.getSchedules().stream()
                    .flatMap(s -> s.getSections().stream())
                    .map(SectionEntity::getPrice)
                    .toList();

            Integer minPrice = prices.stream()
                    .min(Comparator.naturalOrder()).orElse(null);

            Integer maxPrice = prices.stream()
                    .max(Comparator.naturalOrder()).orElse(0);

            // 잔여 좌석 여부 (회차 중 하나라도 AVAILABLE 좌석 존재)
            boolean hasAvailableSeat = concert.getSchedules().stream()
                    .flatMap(s -> s.getSections().stream())
                    .flatMap((sec -> sec.getSeats().stream()))
                    .anyMatch(seat -> seat.getStatus() == SeatStatus.AVAILABLE);

            return ConcertListResponse.of(concert, earliestDate, minPrice, maxPrice, hasAvailableSeat);
        });

        return ConcertListPageResponse.of(responsePage);
    }

    // 공연 검색
    public ConcertListPageResponse searchConcert(String keyword, Pageable pageable) {
        Page<ConcertEntity> concertPage = concertRepository.searchByKeyword(keyword, pageable);

        Page<ConcertListResponse> responsePage = concertPage.map(concert -> {

            LocalDate earliestDat = concert.getSchedules().stream()
                    .map(EventScheduleEntity::getEventDate)
                    .min(Comparator.naturalOrder())
                    .orElse(null);

            List<Integer> prices = concert.getSchedules().stream()
                    .flatMap(s -> s.getSections().stream())
                    .map(SectionEntity::getPrice)
                    .toList();

            Integer minPrice = prices.stream()
                    .min(Comparator.naturalOrder()).orElse(0);

            Integer maxPrice = prices.stream()
                    .max(Comparator.naturalOrder()).orElse(0);

            boolean hasAvailableSeat = concert.getSchedules().stream()
                    .flatMap(s -> s.getSections().stream())
                    .flatMap(sec -> sec.getSeats().stream())
                    .anyMatch(seat -> seat.getStatus() == SeatStatus.AVAILABLE);

            return ConcertListResponse.of(concert, earliestDat, minPrice, maxPrice, hasAvailableSeat);
        });

        return ConcertListPageResponse.of(responsePage);
    }

    // 공연 상세 조회 (CON-03)
    public ConcertDetailResponse getConcertDetail(Long concertId) {

        ConcertEntity concert = concertRepository
                .findByConcertIdAndIsDeletedFalse(concertId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.NOT_FOUND_CONCERT));

        List<EventScheduleEntity> schedules = eventScheduleRepository.
                findByConcert_ConcertIdOrderByEventDateAscEventTimeAsc(concertId);

        // 회차별 응답 생성(잔여 좌석 수 포함)
        List<ScheduleResponse> scheduleResponses = schedules.stream()
                .map(schedule -> {
                    int remainingSeatCount = (int) schedule.getSections().stream()
                            .flatMap(sec -> sec.getSeats().stream())
                            .filter(seat -> seat.getStatus() == SeatStatus.AVAILABLE)
                            .count();

                    return ScheduleResponse.of(schedule, remainingSeatCount);
                })
                .toList();

        // 등급별 가격 (중복 제거, 첫 회차 기준)
        List<GradeResponse>  gradeResponses = schedules.stream()
                .flatMap(s -> s.getSections().stream())
                .map(sec -> GradeResponse.of(sec.getGrade(), sec.getPrice()))
                .distinct()
                .toList();

        return ConcertDetailResponse.of(concert, scheduleResponses, gradeResponses);
    }
}
