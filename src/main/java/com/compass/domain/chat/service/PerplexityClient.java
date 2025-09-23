package com.compass.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// Perplexity API 클라이언트
@Slf4j
@Service
public class PerplexityClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${PERPLEXITY_API_KEY}")
    private String apiKey;

    @Value("${PERPLEXITY_API_URL:https://api.perplexity.ai/chat/completions}")
    private String apiUrl;

    @Value("${PERPLEXITY_API_TIMEOUT:30}")
    private int timeoutSeconds;

    @Value("${PERPLEXITY_API_MAX_RETRIES:3}")
    private int maxRetries;

    // Perplexity API로 검색 쿼리 전송
    public CompletableFuture<String> searchAsync(String query) {
        return CompletableFuture.supplyAsync(() -> {
            return searchWithRetry(query);
        });
    }

    // 재시도 로직이 포함된 검색
    private String searchWithRetry(String query) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("Perplexity API 검색 시도 {}: {}", attempt, query);
                return performSearch(query);

            } catch (Exception e) {
                lastException = e;
                log.warn("Perplexity API 검색 실패 (시도 {}/{}): {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        TimeUnit.SECONDS.sleep(attempt * 2); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("검색 중단됨", ie);
                    }
                }
            }
        }

        log.error("Perplexity API 검색 최종 실패: {}", query, lastException);
        throw new RuntimeException("Perplexity API 검색 실패", lastException);
    }

    // 실제 API 호출 수행
    private String performSearch(String query) {
        var headers = createHeaders();
        var requestBody = createRequestBody(query);
        var entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.POST,
            entity,
            Map.class
        );

        return parseResponse(response);
    }

    // HTTP 헤더 생성
    private HttpHeaders createHeaders() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    // 요청 본문 생성
    private Map<String, Object> createRequestBody(String query) {
        return Map.of(
            "model", "sonar", // Perplexity API 최신 온라인 모델
            "messages", new Object[]{
                Map.of("role", "user", "content", query)
            },
            "max_tokens", 1000,
            "temperature", 0.2
        );
    }

    // 응답 파싱
    private String parseResponse(ResponseEntity<Map> response) {
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            var body = response.getBody();
            // Object[] 대신 List<?>를 사용하여 유연하게 타입을 처리합니다.
            var choices = (java.util.List<?>) body.get("choices");

            if (choices != null && !choices.isEmpty()) {
                var choice = (Map<String, Object>) choices.get(0);
                var message = (Map<String, Object>) choice.get("message");
                return (String) message.get("content");
            }
        }

        throw new RuntimeException("Perplexity API 응답 파싱 실패");
    }

    // 동기 검색 (간단한 쿼리용)
    public String search(String query) {
        return searchWithRetry(query);
    }
}