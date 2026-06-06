package com.Ticksy.backend.domain.concert.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "concerts")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ConcertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_id")
    private Long concertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueEntity venue;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "cast", nullable = true, length = 500)
    private String cast;

    @Column(name = "age_limit", nullable = true, length = 50)
    private String ageLimit;

    @Column(name = "run_time", nullable = true)
    private Integer runTime;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "poster_url", nullable = true, length = 500)
    private String posterUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isDeleted;

    @OneToMany(mappedBy = "concert", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventScheduleEntity> schedule = new ArrayList<>();

    // 삭제
    public void delete() {
        this.isDeleted = true;
    }

    // 공연 정보 수정
    public void update(String title, String cast, String ageLimit, Integer runTime, String description, String posterUrl) {
        if (title != null) this.title = title;
        if (cast != null) this.cast = cast;
        if (ageLimit != null) this.ageLimit = ageLimit;
        if (runTime != null) this.runTime = runTime;
        if (description != null) this.description = description;
        if (posterUrl != null) this.posterUrl = posterUrl;
    }
}
