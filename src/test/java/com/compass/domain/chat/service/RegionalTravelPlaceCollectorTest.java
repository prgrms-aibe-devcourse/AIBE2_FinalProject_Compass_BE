package com.compass.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionalTravelPlaceCollectorTest {

    private static final Logger log = LoggerFactory.getLogger(RegionalTravelPlaceCollectorTest.class);

    @Mock
    private EnhancedPerplexityClient enhancedPerplexityClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RegionalTravelPlaceCollector collector;

    @BeforeEach
    void setUp() {
        // Mock 응답 설정
        String mockResponse = """
            1. 경복궁 - 조선시대 대표 궁궐
            2. 남산타워 - 서울의 랜드마크
            3. 명동 - 쇼핑과 먹거리의 중심
            4. 북촌한옥마을 - 전통 한옥마을
            5. 광장시장 - 전통시장과 먹거리
            6. 청계천 - 도심 속 휴식공간
            7. 인사동 - 전통문화의 거리
            """;

        when(enhancedPerplexityClient.collectBulkData(anyString(), anyInt()))
            .thenReturn(List.of(mockResponse));
    }

    @Test
    void testCollectRegionPlaces_70개_수집() {
        // Given
        String region = "서울";
        int targetCount = 70;

        // When
        log.info("=== {} 지역 {}개 수집 테스트 시작 ===", region, targetCount);
        List<RegionalTravelPlaceCollector.TravelPlace> places =
            collector.collectRegionPlaces(region, targetCount);

        // Then
        assertNotNull(places);
        log.info("수집된 장소 개수: {}", places.size());

        // 최소한 몇 개는 수집되어야 함
        assertTrue(places.size() > 0);

        // 수집된 장소 출력
        log.info("=== 수집된 장소 목록 ===");
        for (int i = 0; i < Math.min(10, places.size()); i++) {
            var place = places.get(i);
            log.info("{}. {} - {} ({})",
                i + 1, place.name(), place.category(), place.region());
        }

        // 중복 확인
        long uniqueCount = places.stream()
            .map(RegionalTravelPlaceCollector.TravelPlace::name)
            .distinct()
            .count();

        log.info("중복 제거 후 고유 장소: {}개", uniqueCount);
        assertEquals(places.size(), uniqueCount, "중복된 장소가 없어야 함");
    }

    @Test
    void testCollectAllRegions_전체지역_수집() {
        // Given
        int placesPerRegion = 10; // 테스트용으로 적은 수

        // When
        log.info("=== 전체 지역 수집 테스트 시작 (각 {}개) ===", placesPerRegion);
        Map<String, List<RegionalTravelPlaceCollector.TravelPlace>> results =
            collector.collectAllRegions(placesPerRegion);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());

        log.info("=== 지역별 수집 결과 ===");
        int totalCount = 0;
        for (Map.Entry<String, List<RegionalTravelPlaceCollector.TravelPlace>> entry : results.entrySet()) {
            int size = entry.getValue().size();
            totalCount += size;
            log.info("{}: {}개", entry.getKey(), size);
        }

        log.info("총 수집된 장소: {}개", totalCount);
        assertTrue(totalCount > 0, "최소한 일부 장소는 수집되어야 함");
    }

    @Test
    void testCollectionStrategies_전략별_수집() {
        // Private 메서드 테스트를 위한 리플렉션
        var strategies = ReflectionTestUtils.invokeMethod(collector, "createCollectionStrategies");

        assertNotNull(strategies);
        assertTrue(strategies instanceof List);

        List<?> strategyList = (List<?>) strategies;
        log.info("=== 수집 전략 목록 ===");
        for (Object strategy : strategyList) {
            log.info("전략: {}", strategy);
        }

        assertEquals(7, strategyList.size(), "7개의 수집 전략이 있어야 함");
    }

    @Test
    void testParseResponse_JSON_파싱() {
        // Given
        String jsonResponse = """
            [
                {"name": "경복궁", "description": "조선 왕조의 정궁", "category": "역사", "address": "서울시 종로구"},
                {"name": "남산타워", "description": "서울의 랜드마크", "category": "관광", "address": "서울시 용산구"}
            ]
            """;

        when(enhancedPerplexityClient.collectBulkData("서울", 2))
            .thenReturn(List.of(jsonResponse));

        // When
        var places = collector.collectRegionPlaces("서울", 2);

        // Then
        assertNotNull(places);
        assertTrue(places.size() > 0);

        log.info("=== JSON 파싱 결과 ===");
        for (var place : places) {
            log.info("{}: {} - {}", place.name(), place.description(), place.address());
        }
    }

    @Test
    void testDeduplication_중복제거() {
        // Given - 중복 포함된 응답
        String duplicateResponse = """
            1. 경복궁
            2. 남산타워
            3. 경복궁
            4. 명동
            5. 남산타워
            """;

        when(enhancedPerplexityClient.collectBulkData("서울", 5))
            .thenReturn(List.of(duplicateResponse));

        // When
        var places = collector.collectRegionPlaces("서울", 5);

        // Then
        long uniqueCount = places.stream()
            .map(RegionalTravelPlaceCollector.TravelPlace::name)
            .distinct()
            .count();

        assertEquals(places.size(), uniqueCount, "중복이 제거되어야 함");
        log.info("중복 제거 테스트 통과: {}개 고유 장소", uniqueCount);
    }
}