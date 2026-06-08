package com.Ticksy.backend.domain.payment.Entity;

import com.Ticksy.backend.domain.payment.enums.PaymentStatus;
import com.Ticksy.backend.domain.reservation.Entity.ReservationEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "payments")
// TODO: 부하 테스트 후 인덱스 적용 후 비교하기
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    // 예매 하나에는 한번에 결제가 된다.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;

    @Column(name = "toss_payment_key", nullable = false, unique = true, length = 200)
    private String tossPaymentKey;

    @Column(name = "amount", nullable = false)
    private  Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status; // DONE, CANCELLED, PARTIAL_CANCELLED

    @Column(name = "paid_at", nullable = true)
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at", nullable = true)
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    // 전액 취소
    public void cancel(LocalDateTime cancelledAt) {
        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    // 부분 취소(70% 환불)
    public void partialCancel(LocalDateTime cancelledAt) {
        this.status = PaymentStatus.PARTIAL_CANCELLED;
        this.cancelledAt = cancelledAt;
    }
}
