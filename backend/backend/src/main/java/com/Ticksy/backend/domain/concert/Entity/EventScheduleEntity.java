package com.Ticksy.backend.domain.concert.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_schedule",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_venue_date_time",
                columnNames = {"concert_id", "event_date", "event_time"}))
public class EventScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    /*
     *  @ManyToOne으로 짠 이유
     * - 하나의 콘서트(Concert)는 여러회차(날짜(Concert))를 가질 수 있다. -> 여러 회차는 같은 공연으로 참조하는 N:1 관계이다.
     * - 따라서 해당 관계에서 리스트로 담을 필요가 없다.
     * ㄴ 현재 EventSchedule 기준에서는 하나의 Concert만 참조하므로 객체 하나(@ManyToOne)로 선언한다.
     * ㄴ 반대로 현재 엔티티가 여러 객체를 가져야 하는 경우 List(@OneToMany) 컬렉션으로 관리한다.
     * ㄴ 현재 객체 기준으로 생각하기
     * - DB기준으로 보면 event_schedule에서 concert_id가 외래 키로 지정되어 있기 때문이다.
     *
     * - @JoinColumn은 외래키 컬럼명을 지정한다.
     * ㄴ 만약 JoinColumn을 사용하지 않는다면 JPA에서 기본 이름을 자동생성한다. -> 이상한 이름이 나올 수 있다.
     *
     * - LAZY을 사용하는 이유
     * - ManyToOne의 기본 fetch 전략은 EAGER(즉시 조회)이다. -> 나중에 데이터가 많을때 조회 시간 증가
     * ㄴ 불필요한 즉시 조회를 방지하기 위해서이다.
     * ㄴ 1. N+1 문제 발생. 2. 연관 객체의 불필요한 추가 조회 발생 가능. 3. 메모리 사용량 증가 가능
     * - LAZY는 실제 연관 객체를 사용하는 시점까지 조회를 미룬다.
     * - 이를 통해 불필요한 조회와 성능 문제를 줄일 수 있다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private ConcertEntity concert;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time", nullable = false)
    private LocalTime eventTime;

    @Column(name = "booking_open_at", nullable = false)
    private LocalDateTime bookingOpenAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // UPCOMING, OPEN, SOLD_OUT, CLOSED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    /*
    * OneToMany로 한 이유
    * - 하나의 회차는 여러 구역을 가진다.
    * - 따라서 구역을 여러개를 담아야 하기 때문애 리스트로 저장  == 하나의 회차는 여러 구역을 가지므로 List 컬렉션으로 관리한다.
    * ㄴ 현재 객체 기준으로 생각하기
    * - DB에서 sections테이블에 외래키로 schedule_id가 참조됨
    *
    * - mappedBy: 양방향 관계를 맺을때 사용.
    * - mappedBy는 연관관계의 주인이 아님을 의미한다.
    * - 주인의 매핑을 참조(읽기)만 가능하다.
    * - 실제 FK 관리(연관관계 관리)는 상대 엔티티(SectionEntity)가 담당한다.
    *- mappedBy = "schedule" 은 SectionEntity의 schedule 필드와 매핑됨을 의미한다.
    *
    * - @Builder.Default
    * - 일반 빌더를 사용하면 초기화 무시되는 문제가 생긴다.
    * - 즉, EventScheduleEntity.builder().build();이면 sections == null로 처리될 수 있다.
    * */
    @OneToMany(mappedBy = "schedule", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SectionEntity> sections = new ArrayList<>();

    // 예매 오픈 여부 확인
    public boolean isBookingOpen() {
        return LocalDateTime.now().isAfter(bookingOpenAt) ;
    }

    // 상태 변경
    public void updateStatus(String status) {
        this.status = status;
    }
}
