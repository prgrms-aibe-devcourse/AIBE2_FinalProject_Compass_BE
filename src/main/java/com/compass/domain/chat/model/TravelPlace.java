package com.compass.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 여행지 정보 모델 (Stage 1에서 수집되는 데이터)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelPlace {
    private String placeId;           // 고유 ID (Google Place ID 등)
    private String name;              // 장소명
    private String category;          // 카테고리 (관광지, 맛집, 카페 등)
    private Double latitude;          // 위도
    private Double longitude;         // 경도
    private String address;           // 주소
    private String description;       // 설명

    // Google Places API 추가 필드
    private Double rating;            // 평점 (1.0 ~ 5.0)
    private Integer reviewCount;      // 리뷰 수
    private Integer priceLevel;       // 가격대 (0 ~ 4)
    private String photoUrl;          // 대표 사진 URL
    private boolean openNow;          // 현재 영업 여부
    private String phoneNumber;       // 전화번호
    private String website;           // 웹사이트

    // 품질 점수 계산 (평점 70% + 리뷰수 30%)
    public double getQualityScore() {
        if (rating == null || reviewCount == null) {
            return 0.0;
        }

        // 평점 정규화 (0-5 -> 0-1)
        double normalizedRating = rating / 5.0;

        // 리뷰 수 로그 스케일 정규화
        double normalizedReviews = Math.log10(reviewCount + 1) / 4.0;
        normalizedReviews = Math.min(normalizedReviews, 1.0);

        // 가중치 적용
        return (normalizedRating * 0.7) + (normalizedReviews * 0.3);
    }

    // 신뢰도 레벨 반환
    public String getReliabilityLevel() {
        if (reviewCount == null || reviewCount == 0) {
            return "정보없음";
        }

        if (reviewCount >= 1000 && rating >= 4.0) {
            return "매우높음";
        } else if (reviewCount >= 500 && rating >= 3.5) {
            return "높음";
        } else if (reviewCount >= 100 && rating >= 3.0) {
            return "보통";
        } else {
            return "낮음";
        }
    }
}