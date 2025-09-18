package com.compass.domain.chat.function.planning;

import com.compass.domain.chat.model.dto.DestinationRecommendationDto;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.service.PerplexityClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

// Task 2.3.1: "목적지 미정" 시 사용자에게 맞춤 목적지를 추천하는 Function

@Slf4j
@Component("recommendDestinations")
@RequiredArgsConstructor
public class RecommendDestinationsFunction implements Function<TravelFormSubmitRequest, DestinationRecommendationDto> {


    private final PerplexityClient perplexityClient;
    private final ObjectMapper objectMapper;

    @Override
    public DestinationRecommendationDto apply(TravelFormSubmitRequest request) {
        log.info("목적지 추천 기능 실행 (PerplexityClient 사용). 출발지: {}", request.departureLocation());

        List<DestinationRecommendationDto.RecommendedDestination> recommendedDestinations;
        try {
            // 1. 프롬프트 생성
            String prompt = createRecommendationPrompt(request.departureLocation(), request.travelStyle());

            // 2. PerplexityClient를 통해 간단하게 API 호출
            // PerplexityClient가 이미 응답의 content 부분만 깔끔하게 추출해서 반환.
            String contentJson = perplexityClient.search(prompt);

            // 3. 클라이언트가 반환한 JSON 배열 문자열을 DTO 리스트로 파싱
            recommendedDestinations = objectMapper.readValue(contentJson, new TypeReference<>() {});

        } catch (Exception e) {
            log.error("PerplexityClient 사용 중 오류 발생. 빈 추천 목록을 반환합니다. userId: {}", request.userId(), e);
            recommendedDestinations = Collections.emptyList();
        }

        // 4. 최종 결과 DTO 생성
        return new DestinationRecommendationDto(
                "이런 곳은 어떠세요? 🗺️",
                "출발지와 선호하는 여행 스타일에 맞춰 추천해 드려요.",
                createDistanceOptions(),
                recommendedDestinations
        );
    }

    // Perplexity API에 맞는 프롬프트를 생성하는 헬퍼 메서드
    private String createRecommendationPrompt(String departure, List<String> styles) {
        String stylesText = String.join(", ", styles);
        return String.format(
                "%s에서 출발하며, %s 스타일의 여행을 즐기는 사람에게 어울리는 국내 여행지 3곳을 추천해줘. 각 장소의 특징과 추천 이유를 간단히 설명하고, 대표 이미지 URL과 관련된 태그를 포함해줘. 반드시 JSON 배열 형식으로만 응답해줘. 다른 부가 설명은 절대 붙이지마.",
                departure, stylesText
        );
    }

    // 거리 기반 선택 옵션을 생성하는 헬퍼 메서드
    private List<String> createDistanceOptions() {
        return List.of("가까운 곳으로", "2~3시간 거리", "비행기 타고 멀리");
    }
}