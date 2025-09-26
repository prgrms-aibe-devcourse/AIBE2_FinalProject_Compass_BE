package com.compass.domain.chat.stage3.dto;

import com.compass.domain.chat.model.TravelPlace;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

// 최적화된 경로 정보
@Getter
@Builder
public class OptimizedRoute {
    private final List<TravelPlace> places;           // 최적 순서로 정렬된 장소들
    private final double totalDistance;               // 총 이동 거리 (km)
    private final long totalDuration;                 // 총 소요 시간 (분)
    private final List<RouteSegment> segments;        // 구간별 상세 정보
    private final String transportMode;               // 이동 수단 (자차, 대중교통)
    private final RouteStatistics statistics;         // 경로 통계 정보

    // 구간별 경로 정보
    @Getter
    @Builder
    public static class RouteSegment {
        private final TravelPlace from;               // 출발지
        private final TravelPlace to;                 // 목적지
        private final double distance;                // 구간 거리 (km)
        private final long duration;                  // 구간 소요시간 (분)
        private final String transportMode;           // 구간별 이동 수단
        private final RouteDetails details;           // 상세 경로 정보
        private final String departureTime;           // 출발 시각
        private final String arrivalTime;             // 도착 시각
    }

    // 상세 경로 정보 (Kakao Mobility API 응답)
    @Getter
    @Builder
    public static class RouteDetails {
        private final String routeType;               // 경로 타입 (도보, 버스, 지하철, 자동차)
        private final List<String> instructions;      // 턴바이턴 안내
        private final String polyline;                // 경로 폴리라인 (지도 표시용)
        private final Double fare;                    // 요금 (대중교통)
        private final List<TransitInfo> transitInfo;  // 대중교통 정보
    }

    // 대중교통 정보
    @Getter
    @Builder
    public static class TransitInfo {
        private final String type;                    // 버스, 지하철
        private final String lineNumber;              // 노선 번호
        private final String boardingStation;         // 승차역
        private final String alightingStation;        // 하차역
        private final int stationCount;               // 정류장 수
    }

    // 경로 통계
    @Getter
    @Builder
    public static class RouteStatistics {
        private final double averageDistanceBetweenPlaces;  // 평균 이동 거리
        private final long averageDurationBetweenPlaces;    // 평균 이동 시간
        private final int totalSegments;                    // 전체 구간 수
        private final double optimizationRate;              // 최적화 비율 (원래 대비)
        private final long estimatedStartTime;              // 예상 출발 시간
        private final long estimatedEndTime;                // 예상 도착 시간
    }

    // 경로 최적화 정보
    public double getOptimizationPercentage() {
        if (statistics != null && statistics.getOptimizationRate() > 0) {
            return (1 - statistics.getOptimizationRate()) * 100;
        }
        return 0;
    }

    // 이동 효율성 계산
    public double getEfficiencyScore() {
        if (totalDistance > 0 && places != null && places.size() > 1) {
            // 방문 장소 수 대비 총 이동 거리
            return (double) places.size() / totalDistance * 100;
        }
        return 0;
    }
}