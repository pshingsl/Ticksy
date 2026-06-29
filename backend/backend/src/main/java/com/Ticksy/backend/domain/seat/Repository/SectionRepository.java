package com.Ticksy.backend.domain.seat.Repository;

import com.Ticksy.backend.domain.concert.Entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SectionRepository
        extends JpaRepository<SectionEntity, Long> {

    // sections + seats 한 번에 fetch join
    @Query("SELECT s FROM SectionEntity s " +
            "JOIN FETCH s.seats " +
            "WHERE s.schedule.scheduleId = :scheduleId")
    List<SectionEntity> findWithSeatsByScheduleId(
            @Param("scheduleId") Long scheduleId
    );
}