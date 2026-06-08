package com.Ticksy.backend.domain.reservation.Entity;

import com.Ticksy.backend.domain.concert.Entity.EventScheduleEntity;
import com.Ticksy.backend.domain.user.Entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Entity
// TODO: 부하테스트 후 인덱스 적용 후 비교하기
@Table(name = "reservations")
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    // 한명의 유저는 여러 개의 티켓을 예매할 수 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // 한 회차에는 여러 개의 티켓을 가질 수 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private EventScheduleEntity schedule;

    @Column(name = "reservation_code", nullable = false, length = 50)
    private String reservationCode; // TK-날짜-순번

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // CONFIRMED, CANCELLED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    // 양방향 관계
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReservationSeatEntity> reservationSeat = new ArrayList<>();

    // 예매 취소
    public void cancel() {
        this.status = "CANCELLED";
    }
}
