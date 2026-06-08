package com.Ticksy.backend.domain.reservation.Entity;

import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
// TODO: 부하 테스트 후 인덱스 적용 후 비교
@Table(name = "reservation_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reservation_seat",
                columnNames = {"reservation_id", "seat_id"}
        ))
public class ReservationSeatEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_seat_id")
    private Long reservationSeatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private SeatEntity seat;

    @Column(name = "price", nullable = false)
    private Integer price; // 결제 당시 좌석 가격

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
