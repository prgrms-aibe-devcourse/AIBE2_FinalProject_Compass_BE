package com.compass.domain.chat.service;

import com.compass.domain.chat.function.external.SearchWithPerplexityFunction;
import com.compass.domain.chat.function.external.SearchTourAPIFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Stage 1: 장소 리스트업 서비스
 * 
 * 목적지별로 카테고리별 장소를 수집하고 여행 스타일에 맞게 필터링하여
 * Stage 2, 3에서 사용할 수 있는 풍부한 장소 데이터를 제공합니다.
 * 
 * 주요 기능:
 * - 카테고리별 장소 수집 (관광지, 맛집, 쇼핑, 문화시설 등)
 * - 여행 스타일별 우선순위 적용
 * - 각 카테고리별 최대 100개 장소 제공
 * - Stage 2, 3 전달용 데이터 구조 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSelectionService {

    private final SearchWithPerplexityFunction perplexitySearch;
    private final SearchTourAPIFunction tourAPISearch;
    private final PlaceDeduplicator placeDeduplicator;
    private final PlaceFilterService placeFilterService;
    private final Stage1DatabaseService stage1DatabaseService;

    // 카테고리별 검색 키워드 매핑
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
        "관광지", Arrays.asList("관광지", "명소", "랜드마크", "유적지", "공원", "전망대"),
        "맛집", Arrays.asList("맛집", "음식점", "카페", "베이커리", "술집", "바"),
        "쇼핑", Arrays.asList("쇼핑몰", "시장", "백화점", "아울렛", "상점", "기념품"),
        "문화시설", Arrays.asList("박물관", "미술관", "극장", "콘서트홀", "문화센터", "갤러리"),
        "자연", Arrays.asList("해변", "산", "강", "호수", "숲", "정원", "자연공원"),
        "액티비티", Arrays.asList("체험", "놀이공원", "워터파크", "스포츠", "레저", "액티비티"),
        "숙박", Arrays.asList("호텔", "펜션", "게스트하우스", "리조트", "모텔"),
        "교통", Arrays.asList("역", "터미널", "공항", "항구", "교통")
    );

    // 여행 스타일별 카테고리 우선순위
    private static final Map<String, List<String>> TRAVEL_STYLE_PRIORITIES = Map.of(
        "CULTURAL", Arrays.asList("문화시설", "관광지", "맛집", "쇼핑", "자연", "액티비티"),
        "FOODIE", Arrays.asList("맛집", "쇼핑", "관광지", "문화시설", "자연", "액티비티"),
        "NATURE", Arrays.asList("자연", "액티비티", "관광지", "맛집", "문화시설", "쇼핑"),
        "SHOPPING", Arrays.asList("쇼핑", "맛집", "관광지", "문화시설", "자연", "액티비티"),
        "ADVENTURE", Arrays.asList("액티비티", "자연", "관광지", "맛집", "쇼핑", "문화시설"),
        "RELAXATION", Arrays.asList("자연", "관광지", "맛집", "문화시설", "쇼핑", "액티비티")
    );

    /**
     * Stage 1 메인 메서드: 7블록 전략으로 장소 리스트업
     * 
     * @param destination 목적지
     * @param travelInfo 여행 정보
     * @return Stage1Output (카테고리별 장소 데이터)
     */
    public Stage1Output selectPlaces(String destination, TravelInfo travelInfo) {
        return selectPlacesWithThreadId(destination, travelInfo, null);
    }

    /**
     * Stage 1 메인 메서드: 7블록 전략으로 장소 리스트업 (스레드 ID 포함)
     * 
     * @param destination 목적지
     * @param travelInfo 여행 정보
     * @param threadId 스레드 ID (DB 저장용, null 가능)
     * @return Stage1Output (카테고리별 장소 데이터)
     */
    public Stage1Output selectPlacesWithThreadId(String destination, TravelInfo travelInfo, String threadId) {
        log.info("=== Stage 1 장소 리스트업 시작 (시간대별 블록 전략) ===");
        log.info("목적지: {}, 여행스타일: {}, {}일 여행", 
                destination, travelInfo.travelStyle(), travelInfo.tripDays());

        try {
            long startTime = System.currentTimeMillis();
            
            // 🔥 시간대별 블록 검색 전략 (일수별로 80개씩)
            List<PlaceDeduplicator.TourPlace> allPlaces = new ArrayList<>();
            
            // 일수별로 시간대 블록 생성
            for (int day = 1; day <= travelInfo.tripDays(); day++) {
                log.info("Day {} 시간대별 블록 검색 시작", day);
                
                // 각 시간대 블록별로 Perplexity 검색
                allPlaces.addAll(collectTimeBlockPlaces(destination, "BREAKFAST", day, travelInfo.travelStyle(), 10));
                allPlaces.addAll(collectTimeBlockPlaces(destination, "MORNING_ACTIVITY", day, travelInfo.travelStyle(), 15));
                allPlaces.addAll(collectTimeBlockPlaces(destination, "LUNCH", day, travelInfo.travelStyle(), 10));
                allPlaces.addAll(collectTimeBlockPlaces(destination, "CAFE", day, travelInfo.travelStyle(), 10));
                allPlaces.addAll(collectTimeBlockPlaces(destination, "AFTERNOON_ACTIVITY", day, travelInfo.travelStyle(), 15));
                allPlaces.addAll(collectTimeBlockPlaces(destination, "DINNER", day, travelInfo.travelStyle(), 10));
                allPlaces.addAll(collectTimeBlockPlaces(destination, "EVENING_ACTIVITY", day, travelInfo.travelStyle(), 10));
                
                log.info("Day {} 완료: 80개 블록 수집", day);
            }

            log.info("시간대별 블록 수집 완료: 총 {}개 장소 ({}일 × 80개)", allPlaces.size(), travelInfo.tripDays());

            // 중복 제거
            List<PlaceDeduplicator.TourPlace> uniquePlaces = placeDeduplicator.deduplicate(allPlaces);
            log.info("중복 제거 후: {}개 장소", uniquePlaces.size());

            // 🔥 1차 DB 저장 (Perplexity 원본 데이터)
            if (threadId != null) {
                try {
                    stage1DatabaseService.savePrimaryResult(threadId, uniquePlaces);
                    log.info("1차 DB 저장 완료: threadId={}", threadId);
                } catch (Exception e) {
                    log.error("1차 DB 저장 실패: threadId={}", threadId, e);
                    // DB 저장 실패해도 계속 진행
                }
            }

            // 🔥 순차적 정보 보충 (새 가이드 방식)
            List<PlaceDeduplicator.TourPlace> enhancedPlaces = enhancePlacesSequentially(uniquePlaces, destination);
            log.info("순차적 정보 보충 완료: {}개 장소", enhancedPlaces.size());

            // 카테고리별 분류 (보완된 데이터 사용)
            Map<String, List<PlaceDeduplicator.TourPlace>> categoryPlaces = enhancedPlaces.stream()
                .collect(Collectors.groupingBy(PlaceDeduplicator.TourPlace::category));

            // 통계 정보 생성 (보완된 데이터 사용)
            PlaceStatistics statistics = generateStatistics(categoryPlaces, enhancedPlaces);
            
            long endTime = System.currentTimeMillis();
            
            log.info("=== Stage 1 완료 (7블록 전략) ===");
            log.info("실행 시간: {}ms", endTime - startTime);
            log.info("총 장소 수: {} 개", uniquePlaces.size());
            categoryPlaces.forEach((category, places) -> 
                log.info("  - {}: {} 개", category, places.size()));

            return new Stage1Output(
                enhancedPlaces,
                categoryPlaces,
                statistics,
                travelInfo.tripDays(),
                generateWarnings(categoryPlaces, travelInfo)
            );

        } catch (Exception e) {
            log.error("Stage 1 장소 선별 실패", e);
            return createEmptyOutput(travelInfo.tripDays(), e.getMessage());
        }
    }

    /**
     * 시간대별 블록 장소 수집 (새 가이드 방식)
     * 
     * @param destination 목적지
     * @param timeBlock 시간대 블록 (BREAKFAST, MORNING_ACTIVITY 등)
     * @param day 여행 일차
     * @param travelStyle 여행 스타일
     * @param targetCount 목표 개수
     * @return 수집된 장소 리스트
     */
    private List<PlaceDeduplicator.TourPlace> collectTimeBlockPlaces(String destination, String timeBlock, int day, String travelStyle, int targetCount) {
        log.info("Day {} {} 블록 수집 시작 (목표: {}개)", day, timeBlock, targetCount);
        
        try {
            // 시간대별 검색 쿼리 생성
            String query = buildTimeBlockQuery(destination, timeBlock, day, travelStyle);
            var results = perplexitySearch.apply(
                new SearchWithPerplexityFunction.SearchQuery(query, destination, timeBlock)
            );
            
            List<PlaceDeduplicator.TourPlace> blockPlaces = results.stream()
                .map(result -> convertFromPerplexityWithTimeBlock(result, travelStyle, timeBlock, day))
                .collect(Collectors.toList());
                
            log.info("Day {} {} 블록 수집 완료: {}개", day, timeBlock, blockPlaces.size());
            return blockPlaces;
            
        } catch (Exception e) {
            log.error("Day {} {} 블록 수집 실패", day, timeBlock, e);
            return Collections.emptyList();
        }
    }

    /**
     * 시간대별 검색 쿼리 생성 (새 가이드 방식)
     */
    private String buildTimeBlockQuery(String destination, String timeBlock, int day, String travelStyle) {
        String baseQuery = destination + " Day" + day + " " + travelStyle;
        
        return switch (timeBlock) {
            case "BREAKFAST" -> baseQuery + " 아침식사 브런치 맛집 추천 10곳";
            case "MORNING_ACTIVITY" -> baseQuery + " 오전 관광지 명소 체험 추천 15곳";
            case "LUNCH" -> baseQuery + " 점심 맛집 현지인 추천 10곳";
            case "CAFE" -> baseQuery + " 오후 인스타그램 카페 디저트 10곳";
            case "AFTERNOON_ACTIVITY" -> baseQuery + " 오후 쇼핑 관광 명소 15곳";
            case "DINNER" -> baseQuery + " 저녁 맛집 분위기 좋은 10곳";
            case "EVENING_ACTIVITY" -> baseQuery + " 저녁 야경 바 문화공간 10곳";
            default -> baseQuery + " 추천 장소";
        };
    }

    /**
     * Perplexity 결과를 시간블록 정보와 함께 변환
     */
    private PlaceDeduplicator.TourPlace convertFromPerplexityWithTimeBlock(
            SearchWithPerplexityFunction.SearchResult result, String travelStyle, String timeBlock, int day) {
        
        String category = getTimeBlockCategory(timeBlock);
        String recommendTime = getRecommendTime(timeBlock);
        
        return new PlaceDeduplicator.TourPlace(
            generateId(result.name(), result.address()),
            result.name(),
            result.address(),
            null, null, // 좌표 정보 없음 (순차적 보완 예정)
            category,
            result.rating(),
            result.description(),
            result.hours(),
            null, // 가격 정보 없음 (순차적 보완 예정)
            Arrays.asList("Perplexity", category, timeBlock),
            "Perplexity",
            travelStyle,
            timeBlock,      // 시간대 블록
            day,            // 여행 일차
            recommendTime   // 추천 방문 시간
        );
    }

    /**
     * 시간대별 카테고리 반환 (새 가이드 방식)
     */
    private String getTimeBlockCategory(String timeBlock) {
        return switch (timeBlock) {
            case "BREAKFAST", "LUNCH", "DINNER" -> "맛집";
            case "CAFE" -> "카페";
            case "MORNING_ACTIVITY", "AFTERNOON_ACTIVITY" -> "관광지";
            case "EVENING_ACTIVITY" -> "문화시설";
            default -> "기타";
        };
    }

    /**
     * 순차적 정보 보충 (새 가이드 방식)
     * 1단계: Tour API → 2단계: Kakao Map → 3단계: Google Places
     */
    private List<PlaceDeduplicator.TourPlace> enhancePlacesSequentially(List<PlaceDeduplicator.TourPlace> places, String destination) {
        log.info("순차적 정보 보충 시작: {}개 장소", places.size());
        
        List<PlaceDeduplicator.TourPlace> enhancedPlaces = new ArrayList<>();
        
        for (PlaceDeduplicator.TourPlace place : places) {
            try {
                // 1단계: Tour API로 상세 정보 보충
                PlaceDeduplicator.TourPlace step1 = enhanceWithTourAPI(place, destination);
                
                // 2단계: Kakao Map으로 주소 및 좌표 보완 (구현 예정)
                PlaceDeduplicator.TourPlace step2 = enhanceWithKakaoMap(step1, destination);
                
                // 3단계: Google Places로 사진 및 가격 정보 추가 (구현 예정)
                PlaceDeduplicator.TourPlace step3 = enhanceWithGooglePlaces(step2);
                
                // 4단계: 필수 정보 검증
                if (isValidPlace(step3)) {
                    enhancedPlaces.add(step3);
                } else {
                    log.debug("필수 정보 부족으로 제외: {}", step3.name());
                }
                
            } catch (Exception e) {
                log.error("장소 정보 보충 실패: {}", place.name(), e);
                // 실패해도 원본 데이터라도 포함
                if (isValidPlace(place)) {
                    enhancedPlaces.add(place);
                }
            }
        }
        
        log.info("순차적 정보 보충 완료: {}개 → {}개", places.size(), enhancedPlaces.size());
        return enhancedPlaces;
    }

    /**
     * Tour API로 상세 정보 보충
     */
    private PlaceDeduplicator.TourPlace enhanceWithTourAPI(PlaceDeduplicator.TourPlace place, String destination) {
        try {
            var tourResults = tourAPISearch.apply(new SearchTourAPIFunction.Location(destination, getAreaCodeForDestination(destination))); // 위치 기반 검색
            if (tourResults.isEmpty()) {
                return place; // 정보 없으면 원본 반환
            }
            
            var tourPlace = tourResults.get(0); // 첫 번째 결과 사용
            
            // 기존 정보 우선, 부족한 정보만 보완
            return new PlaceDeduplicator.TourPlace(
                place.id(),
                place.name(),
                place.address() != null ? place.address() : tourPlace.address(),
                place.latitude(), place.longitude(), // Tour API는 좌표 제공 안함
                place.category(),
                place.rating() != null ? place.rating() : tourPlace.rating(),
                place.description() != null ? place.description() : tourPlace.description(),
                place.operatingHours(), // Perplexity가 더 정확
                place.priceRange(),
                mergeTags(place.tags(), List.of("TourAPI")),
                mergeSource(place.source(), "TourAPI"),
                place.travelStyle(),
                place.timeBlock(),
                place.day(),
                place.recommendTime()
            );
            
        } catch (Exception e) {
            log.debug("Tour API 보충 실패: {}", place.name(), e);
            return place;
        }
    }

    /**
     * Kakao Map으로 주소 및 좌표 보완 (구현 예정)
     */
    private PlaceDeduplicator.TourPlace enhanceWithKakaoMap(PlaceDeduplicator.TourPlace place, String destination) {
        // TODO: Kakao Map API 구현 후 좌표 정보 보완
        return place;
    }

    /**
     * Google Places로 사진 및 가격 정보 추가 (구현 예정)
     */
    private PlaceDeduplicator.TourPlace enhanceWithGooglePlaces(PlaceDeduplicator.TourPlace place) {
        // TODO: Google Places API 구현 후 사진/가격 정보 보완
        return place;
    }

    /**
     * 필수 정보 검증 (새 가이드 방식)
     */
    private boolean isValidPlace(PlaceDeduplicator.TourPlace place) {
        return place.name() != null && !place.name().trim().isEmpty() &&
               place.timeBlock() != null &&
               place.day() != null &&
               place.category() != null;
        // 주소, 좌표는 선택사항 (Kakao/Google에서 보완 예정)
    }

    /**
     * 태그 병합
     */
    private List<String> mergeTags(List<String> originalTags, List<String> newTags) {
        List<String> merged = new ArrayList<>(originalTags);
        for (String newTag : newTags) {
            if (!merged.contains(newTag)) {
                merged.add(newTag);
            }
        }
        return merged;
    }

    /**
     * 출처 병합
     */
    private String mergeSource(String originalSource, String newSource) {
        if (originalSource.contains(newSource)) {
            return originalSource;
        }
        return originalSource + ", " + newSource;
    }

    /**
     * 카테고리별 장소 수집
     */
    private Map<String, List<PlaceDeduplicator.TourPlace>> collectPlacesByCategory(String destination, TravelInfo travelInfo) {
        Map<String, List<PlaceDeduplicator.TourPlace>> categoryPlaces = new HashMap<>();
        
        // 각 카테고리별로 병렬 검색
        List<CompletableFuture<Void>> futures = CATEGORY_KEYWORDS.entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(() -> {
                String category = entry.getKey();
                List<String> keywords = entry.getValue();
                
                List<PlaceDeduplicator.TourPlace> places = searchPlacesForCategory(destination, category, keywords, travelInfo);
                synchronized (categoryPlaces) {
                    categoryPlaces.put(category, places);
                }
            }))
            .collect(Collectors.toList());
        
        // 모든 검색 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return categoryPlaces;
    }

    /**
     * 특정 카테고리의 장소 검색
     */
    private List<PlaceDeduplicator.TourPlace> searchPlacesForCategory(String destination, String category, List<String> keywords, TravelInfo travelInfo) {
        List<PlaceDeduplicator.TourPlace> allPlaces = new ArrayList<>();
        
        try {
            // Perplexity 검색 (각 키워드별)
            for (String keyword : keywords) {
                var query = new SearchWithPerplexityFunction.SearchQuery(destination, keyword, category);
                List<SearchWithPerplexityFunction.SearchResult> perplexityResults = perplexitySearch.apply(query);
                
                List<PlaceDeduplicator.TourPlace> places = perplexityResults.stream()
                    .map(result -> convertFromPerplexity(result, category, travelInfo.travelStyle()))
                    .collect(Collectors.toList());
                
                allPlaces.addAll(places);
            }
            
            // 관광공사 API 검색 (카테고리별)
            if (shouldSearchTourAPI(category)) {
                var location = new SearchTourAPIFunction.Location(destination, getAreaCodeForDestination(destination));
                List<SearchTourAPIFunction.TourPlace> tourResults = tourAPISearch.apply(location);
                
                List<PlaceDeduplicator.TourPlace> tourPlaces = tourResults.stream()
                    .filter(place -> matchesCategory(place, category))
                    .map(place -> convertFromTourAPI(place, category, travelInfo.travelStyle()))
                    .collect(Collectors.toList());
                
                allPlaces.addAll(tourPlaces);
            }
            
            // 중복 제거
            List<PlaceDeduplicator.TourPlace> uniquePlaces = placeDeduplicator.deduplicate(allPlaces);
            
            log.debug("카테고리 '{}' 검색 완료: {} 개 (중복제거 전: {} 개)", category, uniquePlaces.size(), allPlaces.size());
            
            return uniquePlaces;
            
        } catch (Exception e) {
            log.error("카테고리 '{}' 검색 실패", category, e);
            return new ArrayList<>();
        }
    }

    /**
     * 여행 스타일별 우선순위 적용
     */
    private Map<String, List<PlaceDeduplicator.TourPlace>> applyTravelStylePriority(
            Map<String, List<PlaceDeduplicator.TourPlace>> categoryPlaces, String travelStyle) {
        
        List<String> priorities = TRAVEL_STYLE_PRIORITIES.getOrDefault(travelStyle, 
            Arrays.asList("관광지", "맛집", "쇼핑", "문화시설", "자연", "액티비티"));
        
        Map<String, List<PlaceDeduplicator.TourPlace>> prioritizedPlaces = new LinkedHashMap<>();
        
        // 우선순위에 따라 정렬
        for (String category : priorities) {
            if (categoryPlaces.containsKey(category)) {
                List<PlaceDeduplicator.TourPlace> places = categoryPlaces.get(category);
                // 평점 순으로 정렬
                places.sort((a, b) -> Double.compare(
                    b.rating() != null ? b.rating() : 0.0,
                    a.rating() != null ? a.rating() : 0.0
                ));
                prioritizedPlaces.put(category, places);
            }
        }
        
        return prioritizedPlaces;
    }

    /**
     * 카테고리별 상위 N개 선별
     */
    private Map<String, List<PlaceDeduplicator.TourPlace>> selectTopPlacesByCategory(
            Map<String, List<PlaceDeduplicator.TourPlace>> categoryPlaces, int maxPerCategory) {
        
        Map<String, List<PlaceDeduplicator.TourPlace>> selectedPlaces = new HashMap<>();
        
        categoryPlaces.forEach((category, places) -> {
            List<PlaceDeduplicator.TourPlace> topPlaces = places.stream()
                .limit(maxPerCategory)
                .collect(Collectors.toList());
            selectedPlaces.put(category, topPlaces);
        });
        
        return selectedPlaces;
    }

    /**
     * 통계 정보 생성
     */
    private PlaceStatistics generateStatistics(Map<String, List<PlaceDeduplicator.TourPlace>> categoryPlaces, 
                                             List<PlaceDeduplicator.TourPlace> allPlaces) {
        
        Map<String, Long> categoryDistribution = categoryPlaces.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (long) entry.getValue().size()
            ));
        
        double averageRating = allPlaces.stream()
            .filter(place -> place.rating() != null)
            .mapToDouble(PlaceDeduplicator.TourPlace::rating)
            .average()
            .orElse(0.0);
        
        return new PlaceStatistics(allPlaces.size(), categoryDistribution, averageRating);
    }

    /**
     * 경고 메시지 생성
     */
    private List<String> generateWarnings(Map<String, List<PlaceDeduplicator.TourPlace>> categoryPlaces, TravelInfo travelInfo) {
        List<String> warnings = new ArrayList<>();
        
        // 카테고리별 장소 수 체크
        categoryPlaces.forEach((category, places) -> {
            if (places.size() < 10) {
                warnings.add(String.format("'%s' 카테고리의 장소가 부족합니다 (%d개)", category, places.size()));
            }
        });
        
        // 전체 장소 수 체크
        int totalPlaces = categoryPlaces.values().stream().mapToInt(List::size).sum();
        int recommendedMin = travelInfo.tripDays() * 20; // 일당 20개 권장
        
        if (totalPlaces < recommendedMin) {
            warnings.add(String.format("전체 장소 수가 권장량보다 적습니다 (현재: %d개, 권장: %d개)", totalPlaces, recommendedMin));
        }
        
        return warnings;
    }

    // ========== 헬퍼 메서드 ==========

    private PlaceDeduplicator.TourPlace convertFromPerplexity(SearchWithPerplexityFunction.SearchResult result, String travelStyle, String blockName) {
        // 블록명에 따른 카테고리 자동 분류
        String category = determineCategory(blockName, result.name(), result.description());
        String timeBlock = convertBlockNameToTimeBlock(blockName);
        String recommendTime = getRecommendTime(timeBlock);
        
        return new PlaceDeduplicator.TourPlace(
            generateId(result.name(), result.address()),
            result.name(),
            result.address(),
            null, null, // 좌표 정보 없음 (Tour API로 보완 예정)
            category,
            result.rating(),
            result.description(),
            result.hours(),
            null, // 가격 정보 없음 (Tour API로 보완 예정)
            Arrays.asList("Perplexity", category, blockName),
            "Perplexity",
            travelStyle,
            timeBlock,      // 시간대 블록
            1,              // 기본 1일차 (나중에 동적으로 설정)
            recommendTime   // 추천 방문 시간
        );
    }

    /**
     * 블록명을 시간대 블록으로 변환
     */
    private String convertBlockNameToTimeBlock(String blockName) {
        return switch (blockName) {
            case "아침식사" -> "BREAKFAST";
            case "아침일과" -> "MORNING_ACTIVITY";
            case "점심식사" -> "LUNCH";
            case "카페" -> "CAFE";
            case "점심일과" -> "AFTERNOON_ACTIVITY";
            case "저녁식사" -> "DINNER";
            case "저녁일과" -> "EVENING_ACTIVITY";
            default -> "MORNING_ACTIVITY";
        };
    }

    /**
     * 시간대별 추천 시간 반환
     */
    private String getRecommendTime(String timeBlock) {
        return switch (timeBlock) {
            case "BREAKFAST" -> "08:00-10:00";
            case "MORNING_ACTIVITY" -> "10:00-12:00";
            case "LUNCH" -> "12:00-14:00";
            case "CAFE" -> "14:00-15:30";
            case "AFTERNOON_ACTIVITY" -> "15:30-17:00";
            case "DINNER" -> "17:00-19:00";
            case "EVENING_ACTIVITY" -> "19:00-21:00";
            default -> "";
        };
    }

    /**
     * 블록명에 따른 카테고리 자동 분류
     */
    private String determineCategory(String blockName, String placeName, String description) {
        // 블록명 기반 카테고리 매핑
        switch (blockName) {
            case "아침식사":
            case "점심식사": 
            case "저녁식사":
                return "맛집";
            case "카페":
                return "카페";
            case "아침일과":
            case "점심일과":
                // 장소명/설명으로 세부 분류
                String lowerName = (placeName + " " + description).toLowerCase();
                if (lowerName.contains("박물관") || lowerName.contains("미술관") || lowerName.contains("갤러리")) {
                    return "문화시설";
                } else if (lowerName.contains("쇼핑") || lowerName.contains("시장") || lowerName.contains("백화점")) {
                    return "쇼핑";
                } else if (lowerName.contains("공원") || lowerName.contains("산") || lowerName.contains("강")) {
                    return "자연";
                } else {
                    return "관광지";
                }
            case "저녁일과":
                return "액티비티";
            default:
                return "관광지";
        }
    }

    private PlaceDeduplicator.TourPlace convertFromTourAPI(SearchTourAPIFunction.TourPlace tourPlace, String category, String travelStyle) {
        return new PlaceDeduplicator.TourPlace(
            generateId(tourPlace.name(), tourPlace.address()),
            tourPlace.name(),
            tourPlace.address(),
            null, null, // 좌표 변환 필요시 추가
            category,
            tourPlace.rating(),
            tourPlace.description(),
            null, // 운영시간 정보 없음
            null, // 가격 정보 없음
            Arrays.asList("관광공사", category),
            "TourAPI",
            travelStyle, // 여행 스타일 추가
            null,  // 시간블록 없음 (Perplexity에서만 설정)
            null,  // 일차 없음
            null   // 추천시간 없음
        );
    }

    private String generateId(String name, String address) {
        String base = (name != null ? name : "") + (address != null ? address : "");
        return "place_" + Math.abs(base.hashCode());
    }

    private boolean shouldSearchTourAPI(String category) {
        return Arrays.asList("관광지", "문화시설", "자연").contains(category);
    }

    private boolean matchesCategory(SearchTourAPIFunction.TourPlace place, String category) {
        // 관광공사 API 결과를 카테고리별로 필터링하는 로직
        return true; // 임시로 모든 결과 허용
    }

    private String getAreaCodeForDestination(String destination) {
        return switch (destination.toLowerCase()) {
            case "서울", "seoul" -> "1";
            case "인천", "incheon" -> "2";
            case "대전", "daejeon" -> "3";
            case "대구", "daegu" -> "4";
            case "광주", "gwangju" -> "5";
            case "부산", "busan" -> "6";
            case "울산", "ulsan" -> "7";
            case "세종", "sejong" -> "8";
            case "경기", "gyeonggi" -> "31";
            case "강원", "gangwon" -> "32";
            case "충북", "chungbuk" -> "33";
            case "충남", "chungnam" -> "34";
            case "경북", "gyeongbuk" -> "35";
            case "경남", "gyeongnam" -> "36";
            case "전북", "jeonbuk" -> "37";
            case "전남", "jeonnam" -> "38";
            case "제주", "jeju" -> "39";
            default -> "1"; // 기본값: 서울
        };
    }

    private Stage1Output createEmptyOutput(int tripDays, String errorMessage) {
        return new Stage1Output(
            new ArrayList<>(),
            new HashMap<>(),
            new PlaceStatistics(0, new HashMap<>(), 0.0),
            tripDays,
            List.of("오류 발생: " + errorMessage)
        );
    }

    // ========== 데이터 클래스 ==========

    /**
     * 여행 정보
     */
    public record TravelInfo(
        String travelStyle,    // CULTURAL, FOODIE, NATURE, SHOPPING, ADVENTURE, RELAXATION
        String budget,         // LOW, MEDIUM, HIGH, UNLIMITED
        List<String> interests, // 관심사 키워드
        LocalDate startDate,
        LocalDate endDate,
        int tripDays,
        int companions
    ) {}

    /**
     * Stage 1 출력 결과
     */
    public record Stage1Output(
        List<PlaceDeduplicator.TourPlace> places,                    // 전체 장소 리스트 (Stage 2,3 전달용)
        Map<String, List<PlaceDeduplicator.TourPlace>> categoryPlaces, // 카테고리별 장소 (최대 100개씩)
        PlaceStatistics statistics,                                   // 통계 정보
        int tripDays,                                                // 여행 일수
        List<String> warnings                                        // 경고 메시지
    ) {}

    /**
     * 장소 통계 정보
     */
    public record PlaceStatistics(
        int totalCount,
        Map<String, Long> categoryDistribution,
        double averageRating
    ) {}
}