package com.compass.domain.chat.route_optimization.entity;

import com.compass.domain.chat.entity.ChatThread;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travel_itineraries")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class TravelItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private ChatThread thread;

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer totalDays;

    @Column(name = "optimization_strategy", length = 50)
    private String optimizationStrategy;  // DISTANCE, TIME, BALANCED

    @Column(name = "transport_mode", length = 50)
    private String transportMode;  // CAR, PUBLIC_TRANSPORT, WALKING

    @Column(name = "accommodation_address", columnDefinition = "TEXT")
    private String accommodationAddress;

    @Column(name = "total_distance")
    private Double totalDistance;  // 전체 이동 거리 (km)

    @Column(name = "total_duration")
    private Integer totalDuration;  // 전체 소요 시간 (분)

    @Column(name = "is_final", nullable = false)
    @Builder.Default
    private Boolean isFinal = false;  // 최종 확정 여부

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;  // 활성 상태

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TravelPlace> places = new ArrayList<>();

    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TravelPlaceCandidate> candidates = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 편의 메서드
    public void addPlace(TravelPlace place) {
        places.add(place);
        place.setItinerary(this);
    }

    public void addCandidate(TravelPlaceCandidate candidate) {
        candidates.add(candidate);
        candidate.setItinerary(this);
    }

    public void removePlace(TravelPlace place) {
        places.remove(place);
        place.setItinerary(null);
    }

    public void finalize() {
        this.isFinal = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}