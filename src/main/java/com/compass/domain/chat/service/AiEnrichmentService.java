package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiEnrichmentService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${perplexity.api.key:}")
    private String perplexityApiKey;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    private static final String PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // 상위 인기 장소 AI 보강
    @Transactional
    public int enrichTopPlacesWithAI(int limit) {
        log.info("상위 {} 개 장소 AI 보강 시작", limit);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 평점 높고 리뷰 많은 상위 장소
        List<TravelCandidate> topPlaces = travelCandidateRepository.findAll().stream()
            .filter(c -> c.getRating() != null && c.getRating() > 4.0)
            .filter(c -> c.getReviewCount() != null && c.getReviewCount() > 100)
            .sorted((a, b) -> Double.compare(
                b.getRating() * Math.log(b.getReviewCount() + 1),
                a.getRating() * Math.log(a.getReviewCount() + 1)
            ))
            .limit(limit)
            .toList();

        log.info("AI 보강 대상: {} 개", topPlaces.size());

        topPlaces.forEach(candidate -> {
            try {
                boolean enriched = enrichWithAI(candidate);
                if (enriched) {
                    candidate.setEnrichmentStatus(2);
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                    log.debug("AI 보강 성공: {}", candidate.getName());
                } else {
                    failCount.incrementAndGet();
                }

                // API 비용 관리 (분당 3개)
                Thread.sleep(20000);

            } catch (Exception e) {
                log.error("AI 보강 실패 - {}: {}", candidate.getName(), e.getMessage());
                failCount.incrementAndGet();
            }
        });

        log.info("AI 보강 완료 - 성공: {}, 실패: {}", successCount.get(), failCount.get());
        return successCount.get();
    }

    // 비동기 AI 보강
    @Async
    @Transactional
    public CompletableFuture<Integer> enrichBatchWithAIAsync(List<Long> candidateIds) {
        log.info("비동기 AI 보강 시작 - {} 개", candidateIds.size());

        AtomicInteger successCount = new AtomicInteger(0);
        List<TravelCandidate> candidates = travelCandidateRepository.findAllById(candidateIds);

        candidates.forEach(candidate -> {
            try {
                boolean enriched = enrichWithAI(candidate);
                if (enriched) {
                    candidate.setEnrichmentStatus(2);
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                }
                Thread.sleep(20000);
            } catch (Exception e) {
                log.error("비동기 AI 보강 실패: {}", e.getMessage());
            }
        });

        return CompletableFuture.completedFuture(successCount.get());
    }

    // 개별 AI 보강
    private boolean enrichWithAI(TravelCandidate candidate) {
        try {
            // Perplexity로 실시간 정보 수집
            String perplexityResult = callPerplexity(candidate);
            if (perplexityResult != null && !perplexityResult.isEmpty()) {
                parsePerplexityResult(candidate, perplexityResult);
            }

            // OpenAI로 창의적 콘텐츠 생성
            String openaiResult = callOpenAI(candidate);
            if (openaiResult != null && !openaiResult.isEmpty()) {
                parseOpenAIResult(candidate, openaiResult);
            }

            return true;

        } catch (Exception e) {
            log.error("AI 보강 중 오류: {}", e.getMessage());
            return false;
        }
    }

    // Perplexity API 호출
    private String callPerplexity(TravelCandidate candidate) {
        try {
            String prompt = buildPerplexityPrompt(candidate);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.1-sonar-small-128k-online");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 한국 여행 전문가입니다. 2024년 최신 정보를 제공해주세요."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);

            Mono<String> response = webClient.post()
                .uri(PERPLEXITY_API_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + perplexityApiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

            String responseBody = response.block();
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("choices") && root.get("choices").size() > 0) {
                return root.get("choices").get(0).get("message").get("content").asText();
            }

        } catch (Exception e) {
            log.error("Perplexity API 호출 실패: {}", e.getMessage());
        }

        return null;
    }

    // OpenAI API 호출
    private String callOpenAI(TravelCandidate candidate) {
        try {
            String prompt = buildOpenAIPrompt(candidate);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "당신은 창의적인 한국 여행 가이드입니다. 관광객에게 유용한 정보를 친근하게 제공해주세요."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.8);
            requestBody.put("max_tokens", 400);

            Mono<String> response = webClient.post()
                .uri(OPENAI_API_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);

            String responseBody = response.block();
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.has("choices") && root.get("choices").size() > 0) {
                return root.get("choices").get(0).get("message").get("content").asText();
            }

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage());
        }

        return null;
    }

    // Perplexity 프롬프트 생성
    private String buildPerplexityPrompt(TravelCandidate candidate) {
        return String.format(
            "한국 %s의 '%s'에 대한 2024년 최신 정보를 간단히 알려주세요:\n" +
            "1. 추천 방문 시간 (예: 1-2시간)\n" +
            "2. 주요 볼거리나 특징 (2-3개)\n" +
            "3. 주변 500m 내 다른 명소 (2-3개)\n" +
            "4. 최근 변경사항이나 특별 이벤트\n\n" +
            "각 항목을 한 줄씩 간단히 답변해주세요.",
            candidate.getRegion() != null ? candidate.getRegion() : "한국",
            candidate.getName()
        );
    }

    // OpenAI 프롬프트 생성
    private String buildOpenAIPrompt(TravelCandidate candidate) {
        String timeBlock = candidate.getTimeBlock() != null ?
            candidate.getTimeBlock().getKoreanName() : "방문";

        return String.format(
            "'%s'(%s)을 %s 시간에 방문하는 관광객을 위한 정보:\n" +
            "1. 한 문장 소개 (50자 이내)\n" +
            "2. 꼭 알아야 할 팁 3가지 (각 30자 이내)\n" +
            "3. 계절별 추천 (봄/여름/가을/겨울 각 20자)\n\n" +
            "친근하고 유용한 정보로 작성해주세요.",
            candidate.getName(),
            candidate.getCategory() != null ? candidate.getCategory() : "관광지",
            timeBlock
        );
    }

    // Perplexity 결과 파싱
    private void parsePerplexityResult(TravelCandidate candidate, String result) {
        try {
            String[] lines = result.split("\n");

            for (String line : lines) {
                line = line.trim();

                // 추천 방문 시간
                if (line.contains("시간") && line.contains("추천")) {
                    String duration = extractDuration(line);
                    if (duration != null && candidate.getRecommendedDuration() == null) {
                        candidate.setRecommendedDuration(duration);
                    }
                }
                // 주요 볼거리
                else if (line.contains("볼거리") || line.contains("특징")) {
                    String highlights = line.replaceFirst("^[0-9]\\.", "").trim();
                    if (candidate.getHighlights() == null) {
                        candidate.setHighlights(highlights);
                    } else {
                        candidate.setHighlights(candidate.getHighlights() + "\n" + highlights);
                    }
                }
                // 주변 명소
                else if (line.contains("주변") || line.contains("근처")) {
                    String nearby = line.replaceFirst("^[0-9]\\.", "").trim();
                    if (candidate.getNearbyAttractions() == null) {
                        candidate.setNearbyAttractions(nearby);
                    }
                }
                // 특별 이벤트
                else if (line.contains("이벤트") || line.contains("행사") || line.contains("변경")) {
                    String events = line.replaceFirst("^[0-9]\\.", "").trim();
                    if (candidate.getSpecialEvents() == null) {
                        candidate.setSpecialEvents(events);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Perplexity 결과 파싱 실패: {}", e.getMessage());
        }
    }

    // OpenAI 결과 파싱
    private void parseOpenAIResult(TravelCandidate candidate, String result) {
        try {
            String[] lines = result.split("\n");

            for (String line : lines) {
                line = line.trim();

                // 소개
                if (line.contains("소개") && candidate.getDescription() == null) {
                    String description = line.replaceFirst("^[0-9]\\.", "")
                                           .replaceFirst("^소개:", "").trim();
                    if (description.length() <= 100) {
                        candidate.setDescription(description);
                    }
                }
                // 팁
                else if (line.contains("팁")) {
                    String tips = extractTips(lines);
                    if (tips != null && candidate.getTips() == null) {
                        candidate.setTips(tips);
                    }
                }
                // 계절별 추천
                else if (line.contains("봄") || line.contains("여름") ||
                         line.contains("가을") || line.contains("겨울")) {
                    String seasonal = extractSeasonalInfo(lines);
                    if (seasonal != null && candidate.getSpecialEvents() == null) {
                        candidate.setSpecialEvents(seasonal);
                    }
                }
            }

            // 결과가 구조화되지 않은 경우 전체를 팁으로 저장
            if (candidate.getTips() == null && result.length() < 500) {
                candidate.setTips(result);
            }

        } catch (Exception e) {
            log.error("OpenAI 결과 파싱 실패: {}", e.getMessage());
        }
    }

    // 시간 추출
    private String extractDuration(String text) {
        if (text.contains("30분")) return "30분";
        if (text.contains("1시간")) return "1시간";
        if (text.contains("1-2시간")) return "1-2시간";
        if (text.contains("2시간")) return "2시간";
        if (text.contains("2-3시간")) return "2-3시간";
        if (text.contains("반나절")) return "3-4시간";
        if (text.contains("하루")) return "하루";

        return null;
    }

    // 팁 추출
    private String extractTips(String[] lines) {
        StringBuilder tips = new StringBuilder();
        int tipCount = 0;

        for (String line : lines) {
            if (line.matches("^[0-9]\\..+") && tipCount < 3) {
                if (tipCount > 0) tips.append("\n");
                tips.append(line);
                tipCount++;
            }
        }

        return tips.length() > 0 ? tips.toString() : null;
    }

    // 계절 정보 추출
    private String extractSeasonalInfo(String[] lines) {
        StringBuilder seasonal = new StringBuilder();

        for (String line : lines) {
            if (line.contains("봄") || line.contains("여름") ||
                line.contains("가을") || line.contains("겨울")) {
                if (seasonal.length() > 0) seasonal.append("\n");
                seasonal.append(line);
            }
        }

        return seasonal.length() > 0 ? seasonal.toString() : null;
    }

    // 스케줄 실행 (매일 새벽 2시)
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledEnrichment() {
        log.info("정기 AI 보강 작업 시작");
        enrichTopPlacesWithAI(50);  // 매일 상위 50개씩
    }

    // 통계 정보
    public Map<String, Object> getAIEnrichmentStatistics() {
        long total = travelCandidateRepository.count();
        long aiEnriched = travelCandidateRepository.findAll().stream()
            .filter(c -> c.getEnrichmentStatus() != null && c.getEnrichmentStatus() >= 2)
            .count();

        long withTips = travelCandidateRepository.findAll().stream()
            .filter(c -> c.getTips() != null && !c.getTips().isEmpty())
            .count();

        long withHighlights = travelCandidateRepository.findAll().stream()
            .filter(c -> c.getHighlights() != null && !c.getHighlights().isEmpty())
            .count();

        return Map.of(
            "total", total,
            "aiEnriched", aiEnriched,
            "withTips", withTips,
            "withHighlights", withHighlights,
            "aiEnrichmentRate", (double) aiEnriched / total * 100
        );
    }
}