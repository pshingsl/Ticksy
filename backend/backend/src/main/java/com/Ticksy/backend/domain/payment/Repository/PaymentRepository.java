package com.Ticksy.backend.domain.payment.Repository;

import com.Ticksy.backend.domain.payment.Entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByReservation_ReservationId(Long reservationId);
}
