package com.Ticksy.backend.domain.concert.Repository;

import com.Ticksy.backend.domain.concert.Entity.ConcertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ConcertRepository extends JpaRepository<Long, ConcertEntity> {

    // 공연 상세 조회
    // select * from concert where concertId =? and is_deleted = false;
    Optional<ConcertEntity> findByConcertIdAndIsDeletedFalse(Long concertId);

    // 공연 목록 조회(날짜/지역 필터 + 정렬)
    // select distinct c.* from concert c INNERT JOIN venue v on c.venue_id = v.venue_id
    // LEFT JOIN c.event_schedule s ON concert_id = s.concert_id
    // WHERE c.is_deleted = false
    // AND (? IS NULL OR s.event_date = ?)
    // AND (? IS NULL OR v.address LIKE CONCAT('%', ?, '%'))
    // LIMIT ? OFFSET ?;
    @Query("SELECT DISTINCT c FROM ConcertEntity c " +
            "JOIN c.venue v " +
            "LEFT JOIN c.schedules s " +
            "WHERE c.isDeleted = false " +
            "AND (:date IS NULL OR s.eventDate = :date) " +
            "AND (:region IS NULL OR v.address LIKE %:region%)")
    Page<ConcertEntity> findAllWithFilters(
            @Param("date") LocalDate date,
            @Param("region") String region,
            Pageable pageable
    );

    // 공연 검색(공연명 또는 출연진)
    // select c.* from concert c
    // where c.isdeleted = false
    // AND (c.title LIKE CONCAT('%', ?, '%') OR c.cast LIKE CONCAT('%', ?, '%'))
    // LIMIT ? OFFSET ?;
    @Query("SELECT c FROM ConcertEntity c " +
            "WHERE c.isDeleted = false " +
            "AND (c.title LIKE %:keyword% OR c.cast LIKE %:keyword%)")
    Page<ConcertEntity> serchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

