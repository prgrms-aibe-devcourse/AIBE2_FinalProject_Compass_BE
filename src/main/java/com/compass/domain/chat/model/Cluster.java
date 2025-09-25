package com.compass.domain.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 사전 정의된 클러스터 모델
 * 개선안에 따른 클러스터 기반 데이터 수집을 위한 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cluster {
    
    private String name;                    // 클러스터명 (hongdae, gangnam, etc.)
    private String displayName;            // 표시명 (홍대, 강남, etc.)
    private Double centerLat;              // 중심 위도
    private Double centerLng;              // 중심 경도
    private Integer radius;                // 반경 (미터)
    private List<String> styles;           // 여행 스타일 태그
    private String ageGroup;              // 연령대
    private String budget;                // 예산 수준
    private List<String> characteristics; // 클러스터 특성
    private String description;           // 클러스터 설명
    private Double matchScore;            // 매칭 점수
    
    /**
     * 여행 스타일과 클러스터의 매칭 점수 계산
     * 개선안의 스타일 기반 매칭 알고리즘 구현
     */
    public double calculateMatchScore(Map<String, Object> travelStyle) {
        double score = 0.0;
        int matchCount = 0;
        
        // 스타일 매칭 점수 계산
        if (travelStyle.containsKey("styles")) {
            List<String> userStyles = (List<String>) travelStyle.get("styles");
            for (String userStyle : userStyles) {
                if (this.styles.contains(userStyle)) {
                    score += 0.3; // 스타일 매칭 시 0.3점
                    matchCount++;
                }
            }
        }
        
        // 연령대 매칭 점수 계산
        if (travelStyle.containsKey("ageGroup")) {
            String userAgeGroup = (String) travelStyle.get("ageGroup");
            if (this.ageGroup.equals(userAgeGroup)) {
                score += 0.4; // 연령대 매칭 시 0.4점
                matchCount++;
            }
        }
        
        // 예산 매칭 점수 계산
        if (travelStyle.containsKey("budget")) {
            String userBudget = (String) travelStyle.get("budget");
            if (this.budget.equals(userBudget)) {
                score += 0.3; // 예산 매칭 시 0.3점
                matchCount++;
            }
        }
        
        // 최대 점수는 1.0
        return Math.min(score, 1.0);
    }
    
    /**
     * 클러스터별 장소 수집 비율 계산
     * 개선안의 클러스터 분배 알고리즘 구현
     */
    public int calculatePlaceCount(int totalPlaces, double matchScore) {
        int placeCount;
        if (matchScore >= 0.8) {
            placeCount = (int) (totalPlaces * 0.6); // 높은 매칭: 60%
        } else if (matchScore >= 0.5) {
            placeCount = (int) (totalPlaces * 0.3); // 중간 매칭: 30%
        } else {
            placeCount = (int) (totalPlaces * 0.1); // 낮은 매칭: 10%
        }
        
        // 최소 1개는 할당 (매칭 점수가 0.1 이상인 경우)
        if (matchScore >= 0.1 && placeCount == 0) {
            placeCount = 1;
        }
        
        return placeCount;
    }
}