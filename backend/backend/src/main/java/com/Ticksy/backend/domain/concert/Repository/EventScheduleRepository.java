package com.Ticksy.backend.domain.concert.Repository;

import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventScheduleRepository extends JpaRepository<EventScheduleEntity, Long> {

    // 특정 공연의 회차 목록(날짜순)
    List<EventScheduleEntity> findByConcert_ConcertIdOrderByEventDateAscEventTimeAsc(Long concertId);

    // 회차 단건 조회 (좌석 배치도 조회 시 사용 예정)
    Optional<EventScheduleEntity> findByScheduleIdAndConcert_ConcertId(
            Long scheduledId, Long concetId
    );

}
