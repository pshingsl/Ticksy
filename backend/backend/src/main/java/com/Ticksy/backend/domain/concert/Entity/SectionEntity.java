package com.Ticksy.backend.domain.concert.Entity;

import com.Ticksy.backend.domain.concert.enums.SeatGrade;
import com.Ticksy.backend.domain.seat.Entity.SeatEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
@Entity
// TODO: 부하 테스트 후 인덱스 적용해서 비교하기
@Table(name = "sections")
public class SectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long sectionId;

    // 하나의 회차에는 여러 좌석을 가진다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private EventScheduleEntity schedule;

    @Column(name = "name", nullable = false, length = 50)
    private String name; // VIP구역, R구역 등

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 20)
    private SeatGrade grade; // VIP, R, S, GENERAL

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount;

    @Column(name = "col_count", nullable = false)
    private Integer colCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    // 하나의 구역에는 여러 좌석이 존재한다.
    @OneToMany(mappedBy = "section", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SeatEntity> seat = new ArrayList<>();
}
