package com.compass.domain.chat.route_optimization.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptimizationStrategyFactory {

    private final DistanceOptimizationStrategy distanceStrategy;
    private final TimeOptimizationStrategy timeStrategy;
    private final BalancedOptimizationStrategy balancedStrategy;

    // 전략 선택
    public OptimizationStrategy getStrategy(String strategyType) {
        if (strategyType == null) {
            log.info("전략 타입 미지정, 기본 전략(BALANCED) 사용");
            return balancedStrategy;
        }

        OptimizationStrategy strategy = switch (strategyType.toUpperCase()) {
            case "DISTANCE", "SHORTEST_DISTANCE" -> {
                log.info("거리 최적화 전략 선택");
                yield distanceStrategy;
            }
            case "TIME", "SHORTEST_TIME" -> {
                log.info("시간 최적화 전략 선택");
                yield timeStrategy;
            }
            case "BALANCED", "RECOMMEND" -> {
                log.info("균형 최적화 전략 선택");
                yield balancedStrategy;
            }
            default -> {
                log.warn("알 수 없는 전략 타입: {}, 기본 전략(BALANCED) 사용", strategyType);
                yield balancedStrategy;
            }
        };

        return strategy;
    }

    // 사용자 컨텍스트 기반 전략 추천
    public OptimizationStrategy recommendStrategy(
        int tripDays,
        int placeCount,
        String transportMode,
        Boolean hasChildren,
        Boolean hasElderly
    ) {
        // 노약자나 어린이가 있으면 시간 최적화
        if (Boolean.TRUE.equals(hasChildren) || Boolean.TRUE.equals(hasElderly)) {
            log.info("노약자/어린이 동반 -> 시간 최적화 전략 추천");
            return timeStrategy;
        }

        // 대중교통 이용 시 시간 최적화
        if ("PUBLIC_TRANSPORT".equals(transportMode)) {
            log.info("대중교통 이용 -> 시간 최적화 전략 추천");
            return timeStrategy;
        }

        // 장소가 많고 여행 기간이 짧으면 거리 최적화
        if (placeCount > tripDays * 8) {
            log.info("빡빡한 일정 -> 거리 최적화 전략 추천");
            return distanceStrategy;
        }

        // 기본적으로 균형 전략
        log.info("일반적인 여행 -> 균형 최적화 전략 추천");
        return balancedStrategy;
    }
}