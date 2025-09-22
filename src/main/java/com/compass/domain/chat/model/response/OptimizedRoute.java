package com.compass.domain.chat.model.response;

import java.util.List;
import java.util.Objects;

// 최적화된 경로 응답 DTO
// 복수 목적지의 최적 순서와 경로 정보를 담는 구조
public record OptimizedRoute(
    List<String> optimizedOrder,      // 최적화된 목적지 순서
    Double totalDistance,             // 총 이동 거리 (km)
    Integer totalTravelTime,          // 총 이동 시간 (분)
    String strategy,                  // 사용된 최적화 전략
    List<RouteSegment> segments,      // 구간별 상세 정보
    String message                    // 사용자에게 보여줄 메시지
) {
    // 검증 로직
    public OptimizedRoute {
        Objects.requireNonNull(optimizedOrder, "최적화된 순서는 필수입니다");
        if (optimizedOrder.isEmpty()) {
            throw new IllegalArgumentException("최적화된 순서는 최소 1개 이상이어야 합니다");
        }
        
        if (totalDistance != null && totalDistance < 0) {
            throw new IllegalArgumentException("총 거리는 0 이상이어야 합니다");
        }
        
        if (totalTravelTime != null && totalTravelTime < 0) {
            throw new IllegalArgumentException("총 이동 시간은 0 이상이어야 합니다");
        }
        
        // 기본 메시지 설정
        if (message == null || message.isEmpty()) {
            message = String.format("%d개 목적지의 최적 경로가 계산되었습니다", optimizedOrder.size());
        }
    }
    
    // 성공 응답 생성
    public static OptimizedRoute success(List<String> order, Double distance, Integer time, String strategy, List<RouteSegment> segments) {
        return new OptimizedRoute(order, distance, time, strategy, segments, null);
    }
    
    // 에러 응답 생성
    public static OptimizedRoute error(String message) {
        return new OptimizedRoute(List.of(), 0.0, 0, "NONE", List.of(), message);
    }
    
    // 구간별 경로 정보
    public record RouteSegment(
        String from,                  // 출발지
        String to,                    // 도착지
        Double distance,              // 구간 거리 (km)
        Integer travelTime,           // 구간 이동 시간 (분)
        String transportMethod        // 교통수단 (자동차, 대중교통 등)
    ) {
        public RouteSegment {
            Objects.requireNonNull(from, "출발지는 필수입니다");
            Objects.requireNonNull(to, "도착지는 필수입니다");
            
            if (distance != null && distance < 0) {
                throw new IllegalArgumentException("구간 거리는 0 이상이어야 합니다");
            }
            
            if (travelTime != null && travelTime < 0) {
                throw new IllegalArgumentException("구간 이동 시간은 0 이상이어야 합니다");
            }
            
            // 기본 교통수단 설정
            if (transportMethod == null || transportMethod.isEmpty()) {
                transportMethod = "자동차";
            }
        }
    }
}

