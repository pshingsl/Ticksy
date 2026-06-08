package com.Ticksy.backend.domain.seat.Entity;

import com.Ticksy.backend.domain.concert.Entity.SectionEntity;
import com.Ticksy.backend.domain.seat.enums.SeatStatus;
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
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(
        name = "uk_section_row_col",
        columnNames = {"section_id", "row_num", "col_num"}
))
public class SeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    // 하나의 구역에는 여러 좌석이 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private SectionEntity section;

    @Column(name = "row_num", nullable = false)
    private Integer rowNum;

    @Column(name = "col_num", nullable = false)
    private Integer colNum;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatStatus status; // AVAILABLE, RESERVED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    // 예매 완료 상태 변경
    public void reserve() {
        this.status = SeatStatus.RESERVED;
    }

    // 예매 취소 상태 변경
    public void release() {
        this.status = SeatStatus.AVAILABLE;
    }
}
