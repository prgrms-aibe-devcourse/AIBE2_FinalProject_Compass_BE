package com.compass.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// Perplexity API 클라이언트 (일단 Mock 구현)
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedPerplexityClient {

    public Map<String, Object> searchTravelPlaces(String region, String category, Integer maxResults) {
        log.info("Perplexity 검색: region={}, category={}, maxResults={}", region, category, maxResults);

        // Mock 응답
        return Map.of(
            "status", "success",
            "places", List.of(
                Map.of(
                    "name", region + " " + category + " 1",
                    "category", category,
                    "rating", 4.5,
                    "address", region + " 주소",
                    "description", "인기 " + category
                ),
                Map.of(
                    "name", region + " " + category + " 2",
                    "category", category,
                    "rating", 4.2,
                    "address", region + " 주소",
                    "description", "추천 " + category
                )
            )
        );
    }

    public Map<String, Object> searchWithOptions(Map<String, Object> options) {
        log.info("Perplexity 고급 검색: options={}", options);

        String query = (String) options.getOrDefault("query", "");
        Integer maxResults = (Integer) options.getOrDefault("max_results", 10);

        // Mock 응답
        return Map.of(
            "status", "success",
            "query", query,
            "results", List.of(
                Map.of(
                    "title", "검색 결과 1",
                    "content", query + "에 대한 내용",
                    "source", "https://example.com/1"
                ),
                Map.of(
                    "title", "검색 결과 2",
                    "content", query + "에 대한 추가 정보",
                    "source", "https://example.com/2"
                )
            ),
            "total", maxResults
        );
    }

    public List<String> collectBulkData(String region, int count) {
        log.info("Perplexity 대량 데이터 수집: region={}, count={}", region, count);

        // Mock 데이터 반환
        return List.of(
            region + " 명소 1",
            region + " 명소 2",
            region + " 명소 3",
            region + " 맛집 1",
            region + " 맛집 2"
        );
    }
}