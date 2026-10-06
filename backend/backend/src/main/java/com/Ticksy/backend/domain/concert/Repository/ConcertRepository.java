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
public interface ConcertRepository extends JpaRepository<ConcertEntity, Long> {

    // 공연 상세 조회
    Optional<ConcertEntity> findByConcertIdAndIsDeletedFalse(Long concertId);

    // 공연 목록 조회(날짜/지역 필터 + 정렬)
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
    @Query("SELECT c FROM ConcertEntity c " +
            "WHERE c.isDeleted = false " +
            "AND (c.title LIKE %:keyword% OR c.cast LIKE %:keyword%)")
    Page<ConcertEntity> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

