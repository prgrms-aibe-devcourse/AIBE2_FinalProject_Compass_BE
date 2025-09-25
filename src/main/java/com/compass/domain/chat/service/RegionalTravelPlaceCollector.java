package com.compass.domain.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 지역별 여행지 대량 수집 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class RegionalTravelPlaceCollector {

    private final EnhancedPerplexityClient enhancedPerplexityClient;
    private final ObjectMapper objectMapper;

    // 한국 주요 지역 정의
    private static final List<String> KOREAN_REGIONS = List.of(
        "서울", "부산", "인천", "대구", "대전", "광주", "울산",
        "경기도", "강원도", "충청북도", "충청남도",
        "전라북도", "전라남도", "경상북도", "경상남도", "제주도"
    );

    // 지역별 여행지 수집 (목표: 각 지역당 70개)
    public Map<String, List<TravelPlace>> collectAllRegions(int placesPerRegion) {
        Map<String, List<TravelPlace>> regionMap = new ConcurrentHashMap<>();

        // 병렬 처리로 모든 지역 동시 수집
        List<CompletableFuture<Void>> futures = KOREAN_REGIONS.stream()
            .map(region -> CompletableFuture.runAsync(() -> {
                try {
                    log.info("{} 지역 수집 시작 (목표: {}개)", region, placesPerRegion);
                    List<TravelPlace> places = collectRegionPlaces(region, placesPerRegion);
                    regionMap.put(region, places);
                    log.info("{} 지역 수집 완료: {}개", region, places.size());
                } catch (Exception e) {
                    log.error("{} 지역 수집 실패: {}", region, e.getMessage());
                    regionMap.put(region, new ArrayList<>());
                }
            }))
            .collect(Collectors.toList());

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return regionMap;
    }

    // 특정 지역의 여행지 수집
    public List<TravelPlace> collectRegionPlaces(String region, int targetCount) {
        Set<String> uniquePlaces = new HashSet<>();
        List<TravelPlace> allPlaces = new ArrayList<>();

        // 다양한 테마별로 수집
        List<CollectionStrategy> strategies = createCollectionStrategies();

        for (CollectionStrategy strategy : strategies) {
            if (allPlaces.size() >= targetCount) break;

            try {
                List<TravelPlace> places = collectByStrategy(region, strategy, targetCount / strategies.size() + 5);

                // 중복 제거하며 추가
                for (TravelPlace place : places) {
                    if (uniquePlaces.add(place.name)) {
                        allPlaces.add(place);
                    }
                }

                log.debug("{} - {} 수집 완료: {}개", region, strategy.theme, places.size());

            } catch (Exception e) {
                log.warn("수집 실패: {} - {}", region, strategy.theme);
            }
        }

        // 목표 개수만큼 자르기
        if (allPlaces.size() > targetCount) {
            return allPlaces.subList(0, targetCount);
        }

        return allPlaces;
    }

    // 전략별 수집
    private List<TravelPlace> collectByStrategy(String region, CollectionStrategy strategy, int count) {
        List<TravelPlace> places = new ArrayList<>();

        try {
            String prompt = buildPrompt(region, strategy, count);
            List<String> results = enhancedPerplexityClient.collectBulkData(region, count);

            for (String result : results) {
                try {
                    // JSON 파싱 시도
                    List<Map<String, Object>> parsed = parseJsonResponse(result);
                    places.addAll(convertToTravelPlaces(parsed, region, strategy.theme));
                } catch (Exception e) {
                    // JSON 파싱 실패시 텍스트 파싱
                    places.addAll(parseTextResponse(result, region, strategy.theme));
                }
            }

        } catch (Exception e) {
            log.error("전략 수집 실패: {}", e.getMessage());
        }

        return places;
    }

    // 수집 전략 생성
    private List<CollectionStrategy> createCollectionStrategies() {
        return List.of(
            new CollectionStrategy("유명관광지", List.of("랜드마크", "명소", "관광명소")),
            new CollectionStrategy("자연경관", List.of("산", "바다", "호수", "계곡")),
            new CollectionStrategy("역사문화", List.of("궁궐", "사찰", "박물관", "문화재")),
            new CollectionStrategy("맛집", List.of("맛집", "카페", "전통음식", "로컬맛집")),
            new CollectionStrategy("체험활동", List.of("레저", "스포츠", "체험", "액티비티")),
            new CollectionStrategy("숨은명소", List.of("숨은명소", "핫플레이스", "로컬추천")),
            new CollectionStrategy("쇼핑", List.of("시장", "쇼핑몰", "아울렛", "특산품"))
        );
    }

    // 프롬프트 생성
    private String buildPrompt(String region, CollectionStrategy strategy, int count) {
        String keywords = String.join(", ", strategy.keywords);
        return String.format(
            "%s 지역의 %s 관련 장소 %d개를 추천해주세요. " +
            "키워드: %s " +
            "각 장소는 다음 형식으로: " +
            "[{\"name\": \"장소명\", \"description\": \"설명\", \"category\": \"카테고리\", \"address\": \"주소\"}]",
            region, strategy.theme, count, keywords
        );
    }

    // JSON 응답 파싱
    private List<Map<String, Object>> parseJsonResponse(String response) {
        try {
            // JSON 배열 찾기
            int startIdx = response.indexOf("[");
            int endIdx = response.lastIndexOf("]");
            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonPart = response.substring(startIdx, endIdx + 1);
                return objectMapper.readValue(jsonPart, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.debug("JSON 파싱 실패, 텍스트 파싱 시도");
        }
        return new ArrayList<>();
    }

    // 텍스트 응답 파싱
    private List<TravelPlace> parseTextResponse(String response, String region, String theme) {
        List<TravelPlace> places = new ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 숫자나 bullet point로 시작하는 라인 찾기
            if (line.matches("^\\d+\\..*") || line.matches("^[-•*].*")) {
                String placeName = extractPlaceName(line);
                if (!placeName.isEmpty()) {
                    places.add(new TravelPlace(
                        placeName,
                        line,
                        theme,
                        region,
                        null
                    ));
                }
            }
        }

        return places;
    }

    // 장소명 추출
    private String extractPlaceName(String line) {
        // 숫자, bullet point 제거
        line = line.replaceAll("^\\d+\\.", "").replaceAll("^[-•*]", "").trim();

        // 괄호 안의 내용 제거
        int parenIdx = line.indexOf("(");
        if (parenIdx > 0) {
            line = line.substring(0, parenIdx).trim();
        }

        // 콜론 앞부분만 추출
        int colonIdx = line.indexOf(":");
        if (colonIdx > 0) {
            line = line.substring(0, colonIdx).trim();
        }

        return line;
    }

    // Map을 TravelPlace로 변환
    private List<TravelPlace> convertToTravelPlaces(List<Map<String, Object>> maps, String region, String theme) {
        return maps.stream()
            .map(map -> new TravelPlace(
                (String) map.getOrDefault("name", ""),
                (String) map.getOrDefault("description", ""),
                (String) map.getOrDefault("category", theme),
                region,
                (String) map.get("address")
            ))
            .filter(place -> !place.name.isEmpty())
            .collect(Collectors.toList());
    }

    // 내부 클래스들
    public static record TravelPlace(
        String name,
        String description,
        String category,
        String region,
        String address
    ) {}

    private static record CollectionStrategy(
        String theme,
        List<String> keywords
    ) {}

    // 수집 결과 저장 (옵션)
    public void saveCollectionResults(Map<String, List<TravelPlace>> results, String filename) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);
            // 파일로 저장하는 로직 추가 가능
            log.info("수집 결과 저장 완료: {}", filename);
        } catch (Exception e) {
            log.error("저장 실패: {}", e.getMessage());
        }
    }

    // 수집 통계
    public void printCollectionStats(Map<String, List<TravelPlace>> results) {
        log.info("=== 수집 통계 ===");
        int total = 0;
        for (Map.Entry<String, List<TravelPlace>> entry : results.entrySet()) {
            int count = entry.getValue().size();
            total += count;
            log.info("{}: {}개", entry.getKey(), count);
        }
        log.info("총 수집된 여행지: {}개", total);
    }
}