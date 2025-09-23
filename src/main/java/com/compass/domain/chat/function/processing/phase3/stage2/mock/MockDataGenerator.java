package com.compass.domain.chat.function.processing.phase3.stage2.mock;

import com.compass.domain.chat.function.processing.phase3.stage2.model.Stage1Result;
import com.compass.domain.chat.function.processing.phase3.stage2.model.TourPlace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

// Stage1 Mock 데이터 생성기 - 실제 Stage1에서 받을 데이터를 시뮬레이션
@Slf4j
@Component
public class MockDataGenerator {

    private static final Random random = new Random();

    // 서울 주요 지역의 실제 좌표
    private static final Map<String, double[]> SEOUL_REGIONS = Map.of(
        "홍대", new double[]{37.5563, 126.9220},
        "강남", new double[]{37.4979, 127.0276},
        "명동", new double[]{37.5636, 126.9869},
        "성수", new double[]{37.5446, 127.0565},
        "종로", new double[]{37.5729, 126.9793},
        "이태원", new double[]{37.5346, 126.9945},
        "잠실", new double[]{37.5113, 127.0980},
        "신촌", new double[]{37.5585, 126.9390}
    );

    // 시간 블록 정의
    private static final String[] TIME_BLOCKS = {
        "BREAKFAST", "MORNING_ACTIVITY", "LUNCH", "CAFE",
        "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"
    };

    // 카테고리 정의
    private static final String[] CATEGORIES = {
        "맛집", "카페", "관광지", "쇼핑", "문화시설", "액티비티", "공원"
    };

    // Stage1 결과 Mock 데이터 생성
    public Stage1Result generateStage1Result(String threadId, int tripDays) {
        log.info("Stage1 Mock 데이터 생성 시작: tripDays={}", tripDays);

        List<TourPlace> allPlaces = new ArrayList<>();

        // 각 일차별로 장소 생성
        for (int day = 1; day <= tripDays; day++) {
            List<TourPlace> dayPlaces = generatePlacesForDay(day);
            allPlaces.addAll(dayPlaces);
            log.info("Day {} 장소 생성 완료: {}개", day, dayPlaces.size());
        }

        Stage1Result result = Stage1Result.builder()
            .id(UUID.randomUUID().toString())
            .threadId(threadId)
            .places(allPlaces)
            .createdAt(LocalDateTime.now())
            .status("COMPLETED")
            .build();

        log.info("Stage1 Mock 데이터 생성 완료: 총 {}개 장소", allPlaces.size());
        return result;
    }

    // 특정 일차의 장소들 생성 (각 시간 블록별로 10-15개씩)
    private List<TourPlace> generatePlacesForDay(int day) {
        List<TourPlace> dayPlaces = new ArrayList<>();
        List<String> regionNames = new ArrayList<>(SEOUL_REGIONS.keySet());

        // 각 시간 블록별로 장소 생성
        for (String timeBlock : TIME_BLOCKS) {
            int placeCount = 10 + random.nextInt(6); // 10-15개

            for (int i = 0; i < placeCount; i++) {
                // 랜덤하게 여러 지역에서 선택 (실제로는 다양한 지역의 장소들)
                String selectedRegion = regionNames.get(random.nextInt(regionNames.size()));
                TourPlace place = generatePlace(day, timeBlock, selectedRegion, i + 1);
                dayPlaces.add(place);
            }
        }

        return dayPlaces;
    }

    // 개별 장소 생성
    private TourPlace generatePlace(int day, String timeBlock, String region, int index) {
        double[] baseCoord = SEOUL_REGIONS.get(region);

        // 기준 좌표에서 약간의 변화 추가 (반경 2km 이내)
        double latVariation = (random.nextDouble() - 0.5) * 0.02;
        double lngVariation = (random.nextDouble() - 0.5) * 0.02;

        String category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        String placeName = generatePlaceName(region, timeBlock, category, index);

        return TourPlace.builder()
            .id(String.format("mock_%d_%s_%d", day, timeBlock, index))
            .name(placeName)
            .timeBlock(timeBlock)
            .day(day)
            .recommendTime(generateRecommendTime(timeBlock))
            .latitude(baseCoord[0] + latVariation)
            .longitude(baseCoord[1] + lngVariation)
            .address(region + " " + (100 + random.nextInt(900)) + "번지")
            .category(category)
            .operatingHours(generateOperatingHours(timeBlock))
            .closedDays(random.nextBoolean() ? "연중무휴" : "매주 " + getDayOfWeek())
            .petAllowed(random.nextBoolean())
            .parkingAvailable(random.nextBoolean())
            .rating(3.5 + random.nextDouble() * 1.5) // 3.5 ~ 5.0
            .priceLevel(generatePriceLevel())
            .isTrendy(random.nextDouble() > 0.7) // 30% 확률로 트렌디
            .build();
    }

    // 장소명 생성
    private String generatePlaceName(String region, String timeBlock, String category, int index) {
        Map<String, List<String>> placeTemplates = Map.of(
            "맛집", List.of(region + " 맛집", region + " 한식당", region + " 레스토랑"),
            "카페", List.of(region + " 카페", region + " 커피숍", region + " 베이커리"),
            "관광지", List.of(region + " 명소", region + " 거리", region + " 타워"),
            "쇼핑", List.of(region + " 백화점", region + " 몰", region + " 시장"),
            "문화시설", List.of(region + " 박물관", region + " 미술관", region + " 극장"),
            "액티비티", List.of(region + " 체험관", region + " 놀이공원", region + " 스포츠센터"),
            "공원", List.of(region + " 공원", region + " 산책로", region + " 광장")
        );

        List<String> templates = placeTemplates.getOrDefault(category, List.of(region + " 장소"));
        return templates.get(random.nextInt(templates.size())) + " " + index;
    }

    // 추천 시간 생성
    private String generateRecommendTime(String timeBlock) {
        Map<String, String> timeMap = Map.of(
            "BREAKFAST", "08:00-10:00",
            "MORNING_ACTIVITY", "10:00-12:00",
            "LUNCH", "12:00-14:00",
            "CAFE", "14:00-16:00",
            "AFTERNOON_ACTIVITY", "16:00-18:00",
            "DINNER", "18:00-20:00",
            "EVENING_ACTIVITY", "20:00-22:00"
        );
        return timeMap.getOrDefault(timeBlock, "09:00-18:00");
    }

    // 운영 시간 생성
    private String generateOperatingHours(String timeBlock) {
        if (timeBlock.contains("BREAKFAST")) {
            return "07:00-15:00";
        } else if (timeBlock.contains("DINNER") || timeBlock.contains("EVENING")) {
            return "11:00-23:00";
        } else {
            return "09:00-21:00";
        }
    }

    // 요일 생성
    private String getDayOfWeek() {
        String[] days = {"월요일", "화요일", "수요일", "목요일", "금요일"};
        return days[random.nextInt(days.length)];
    }

    // 가격대 생성
    private String generatePriceLevel() {
        String[] levels = {"$", "$$", "$$$", "$$$$"};
        return levels[random.nextInt(levels.length)];
    }
}