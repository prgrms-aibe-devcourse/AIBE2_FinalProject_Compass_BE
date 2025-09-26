package com.compass.domain.chat.common.utils;

import com.compass.domain.chat.common.constants.StageConstants;
import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.TravelPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// 장소 점수 계산 유틸리티 클래스
@Component
@RequiredArgsConstructor
public class PlaceScoreCalculator {

    private final DistanceCalculator distanceCalculator;

    // 기본 장소 점수 계산 (리뷰수 + 평점)
    public double calculateBaseScore(TravelPlace place) {
        if (place == null) {
            return 0.0;
        }

        double reviewScore = calculateReviewScore(place.getReviewCount());
        double ratingScore = calculateRatingScore(place.getRating());

        // 리뷰 50%, 평점 50%
        return (reviewScore * 0.5) + (ratingScore * 0.5);
    }

    // TravelCandidate 점수 계산
    public double calculateBaseScore(TravelCandidate candidate) {
        if (candidate == null) {
            return 0.0;
        }

        double reviewScore = calculateReviewScore(candidate.getReviewCount());
        double ratingScore = calculateRatingScore(candidate.getRating());

        // 리뷰 50%, 평점 50%
        return (reviewScore * 0.5) + (ratingScore * 0.5);
    }

    // 거리를 고려한 점수 계산
    public double calculateScoreWithDistance(TravelPlace place, TravelPlace reference) {
        if (place == null) {
            return 0.0;
        }

        double baseScore = calculateBaseScore(place);

        if (reference == null) {
            return baseScore;
        }

        double distance = distanceCalculator.calculate(place, reference);
        double distanceScore = calculateDistanceScore(distance);

        // 기본 점수 60%, 거리 점수 40%
        return (baseScore * StageConstants.ScoreWeight.BASE_SCORE_WEIGHT) +
               (distanceScore * StageConstants.ScoreWeight.QUALITY_WEIGHT);
    }

    // 여러 기준점을 고려한 점수 계산 (최소 거리 기준)
    public double calculateScoreWithMultipleReferences(TravelPlace place,
                                                       List<TravelPlace> references) {
        if (place == null) {
            return 0.0;
        }

        double baseScore = calculateBaseScore(place);

        if (references == null || references.isEmpty()) {
            return baseScore;
        }

        double minDistance = distanceCalculator.findMinDistance(place, references);
        double distanceScore = calculateDistanceScore(minDistance);

        // 거리 40%, 리뷰수 30%, 평점 30%
        return (distanceScore * StageConstants.ScoreWeight.DISTANCE_WEIGHT) +
               (baseScore * (StageConstants.ScoreWeight.REVIEW_WEIGHT +
                           StageConstants.ScoreWeight.RATING_WEIGHT));
    }

    // 리뷰수 점수 계산 (로그 스케일)
    private double calculateReviewScore(Integer reviewCount) {
        if (reviewCount == null || reviewCount <= 0) {
            return 0.0;
        }

        // 리뷰 1000개 이상이면 만점
        return Math.min(1.0,
            Math.log10(reviewCount + 1) / StageConstants.Scoring.REVIEW_LOG_BASE);
    }

    // 평점 점수 계산
    private double calculateRatingScore(Double rating) {
        if (rating == null || rating <= 0) {
            return 0.0;
        }

        // 5점 만점 정규화
        return Math.min(1.0, rating / StageConstants.Scoring.MAX_RATING);
    }

    // 거리 점수 계산
    private double calculateDistanceScore(double distanceKm) {
        if (distanceKm <= 0) {
            return 1.0; // 같은 위치면 만점
        }

        // 5km 이내 만점, 10km에서 0점
        if (distanceKm <= StageConstants.Distance.NEAR_DISTANCE_KM) {
            return 1.0;
        } else if (distanceKm >= StageConstants.Distance.FAR_DISTANCE_KM) {
            return 0.0;
        } else {
            // 5-10km 구간 선형 감소
            return 1.0 - ((distanceKm - StageConstants.Distance.NEAR_DISTANCE_KM) /
                         (StageConstants.Distance.FAR_DISTANCE_KM -
                          StageConstants.Distance.NEAR_DISTANCE_KM));
        }
    }

    // 카테고리 다양성 점수 계산
    public double calculateDiversityScore(String category) {
        if (category == null) {
            return 0.0;
        }

        return switch (category) {
            case StageConstants.Category.RESTAURANT -> 0.9;        // 식사 필수
            case StageConstants.Category.TOURIST_ATTRACTION -> 0.8; // 관광 중요
            case StageConstants.Category.CAFE -> 0.7;               // 휴식 필요
            case StageConstants.Category.ACTIVITY -> 0.6;           // 체험 활동
            case StageConstants.Category.SHOPPING -> 0.5;           // 쇼핑
            default -> 0.4;
        };
    }

    // 종합 점수 계산 (거리 + 품질 + 다양성)
    public double calculateComprehensiveScore(TravelPlace place,
                                             List<TravelPlace> references) {
        if (place == null) {
            return 0.0;
        }

        double baseScore = calculateBaseScore(place);
        double distanceScore = 0.0;

        if (references != null && !references.isEmpty()) {
            double minDistance = distanceCalculator.findMinDistance(place, references);
            distanceScore = calculateDistanceScore(minDistance);
        }

        double diversityScore = calculateDiversityScore(place.getCategory());

        // 거리 40%, 품질(리뷰+평점) 40%, 다양성 20%
        return (distanceScore * StageConstants.ScoreWeight.DISTANCE_WEIGHT) +
               (baseScore * StageConstants.ScoreWeight.QUALITY_WEIGHT) +
               (diversityScore * StageConstants.ScoreWeight.DIVERSITY_WEIGHT);
    }
}