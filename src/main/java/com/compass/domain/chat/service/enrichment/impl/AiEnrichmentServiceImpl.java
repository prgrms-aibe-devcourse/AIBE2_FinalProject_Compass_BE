package com.compass.domain.chat.service.enrichment.impl;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.service.enrichment.AbstractEnrichmentService;
import com.compass.domain.chat.service.enrichment.ApiRateLimiter;
import com.compass.domain.chat.service.enrichment.EnrichmentUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI API를 활용한 고급 정보 보강 서비스
 * Perplexity로 실시간 정보, OpenAI로 창의적 콘텐츠 생성
 */
@Service
@Slf4j
public class AiEnrichmentServiceImpl extends AbstractEnrichmentService {

    private final RestTemplate restTemplate;
    private final ApiRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${perplexity.api.key:}")
    private String perplexityApiKey;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    private static final String PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    public AiEnrichmentServiceImpl(
            TravelCandidateRepository travelCandidateRepository,
            RestTemplate restTemplate,
            ApiRateLimiter rateLimiter,
            ObjectMapper objectMapper) {
        super(travelCandidateRepository);
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getServiceName() {
        return "AiEnrichment";
    }

    @Override
    public int getPriority() {
        return 4; // 가장 낮은 우선순위 (비용 고려)
    }

    @Override
    public int getRateLimitDelay() {
        return 20000; // Perplexity 3/min = 20초 간격
    }

    @Override
    public boolean isEligible(TravelCandidate candidate) {
        // 평점이 높거나 리뷰가 많은 인기 장소만
        return candidate.getRating() != null && candidate.getRating() >= 4.0 &&
               candidate.getReviewCount() != null && candidate.getReviewCount() >= 100;
    }

    @Override
    public boolean enrichSingle(TravelCandidate candidate) {
        try {
            boolean updated = false;

            // 1. Perplexity로 실시간 정보 수집
            if (perplexityApiKey != null && !perplexityApiKey.isEmpty()) {
                rateLimiter.acquire("perplexity");
                Map<String, String> realtimeInfo = getPerplexityInfo(candidate);

                if (realtimeInfo != null) {
                    // 추천 방문 시간
                    if (realtimeInfo.containsKey("recommendedDuration")) {
                        candidate.setRecommendedDuration(
                            EnrichmentUtils.extractDuration(realtimeInfo.get("recommendedDuration"))
                        );
                        updated = true;
                    }

                    // 주요 특징
                    if (realtimeInfo.containsKey("highlights")) {
                        candidate.setHighlights(
                            EnrichmentUtils.truncateString(realtimeInfo.get("highlights"), 1000)
                        );
                        updated = true;
                    }

                    // 방문 팁
                    if (realtimeInfo.containsKey("tips")) {
                        candidate.setTips(
                            EnrichmentUtils.truncateString(realtimeInfo.get("tips"), 500)
                        );
                        updated = true;
                    }

                    // 근처 명소
                    if (realtimeInfo.containsKey("nearbyAttractions")) {
                        candidate.setNearbyAttractions(
                            EnrichmentUtils.truncateString(realtimeInfo.get("nearbyAttractions"), 500)
                        );
                        updated = true;
                    }
                }
            }

            // 2. OpenAI로 창의적 설명 생성 (선택적)
            if (!updated && openAiApiKey != null && !openAiApiKey.isEmpty()) {
                rateLimiter.acquire("openai");
                String description = generateOpenAIDescription(candidate);

                if (description != null && !description.isEmpty()) {
                    candidate.setDescription(
                        EnrichmentUtils.truncateString(description, 1000)
                    );
                    updated = true;
                }
            }

            // AI 보강 플래그 설정
            if (updated) {
                candidate.setAiEnriched(true);
                candidate.setUpdatedAt(LocalDateTime.now());
            }

            return updated;

        } catch (Exception e) {
            rateLimiter.handleBackoff("perplexity", e);
            log.error("AI 보강 실패: {} - {}", candidate.getName(), e.getMessage());
            return false;
        }
    }

    private Map<String, String> getPerplexityInfo(TravelCandidate candidate) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + perplexityApiKey);

            String prompt = String.format(
                "한국 여행지 '%s'(%s)에 대한 정보를 JSON 형식으로 제공해주세요:\n" +
                "{\n" +
                "  \"recommendedDuration\": \"추천 방문 시간 (예: 1-2시간)\",\n" +
                "  \"highlights\": \"주요 볼거리와 특징 (100자 이내)\",\n" +
                "  \"tips\": \"방문 팁과 유용한 정보 (100자 이내)\",\n" +
                "  \"nearbyAttractions\": \"도보 10분 거리 내 다른 명소 (100자 이내)\"\n" +
                "}",
                candidate.getName(),
                candidate.getAddress() != null ? candidate.getAddress() : ""
            );

            Map<String, Object> requestBody = Map.of(
                "model", "sonar-small-chat",
                "messages", List.of(
                    Map.of("role", "system", "content", "당신은 한국 여행 전문가입니다. 정확하고 최신 정보만 제공하세요."),
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", 500
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                PERPLEXITY_API_URL,
                HttpMethod.POST,
                request,
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText();

            // JSON 파싱
            try {
                JsonNode jsonContent = objectMapper.readTree(content);
                return Map.of(
                    "recommendedDuration", jsonContent.path("recommendedDuration").asText(""),
                    "highlights", jsonContent.path("highlights").asText(""),
                    "tips", jsonContent.path("tips").asText(""),
                    "nearbyAttractions", jsonContent.path("nearbyAttractions").asText("")
                );
            } catch (Exception e) {
                log.warn("Perplexity 응답 JSON 파싱 실패, 텍스트로 처리: {}", content);
                return Map.of("highlights", content);
            }

        } catch (Exception e) {
            log.error("Perplexity API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private String generateOpenAIDescription(TravelCandidate candidate) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openAiApiKey);

            String prompt = String.format(
                "%s에 대한 매력적인 여행지 소개를 100자 이내로 작성해주세요. " +
                "장소의 특징과 방문 가치를 강조해주세요.",
                candidate.getName()
            );

            Map<String, Object> requestBody = Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                    Map.of("role", "system", "content", "당신은 감성적인 여행 작가입니다."),
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7,
                "max_tokens", 200
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                OPENAI_API_URL,
                HttpMethod.POST,
                request,
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").path(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 상위 N개 장소에 대해서만 AI 보강 실행
     */
    public int enrichTopPlaces(int limit) {
        log.info("상위 {}개 장소 AI 보강 시작", limit);

        // 평점과 리뷰수 기준으로 상위 장소 선택
        List<TravelCandidate> topPlaces = travelCandidateRepository.findTopRatedPlaces(
            PageRequest.of(0, limit)
        );

        int enrichedCount = 0;
        for (TravelCandidate candidate : topPlaces) {
            if (candidate.getAiEnriched() == null || !candidate.getAiEnriched()) {
                if (enrichSingle(candidate)) {
                    travelCandidateRepository.save(candidate);
                    enrichedCount++;
                    log.info("AI 보강 완료 ({}/{}): {}", enrichedCount, limit, candidate.getName());
                }

                // Rate limiting
                rateLimitDelay();
            }
        }

        return enrichedCount;
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> baseStats = super.getStatistics();

        long totalCandidates = travelCandidateRepository.count();
        long aiEnriched = travelCandidateRepository.countByAiEnrichedTrue();
        long withDescription = travelCandidateRepository.countByDescriptionIsNotNull();
        long withTips = travelCandidateRepository.countByTipsIsNotNull();

        baseStats.put("aiEnrichedCount", aiEnriched);
        baseStats.put("aiEnrichmentRate", (double) aiEnriched / totalCandidates * 100);
        baseStats.put("descriptionRate", (double) withDescription / totalCandidates * 100);
        baseStats.put("tipsRate", (double) withTips / totalCandidates * 100);

        return baseStats;
    }
}