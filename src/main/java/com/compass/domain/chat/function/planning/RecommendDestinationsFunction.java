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

// "목적지 미정" 시 사용자에게 맞춤 목적지를 추천

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

            // 2. PerplexityClient를 통해 API 호출
            String rawContent = perplexityClient.search(prompt);

            // 3. LLM 응답에서 Markdown 코드 블록 등을 제거하여 순수한 JSON만 추출
            String cleanedJson = cleanJsonString(rawContent);

            // 4. 정리된 JSON 배열 문자열을 DTO 리스트로 파싱
            recommendedDestinations = objectMapper.readValue(cleanedJson, new TypeReference<>() {});

        } catch (Exception e) {
            log.error("PerplexityClient 사용 중 오류 발생. 빈 추천 목록을 반환합니다. userId: {}", request.userId(), e);
            recommendedDestinations = Collections.emptyList();
        }

        // 5. 최종 결과 DTO 생성
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
        // [수정] LLM이 우리 DTO 필드명(cityName, country, description, imageUrl, tags)에 맞춰 응답하도록 프롬프트를 매우 구체적으로 변경합니다.
        return String.format(
                "당신은 여행지 추천 전문가입니다. 사용자의 출발지와 여행 스타일에 맞춰 대한민국 국내 여행지 3곳을 추천해주세요. " +
                "당신의 답변은 반드시 순수한 JSON 배열 형식이어야 하며, 다른 어떤 텍스트나 마크다운도 포함해서는 안 됩니다. " +
                "배열의 각 객체는 반드시 \"cityName\", \"country\", \"description\", \"imageUrl\", \"tags\" 라는 정확한 키를 가져야 합니다. " +
                "\"country\" 키의 값은 항상 \"대한민국\"으로 해주세요. " +
                "\"description\" 키에는 그 장소의 특징과 추천 이유를 합쳐서 자연스러운 문장으로 작성해주세요. " +
                "사용자 출발지: %s. 사용자 여행 스타일: %s.",
                departure, stylesText
        );
    }

    // 거리 기반 선택 옵션을 생성하는 헬퍼 메서드
    private List<String> createDistanceOptions() {
        return List.of("가까운 곳으로", "2~3시간 거리", "비행기 타고 멀리");
    }

/**
 * LLM 응답에서 Markdown 코드 블록(`
 * @param response LLM의 원본 응답 문자열
 * @return 정리된 JSON 배열 문자열
 */
private String cleanJsonString(String response) {
    // 응답에서 첫 '['와 마지막 ']'를 찾아 그 사이의 문자열을 추출합니다. (JSON 배열을 가정)
    int firstBracket = response.indexOf('[');
    int lastBracket = response.lastIndexOf(']');
    if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
        return response.substring(firstBracket, lastBracket + 1);
    }
    return response; // JSON 마커를 찾지 못하면 원본 반환
}
}