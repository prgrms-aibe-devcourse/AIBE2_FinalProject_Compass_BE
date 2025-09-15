package com.compass.domain.trip.entity;

import com.compass.common.entity.BaseEntity;
import com.compass.domain.trip.Trip;
import com.compass.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * 장소 리뷰 엔티티
 * 사용자가 방문한 장소에 대한 리뷰 및 평점 관리
 */
@Getter
@Entity
@Table(name = "place_reviews", indexes = {
    @Index(name = "idx_review_place", columnList = "place_id"),
    @Index(name = "idx_review_user", columnList = "user_id"),
    @Index(name = "idx_review_trip", columnList = "trip_id"),
    @Index(name = "idx_review_rating", columnList = "rating"),
    @Index(name = "idx_review_visit_date", columnList = "visit_date")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaceReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;  // Nullable - 여행과 연결되지 않은 리뷰도 가능

    @Column(nullable = false)
    private Integer rating;  // 1-5 평점

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    // 리뷰 이미지 URL 배열
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String images;  // JSON Array of image URLs

    // 리뷰 태그
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tags;  // JSON Array: ["맛있어요", "친절해요", "가성비좋아요"]

    // 리뷰 도움이 됨 수
    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    // 리뷰 신고 수
    @Column(name = "report_count")
    @Builder.Default
    private Integer reportCount = 0;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;  // 실제 방문 확인됨

    @Column(name = "is_visible")
    @Builder.Default
    private Boolean isVisible = true;  // 노출 여부

    // 세부 평점 (선택사항)
    @Column(name = "taste_rating")
    private Integer tasteRating;  // 맛 평점 (음식점용)

    @Column(name = "service_rating")
    private Integer serviceRating;  // 서비스 평점

    @Column(name = "price_rating")
    private Integer priceRating;  // 가격 평점

    @Column(name = "ambience_rating")
    private Integer ambienceRating;  // 분위기 평점

    @Column(name = "cleanliness_rating")
    private Integer cleanlinessRating;  // 청결도 평점

    // 편의 메서드
    public void incrementHelpful() {
        this.helpfulCount++;
    }

    public void incrementReport() {
        this.reportCount++;
        // 신고가 일정 수 이상이면 자동으로 숨김 처리
        if (this.reportCount >= 5) {
            this.isVisible = false;
        }
    }

    public void markAsVerified() {
        this.isVerified = true;
    }

    public void hide() {
        this.isVisible = false;
    }

    public void show() {
        this.isVisible = true;
    }

    public void updateReview(String reviewText, Integer rating) {
        this.reviewText = reviewText;
        this.rating = rating;
    }

    // 평균 세부 평점 계산
    public Double getAverageDetailRating() {
        int count = 0;
        int sum = 0;

        if (tasteRating != null) {
            sum += tasteRating;
            count++;
        }
        if (serviceRating != null) {
            sum += serviceRating;
            count++;
        }
        if (priceRating != null) {
            sum += priceRating;
            count++;
        }
        if (ambienceRating != null) {
            sum += ambienceRating;
            count++;
        }
        if (cleanlinessRating != null) {
            sum += cleanlinessRating;
            count++;
        }

        return count > 0 ? (double) sum / count : null;
    }
}