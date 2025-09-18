package com.compass.domain.chat.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// 목적지 검색 요청 DTO
// 복수 도시 경로 최적화를 위한 입력 데이터 구조
public record DestinationSearchRequest(
    @NotNull @NotEmpty List<String> destinations,  // 목적지 목록 (필수)
    String startLocation,                          // 출발지 (선택)
    String optimizationStrategy                    // 최적화 전략 (ONE_DIRECTION, SHORTEST, CIRCULAR)
) {
    // 검증 로직
    public DestinationSearchRequest {
        if (destinations.size() > 10) {
            throw new IllegalArgumentException("목적지는 최대 10개까지 입력 가능합니다");
        }
        
        // 기본값 설정
        if (optimizationStrategy == null || optimizationStrategy.isEmpty()) {
            optimizationStrategy = "SHORTEST";
        }
        
        // 전략 검증
        if (!isValidStrategy(optimizationStrategy)) {
            throw new IllegalArgumentException("유효하지 않은 최적화 전략입니다: " + optimizationStrategy);
        }
    }
    
    // 최적화 전략 검증
    private static boolean isValidStrategy(String strategy) {
        return "ONE_DIRECTION".equals(strategy) || 
               "SHORTEST".equals(strategy) || 
               "CIRCULAR".equals(strategy);
    }
}
