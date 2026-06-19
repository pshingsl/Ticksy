package com.Ticksy.backend.domain.seat.Repository;

import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<SeatEntity, Long> {

    // 특정 구역의 좌석 전체 조회
    List<SeatEntity> findBySection_SectionId(Long sectionId);

    // 좌석 ID 목록으로 조회 (선점/예매 시 사용)
    List<SeatEntity> findBySeatIdIn(List<Long> seatIds);
}
