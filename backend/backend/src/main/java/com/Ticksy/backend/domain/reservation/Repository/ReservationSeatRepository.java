package com.Ticksy.backend.domain.reservation.Repository;

import com.Ticksy.backend.domain.reservation.Entity.ReservationSeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationSeatRepository extends JpaRepository<ReservationSeatEntity, Long> {

    // 예매에 포함된 좌석목록
    @Query("SELECT rs FROM ReservationSeatEntity rs " +
            "JOIN FETCH rs.seat s " +
            "JOIN FETCH s.section sec " +
            "WHERE rs.reservation.reservationId = :reservationId")
    List<ReservationSeatEntity> findWithSeatByReservationId(
            @Param("reservationId") Long reservationId
    );
}
