package com.compass.domain.chat.function.planning;

import com.compass.domain.chat.model.dto.DestinationRecommendationDto;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

// "목적지 미정" 시 사용자에게 맞춤 목적지를 추천

@Slf4j
@Component("recommendDestinations")
@RequiredArgsConstructor
public class RecommendDestinationsFunction implements Function<TravelFormSubmitRequest, DestinationRecommendationDto> {



    @Override
    public DestinationRecommendationDto apply(TravelFormSubmitRequest request) {
        log.info("목적지 추천 기능 실행. 출발지: {}", request.departureLocation());

        // 간단한 추천 로직으로 변경 (Perplexity 대신)
        List<DestinationRecommendationDto.RecommendedDestination> recommendedDestinations = 
            createSimpleRecommendations(request.departureLocation(), request.travelStyle());

        return new DestinationRecommendationDto(
                "이런 곳은 어떠세요? 🗺️",
                "출발지와 선호하는 여행 스타일에 맞춰 추천해 드려요.",
                createDistanceOptions(),
                recommendedDestinations
        );
    }

    // 간단한 추천 로직 (Perplexity 대신)
    private List<DestinationRecommendationDto.RecommendedDestination> createSimpleRecommendations(
            String departure, List<String> styles) {
        
        // 기본 추천 목적지들
        List<DestinationRecommendationDto.RecommendedDestination> recommendations = List.of(
            new DestinationRecommendationDto.RecommendedDestination(
                "서울", "대한민국", "다양한 문화와 현대적인 매력", 
                "https://example.com/seoul.jpg", 
                List.of("문화", "쇼핑", "맛집")
            ),
            new DestinationRecommendationDto.RecommendedDestination(
                "부산", "대한민국", "바다와 해산물의 매력", 
                "https://example.com/busan.jpg", 
                List.of("바다", "해산물", "휴양")
            ),
            new DestinationRecommendationDto.RecommendedDestination(
                "제주도", "대한민국", "자연과 힐링의 섬", 
                "https://example.com/jeju.jpg", 
                List.of("자연", "힐링", "카페")
            )
        );
        
        return recommendations;
    }

    // 거리 기반 선택 옵션을 생성하는 헬퍼 메서드
    private List<String> createDistanceOptions() {
        return List.of("가까운 곳으로", "2~3시간 거리", "비행기 타고 멀리");
    }
}