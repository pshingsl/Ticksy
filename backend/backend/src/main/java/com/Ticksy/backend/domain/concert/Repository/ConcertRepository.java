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
    // sql -> SELECT DISTINCT c.* FROM concerts c
    // JOIN venues v ON c.venue_id = v.venue_id
    // LEFT JOIN event_schedule s on c.concert_id = s.concert_id
    // WHERE c.is_deltedE = false
    // -- 날짜가 파라미터로 들어오지 않으면(NULL) 패스, 들어오면 일치하는 날짜만 필터링
    // AND (? IS NULL OR s.event_date = ?)
    // -- 지역명이 파라미터로 들어오지 않으면 패스, 들어오면 LIKE 검색
    //  AND (? IS NULL OR v.address LIKE CONCAT('%', ?, '%'));
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
    // sql -> SELECT * FROM concerts WHERE (is_deleted = false AND title LIKE '%A%' OR (cast LIKE '%A%');
    // is_deleted = FALSE가 들어간 이유는 이게 없으면 삭제된 공연 까지 조회가 된다.
    // 따라서 검색시 예정 또는 현재 모집 중인데 이터를 가져와야한다.
    // ()로 구분하는 이유 SQL에서도 연산자간 우선순위가 존재하기 때문에 ()로 구분해줘야 한다 안그러면 버그가 발생한다.
    // '%A%' 알파벳 A 또는 키워드라는 글자를 임시로 집어넣은 것 런타임시 검색에 맞는 데이터 가져옴
    @Query("SELECT c FROM ConcertEntity c " +
            "WHERE c.isDeleted = false " +
            "AND (c.title LIKE %:keyword% OR c.cast LIKE %:keyword%)")
    Page<ConcertEntity> searchByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}

