package com.Ticksy.backend.domain.reservation.Repository;

import com.Ticksy.backend.domain.reservation.Entity.ReservationEntity;
import com.Ticksy.backend.domain.reservation.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    // 마이페이지 예매 내역 조회
    List<ReservationEntity> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    // 예매 상세 조회(본인 확인 포함)
    Optional<ReservationEntity> findByReservationIdAndUser_UserId(Long reservationId, Long userId);

    // 확정된 예매 내역 존재 여부(탈퇴 시 사용)
    boolean existsByUser_UserIdAndStatus(Long userId, ReservationStatus status);

    // 예매번호 마지막 순번 조회(예매생성 번호)
    @Query("SELECT COUNT(r) FROM ReservationEntity r " +
            "WHERE r.reservationCode LIKE :prefix%")
    Long countByReservationCodePrefix(@Param("prefix") String prefix);
}
