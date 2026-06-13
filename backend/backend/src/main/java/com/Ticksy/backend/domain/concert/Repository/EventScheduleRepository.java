package com.Ticksy.backend.domain.concert.Repository;

import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventScheduleRepository extends JpaRepository<Long, EventScheduleEntity> {

    // 특정 공연의 회차 목록(날짜순)
    // select es.* from event_schedule es
    // Inner JOIN concert c ON es.concet_id = c.concert_id
    // where c.concert_id = ?
    // order by es.event_date asc, es.event_time asc;
    List<EventScheduleEntity> findByConcert_ConcertIdOrderByEventDateAscEventTimeAsc(Long concertId);

    // 회차 단건 조회 (좌석 배치도 조회 시 사용 예정)
    // SELECT es.*
    // FROM event_schedule es
    // INNER JOIN concert c ON es.concert_id = c.concert_id
    // WHERE es.schedule_id = ?
    // AND c.concert_id = ?;
    Optional<EventScheduleEntity> findByScheduleIdAndConcert_ConcertId(
            Long scheduledId, Long concetId
    );

}
