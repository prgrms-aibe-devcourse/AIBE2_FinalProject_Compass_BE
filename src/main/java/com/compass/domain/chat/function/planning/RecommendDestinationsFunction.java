package com.compass.domain.chat.function.planning;

import com.compass.domain.chat.model.dto.DestinationRecommendationDto;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.service.PerplexityClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.Objects;
import java.util.stream.Collectors;

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
            // 1. 프롬프트 생성 (TravelFormSubmitRequest에서 직접 정보 사용)
            String prompt = createRecommendationPrompt(request.departureLocation(), request.travelStyle());

            // 2. PerplexityClient를 통해 API 호출
            String rawContent = perplexityClient.search(prompt);

            // 3. LLM 응답에서 순수한 JSON만 추출하고 유효성 검증
            recommendedDestinations = parseAndValidateDestinations(rawContent);

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
        String stylesText = (styles == null || styles.isEmpty()) ? "지정되지 않음" : String.join(", ", styles);
        // [수정] 특정 도시 제한을 없애고, 광범위한 '지역명'으로 추천하도록 변경
        return String.format(
                "당신은 한국 지리에 매우 밝은 여행지 추천 전문가입니다. " +
                        "사용자의 출발지와 여행 스타일에 맞춰 대한민국 국내 여행지 4~5곳을 **광범위한 지역명(예: 부산, 경주, 제주도 같은 느낌)**으로 추천해주세요. " +
                        "당신의 답변은 반드시 다른 설명 없이 순수한 JSON 배열 형식이어야 합니다. " +
                        "각 객체는 반드시 \"cityName\", \"country\", \"description\", \"tags\" 라는 정확한 키를 가져야 합니다. " +
                        "\"country\"는 항상 \"대한민국\"이어야 합니다. " +
                        "\"description\"은 해당 장소의 특징과 추천 이유를 2~3문장으로 자연스럽게 작성해주세요. " +
                        "\"tags\"는 해당 지역의 특징을 나타내는 키워드 3~4개를 포함한 배열이어야 합니다." +
                        "--- \n" +
                        "사용자 출발지: %s \n" +
                        "사용자 여행 스타일: %s",
                departure, stylesText
        );
    }

    /**
     * LLM 응답을 파싱하고, 각 항목의 유효성을 검증하여 신뢰할 수 있는 데이터만 필터링합니다.
     */
    private List<DestinationRecommendationDto.RecommendedDestination> parseAndValidateDestinations(String rawContent) {
        try {
            String cleanedJson = cleanJsonString(rawContent);
            if (cleanedJson.isEmpty()) {
                log.warn("LLM 응답에서 유효한 JSON을 찾지 못했습니다.");
                return Collections.emptyList();
            }

            List<DestinationRecommendationDto.RecommendedDestination> candidates = objectMapper.readValue(cleanedJson, new TypeReference<>() {});

            return candidates.stream()
                    .filter(dest -> Objects.nonNull(dest.cityName()) && !dest.cityName().isBlank() &&
                            Objects.nonNull(dest.description()) && !dest.description().isBlank())
                    .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            log.error("LLM 응답 JSON 파싱에 실패했습니다. Raw Content: {}", rawContent, e);
            return Collections.emptyList();
        }
    }


    // 거리 기반 선택 옵션을 생성하는 헬퍼 메서드
    private List<String> createDistanceOptions() {
        return List.of("가까운 곳으로", "2~3시간 거리", "비행기 타고 멀리");
    }

    /**
     * LLM 응답에서 Markdown 코드 블록 등을 제거하여 순수한 JSON만 추출합니다.
     */
    private String cleanJsonString(String response) {
        int firstBracket = response.indexOf('[');
        int lastBracket = response.lastIndexOf(']');
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            return response.substring(firstBracket, lastBracket + 1);
        }
        return ""; // 유효한 JSON 배열을 찾지 못하면 빈 문자열 반환
    }
}