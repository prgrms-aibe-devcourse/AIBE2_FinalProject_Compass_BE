package com.compass.domain.chat.service;

import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.service.GooglePlacesCollector.GooglePlace;
import com.compass.domain.chat.service.GooglePlacesCollector.TimeBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pre-Stage - Google Places API 통합 서비스
 * Stage 1 실행 전에 시간블럭과 카테고리를 고려하여 인기도/리뷰 기반 정렬된 여행지 데이터 수집
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreStageIntegrationService {

    private final GooglePlacesCollector googlePlacesCollector;

    /**
     * Stage 1 메인 수집 메서드
     * 시간블럭별로 구분된 고품질 여행지 데이터 수집
     *
     * @param destination 여행 목적지 (예: "서울", "부산")
     * @param tripDays 여행 일수
     * @param travelStyle 여행 스타일 (선택사항)
     * @return Stage 2에서 사용할 TravelPlace 리스트
     */
    public List<TravelPlace> collectHighQualityPlacesForStage1(
            String destination,
            int tripDays,
            List<String> travelStyle) {

        log.info("=== Stage 1 시작: Google Places API를 통한 고품질 여행지 수집 ===");
        log.info("목적지: {}, 여행일수: {}일, 여행스타일: {}", destination, tripDays, travelStyle);

        // 하루당 필요한 장소 수 계산 (시간블럭당 2-3개 * 6개 시간블럭)
        int placesPerTimeBlock = calculatePlacesPerTimeBlock(tripDays);

        // 시간블럭별 장소 수집
        Map<TimeBlock, List<GooglePlace>> timeBlockPlaces =
            googlePlacesCollector.collectPlacesByTimeBlock(destination, placesPerTimeBlock);

        // Google Place를 TravelPlace로 변환하면서 시간블럭 정보 추가
        List<TravelPlace> allPlaces = convertToTravelPlaces(timeBlockPlaces);

        // 카테고리 태그 기반 추가 수집 (여행스타일 고려)
        if (travelStyle != null && !travelStyle.isEmpty()) {
            enrichWithStylePreferences(allPlaces, destination, travelStyle);
        }

        // 최종 품질 점수로 재정렬
        allPlaces.sort((p1, p2) -> Double.compare(p2.getQualityScore(), p1.getQualityScore()));

        log.info("Stage 1 완료: 총 {}개 고품질 여행지 수집", allPlaces.size());
        logStatistics(allPlaces);

        return allPlaces;
    }

    /**
     * 여행 일수에 따른 시간블럭당 필요 장소 수 계산
     */
    private int calculatePlacesPerTimeBlock(int tripDays) {
        // 기본: 시간블럭당 5개
        // 여행일수가 많으면 더 많은 대안 필요
        int baseCount = 5;
        int additionalPerDay = tripDays > 3 ? 2 : 0;
        return baseCount + additionalPerDay;
    }

    /**
     * GooglePlace를 TravelPlace로 변환
     */
    private List<TravelPlace> convertToTravelPlaces(Map<TimeBlock, List<GooglePlace>> timeBlockPlaces) {
        List<TravelPlace> travelPlaces = new ArrayList<>();

        for (Map.Entry<TimeBlock, List<GooglePlace>> entry : timeBlockPlaces.entrySet()) {
            TimeBlock timeBlock = entry.getKey();
            List<GooglePlace> places = entry.getValue();

            for (GooglePlace gPlace : places) {
                TravelPlace travelPlace = gPlace.toTravelPlace();

                // 시간블럭 정보를 메타데이터로 추가
                if (travelPlace.getDescription() != null) {
                    travelPlace.setDescription(travelPlace.getDescription() +
                        " | 추천시간: " + timeBlock.name());
                } else {
                    travelPlace.setDescription("추천시간: " + timeBlock.name());
                }

                travelPlaces.add(travelPlace);
            }
        }

        return travelPlaces;
    }

    /**
     * 여행 스타일에 따른 추가 장소 보강
     */
    private void enrichWithStylePreferences(
            List<TravelPlace> places,
            String destination,
            List<String> travelStyle) {

        Set<String> categoryTags = mapStyleToCategories(travelStyle);

        if (!categoryTags.isEmpty()) {
            Map<String, List<GooglePlace>> styleBasedPlaces =
                googlePlacesCollector.collectByCategories(destination, categoryTags, 3);

            // 중복 제거하며 추가
            Set<String> existingIds = places.stream()
                .map(TravelPlace::getPlaceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            for (List<GooglePlace> categoryPlaces : styleBasedPlaces.values()) {
                for (GooglePlace gPlace : categoryPlaces) {
                    if (!existingIds.contains(gPlace.placeId)) {
                        places.add(gPlace.toTravelPlace());
                        existingIds.add(gPlace.placeId);
                    }
                }
            }
        }
    }

    /**
     * 여행 스타일을 카테고리 태그로 매핑
     */
    private Set<String> mapStyleToCategories(List<String> travelStyle) {
        Set<String> categories = new HashSet<>();

        for (String style : travelStyle) {
            switch (style.toLowerCase()) {
                case "문화":
                case "역사":
                    categories.add("문화");
                    categories.add("관광지");
                    break;
                case "맛집":
                case "음식":
                case "미식":
                    categories.add("맛집");
                    categories.add("카페");
                    break;
                case "쇼핑":
                    categories.add("쇼핑");
                    break;
                case "액티비티":
                case "활동":
                case "레저":
                    categories.add("액티비티");
                    break;
                case "자연":
                case "힐링":
                    categories.add("자연");
                    categories.add("관광지");
                    break;
                case "나이트라이프":
                case "야경":
                    categories.add("나이트라이프");
                    break;
                default:
                    // 기본값으로 관광지 추가
                    categories.add("관광지");
            }
        }

        return categories;
    }

    /**
     * 수집된 데이터 통계 로깅
     */
    private void logStatistics(List<TravelPlace> places) {
        if (places.isEmpty()) return;

        // 카테고리별 통계
        Map<String, Long> categoryCount = places.stream()
            .collect(Collectors.groupingBy(
                p -> p.getCategory() != null ? p.getCategory() : "기타",
                Collectors.counting()
            ));

        // 평균 평점 및 리뷰
        double avgRating = places.stream()
            .filter(p -> p.getRating() != null)
            .mapToDouble(TravelPlace::getRating)
            .average()
            .orElse(0.0);

        double avgReviews = places.stream()
            .filter(p -> p.getReviewCount() != null)
            .mapToInt(TravelPlace::getReviewCount)
            .average()
            .orElse(0.0);

        // 신뢰도 분포
        Map<String, Long> reliabilityCount = places.stream()
            .collect(Collectors.groupingBy(
                TravelPlace::getReliabilityLevel,
                Collectors.counting()
            ));

        log.info("=== Stage 1 수집 통계 ===");
        log.info("카테고리 분포: {}", categoryCount);
        log.info("평균 평점: {:.1f}, 평균 리뷰 수: {:.0f}", avgRating, avgReviews);
        log.info("신뢰도 분포: {}", reliabilityCount);

        // 상위 5개 장소 로깅
        log.info("=== 상위 5개 고품질 장소 ===");
        places.stream()
            .limit(5)
            .forEach(place -> {
                log.info("- {} ({}): 평점 {}, 리뷰 {}개, 신뢰도: {}",
                    place.getName(),
                    place.getCategory(),
                    place.getRating(),
                    place.getReviewCount(),
                    place.getReliabilityLevel()
                );
            });
    }

    /**
     * Stage 2를 위한 클러스터링 준비 데이터 생성
     * 시간블럭별로 구분된 장소 맵 반환
     */
    public Map<String, List<TravelPlace>> prepareForStage2Clustering(List<TravelPlace> places) {
        Map<String, List<TravelPlace>> timeBlockMap = new HashMap<>();

        for (TravelPlace place : places) {
            // 설명에서 시간블럭 정보 추출
            String timeBlock = extractTimeBlock(place.getDescription());
            timeBlockMap.computeIfAbsent(timeBlock, k -> new ArrayList<>()).add(place);
        }

        return timeBlockMap;
    }

    /**
     * 설명에서 시간블럭 정보 추출
     */
    private String extractTimeBlock(String description) {
        if (description == null) return "AFTERNOON_ACTIVITY";

        for (TimeBlock tb : TimeBlock.values()) {
            if (description.contains(tb.name())) {
                return tb.name();
            }
        }

        return "AFTERNOON_ACTIVITY"; // 기본값
    }
}