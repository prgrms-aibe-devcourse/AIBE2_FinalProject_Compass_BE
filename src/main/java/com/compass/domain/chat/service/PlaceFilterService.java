package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TourPlace;
import com.compass.domain.chat.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 장소 필터링 서비스
 * 
 * 사용자 선호도에 따른 장소 필터링을 담당합니다.
 * - 예산 범위 필터링
 * - 여행 스타일 매칭
 * - 선호 카테고리 필터링
 * - 운영 시간 및 휴무일 체크
 * - 점수 기반 정렬
 */
@Service
public class PlaceFilterService {

    private static final Logger log = LoggerFactory.getLogger(PlaceFilterService.class);

    private final ClusterService clusterService;

    public PlaceFilterService(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    // 예산 범위별 가격대 매핑
    private static final Map<Budget, Set<String>> BUDGET_PRICE_MAPPING = Map.of(
        Budget.LOW, Set.of("FREE", "$"),
        Budget.MEDIUM, Set.of("FREE", "$", "$$"),
        Budget.HIGH, Set.of("FREE", "$", "$$", "$$$"),
        Budget.UNLIMITED, Set.of("FREE", "$", "$$", "$$$", "$$$$")
    );

    // 여행 스타일별 선호 카테고리 매핑
    private static final Map<TravelStyle, Set<String>> STYLE_CATEGORY_MAPPING = Map.of(
        TravelStyle.RELAXATION, Set.of("카페", "공원", "스파", "해변", "온천", "휴양지"),
        TravelStyle.ADVENTURE, Set.of("액티비티", "등산", "체험", "스포츠", "모험", "아웃도어"),
        TravelStyle.CULTURAL, Set.of("박물관", "미술관", "전통", "역사", "문화재", "궁궐", "사찰"),
        TravelStyle.FOODIE, Set.of("맛집", "시장", "카페", "베이커리", "전통음식", "로컬푸드"),
        TravelStyle.SHOPPING, Set.of("쇼핑", "백화점", "아울렛", "시장", "면세점", "브랜드"),
        TravelStyle.NATURE, Set.of("산", "바다", "공원", "자연", "숲", "계곡", "폭포", "해변")
    );

    /**
     * 사용자 선호도 기반 필터링 (기존 기능 유지)
     * 
     * @param places 원본 장소 리스트
     * @param preferences 사용자 선호도
     * @return 필터링된 장소 리스트
     */
    public List<TourPlace> filterByPreferences(
        List<TourPlace> places,
        UserPreferences preferences
    ) {
        if (places == null || places.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("장소 필터링 시작: {} 개 장소, 스타일: {}, 예산: {}", 
                places.size(), preferences.travelStyle(), preferences.budget());

        List<TourPlace> filtered = places.stream()
            // 1. 예산 범위 필터링
            .filter(place -> matchesBudget(place, preferences.budget()))
            // 2. 여행 스타일 매칭
            .filter(place -> matchesStyle(place, preferences.travelStyle()))
            // 3. 선호 카테고리 필터링
            .filter(place -> matchesCategories(place, preferences.preferredCategories()))
            // 4. 운영시간 확인
            .filter(place -> isOperatingDuringTrip(place, preferences.travelDates()))
            // 5. 점수 기반 정렬
            .sorted(this::compareByScore)
            // 6. 여행 일수에 따른 개수 제한
            .limit(calculateMaxPlaces(preferences.tripDays()))
            .collect(Collectors.toList());

        log.info("장소 필터링 완료: {} -> {} 개 장소 ({}% 필터링)", 
                places.size(), filtered.size(), 
                Math.round((1.0 - (double)filtered.size() / places.size()) * 100));

        return filtered;
    }

    /**
     * Stage 1: 스타일 기반 장소 선별 (클러스터링 추가)
     * 기존 필터링을 유지하면서 클러스터링 기능을 추가
     * 
     * @param places 원본 장소 리스트
     * @param preferences 사용자 선호도
     * @return 클러스터링이 적용된 필터링된 장소 리스트
     */
    public List<TourPlace> filterByPreferencesWithClustering(
        List<TourPlace> places,
        UserPreferences preferences
    ) {
        if (places == null || places.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("Stage 1: 스타일 기반 장소 선별 시작 - {} 개 장소", places.size());

        // 1. 기본 필터링 적용 (기존 기능 유지)
        List<TourPlace> basicFiltered = filterByPreferences(places, preferences);
        
        // 2. 클러스터링 적용
        List<TourPlace> clusteredPlaces = applyClustering(basicFiltered, preferences);
        
        log.info("Stage 1 완료: {} -> {} 개 장소 (클러스터링 적용)", 
                basicFiltered.size(), clusteredPlaces.size());

        return clusteredPlaces;
    }

    /**
     * 클러스터링 적용 (Stage 1 핵심 로직)
     * 개선안의 스타일 기반 매칭 알고리즘 구현
     */
    private List<TourPlace> applyClustering(List<TourPlace> places, UserPreferences preferences) {
        // 1. 여행 스타일을 클러스터 매칭 형식으로 변환
        Map<String, Object> travelStyle = convertToClusterFormat(preferences);
        
        // 2. 클러스터 매칭 점수 계산
        Map<String, Double> clusterScores = clusterService.calculateClusterScores(travelStyle);
        
        // 3. 클러스터별 장소 수집 비율 계산
        int totalPlaces = places.size();
        Map<String, Integer> placeDistribution = clusterService.calculatePlaceDistribution(clusterScores, totalPlaces);
        
        // 4. 클러스터별로 장소 선별
        List<TourPlace> selectedPlaces = selectPlacesByCluster(places, placeDistribution, clusterScores);
        
        return selectedPlaces;
    }

    /**
     * UserPreferences를 클러스터 매칭 형식으로 변환
     */
    private Map<String, Object> convertToClusterFormat(UserPreferences preferences) {
        Map<String, Object> travelStyle = new HashMap<>();
        
        // 스타일 변환
        if (preferences.travelStyle() != null) {
            List<String> styles = convertTravelStyleToClusterStyles(preferences.travelStyle());
            travelStyle.put("styles", styles);
        }
        
        // 연령대 추정 (기본값)
        travelStyle.put("ageGroup", "20-30대"); // 기본값, 추후 개선 가능
        
        // 예산 변환
        if (preferences.budget() != null) {
            String budget = convertBudgetToClusterBudget(preferences.budget());
            travelStyle.put("budget", budget);
        }
        
        return travelStyle;
    }

    /**
     * TravelStyle을 클러스터 스타일로 변환
     */
    private List<String> convertTravelStyleToClusterStyles(TravelStyle style) {
        return switch (style) {
            case RELAXATION -> Arrays.asList("힐링", "휴양");
            case ADVENTURE -> Arrays.asList("활동적", "모험");
            case CULTURAL -> Arrays.asList("문화", "예술");
            case FOODIE -> Arrays.asList("맛집", "음식");
            case SHOPPING -> Arrays.asList("쇼핑", "럭셔리");
            case NATURE -> Arrays.asList("자연", "힐링");
        };
    }

    /**
     * Budget을 클러스터 예산으로 변환
     */
    private String convertBudgetToClusterBudget(Budget budget) {
        return switch (budget) {
            case LOW -> "낮음";
            case MEDIUM -> "중간";
            case HIGH -> "높음";
            case UNLIMITED -> "높음";
        };
    }

    /**
     * 클러스터별로 장소 선별
     * 개선안의 클러스터 분배 알고리즘 구현
     */
    private List<TourPlace> selectPlacesByCluster(
        List<TourPlace> places, 
        Map<String, Integer> placeDistribution, 
        Map<String, Double> clusterScores
    ) {
        List<TourPlace> selectedPlaces = new ArrayList<>();
        
        // 클러스터별로 장소 수집
        for (Map.Entry<String, Integer> entry : placeDistribution.entrySet()) {
            String clusterName = entry.getKey();
            Integer placeCount = entry.getValue();
            Double score = clusterScores.get(clusterName);
            
            if (placeCount > 0 && score > 0.1) {
                Cluster cluster = clusterService.getCluster(clusterName);
                List<TourPlace> clusterPlaces = selectPlacesFromCluster(places, cluster, placeCount);
                selectedPlaces.addAll(clusterPlaces);
                
                log.debug("클러스터 {}: {}개 장소 선별 (매칭점수: {})", 
                         clusterName, clusterPlaces.size(), score);
            }
        }
        
        // 클러스터에서 선별되지 않은 장소들을 나머지로 추가
        List<TourPlace> remainingPlaces = places.stream()
            .filter(place -> !selectedPlaces.contains(place))
            .limit(places.size() - selectedPlaces.size())
            .collect(Collectors.toList());
        
        selectedPlaces.addAll(remainingPlaces);
        
        return selectedPlaces;
    }

    /**
     * 특정 클러스터에서 장소 선별
     * 지리적 근접성 + 동적 점수 시스템 기반으로 장소 선별
     */
    private List<TourPlace> selectPlacesFromCluster(
        List<TourPlace> places, 
        Cluster cluster, 
        int placeCount
    ) {
        if (cluster == null || placeCount <= 0) {
            return new ArrayList<>();
        }
        
        // 클러스터 내 장소들을 동적 점수로 정렬
        return places.stream()
            .filter(place -> isWithinClusterRadius(place, cluster))
            .sorted((p1, p2) -> {
                // 동적 점수 계산 (클러스터 정보 포함)
                double score1 = calculateScore(p1, p1.getTimeBlock(), cluster.getName(), places);
                double score2 = calculateScore(p2, p2.getTimeBlock(), cluster.getName(), places);
                return Double.compare(score2, score1); // 내림차순 (높은 점수 우선)
            })
            .limit(placeCount)
            .collect(Collectors.toList());
    }

    /**
     * 장소가 클러스터 반경 내에 있는지 확인
     */
    private boolean isWithinClusterRadius(TourPlace place, Cluster cluster) {
        if (place.getLatitude() == null || place.getLongitude() == null) {
            return false;
        }
        
        double distance = calculateDistance(
            place, 
            cluster.getCenterLat(), 
            cluster.getCenterLng()
        );
        
        return distance <= (cluster.getRadius() / 1000.0); // 미터를 킬로미터로 변환
    }

    /**
     * 두 지점 간의 거리 계산 (Haversine 공식)
     */
    private double calculateDistance(TourPlace place, Double centerLat, Double centerLng) {
        if (place.getLatitude() == null || place.getLongitude() == null || 
            centerLat == null || centerLng == null) {
            return Double.MAX_VALUE;
        }
        
        final int R = 6371; // 지구 반지름 (km)
        
        double latDistance = Math.toRadians(centerLat - place.getLatitude());
        double lngDistance = Math.toRadians(centerLng - place.getLongitude());
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(place.getLatitude())) 
                * Math.cos(Math.toRadians(centerLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    /**
     * 예산 범위 체크
     */
    private boolean matchesBudget(TourPlace place, Budget budget) {
        if (budget == null || place.getPriceLevel() == null) {
            return true; // 정보가 없으면 통과
        }

        Set<String> allowedPrices = BUDGET_PRICE_MAPPING.get(budget);
        return allowedPrices.contains(place.getPriceLevel());
    }

    /**
     * 여행 스타일 매칭
     */
    private boolean matchesStyle(TourPlace place, TravelStyle style) {
        if (style == null || place.getCategory() == null) {
            return true; // 정보가 없으면 통과
        }

        Set<String> preferredCategories = STYLE_CATEGORY_MAPPING.get(style);
        
        // 카테고리가 선호 카테고리와 매칭되는지 확인
        return preferredCategories.stream()
                .anyMatch(preferred -> place.getCategory().toLowerCase().contains(preferred.toLowerCase()));
    }

    /**
     * 선호 카테고리 필터링
     */
    private boolean matchesCategories(TourPlace place, List<String> preferredCategories) {
        if (preferredCategories == null || preferredCategories.isEmpty() || place.getCategory() == null) {
            return true; // 선호도가 없거나 카테고리 정보가 없으면 통과
        }

        String category = place.getCategory().toLowerCase();
        return preferredCategories.stream()
                .anyMatch(preferred -> category.contains(preferred.toLowerCase()));
    }

    /**
     * 운영 시간 확인
     */
    private boolean isOperatingDuringTrip(TourPlace place, DateRange dates) {
        if (dates == null || place.getOperatingHours() == null) {
            return true; // 정보가 없으면 통과
        }

        // 간단한 운영시간 체크 (24시간 운영이거나 일반적인 운영시간)
        String hours = place.getOperatingHours().toLowerCase();
        
        // 24시간 운영
        if (hours.contains("24시간") || hours.contains("00:00-24:00")) {
            return true;
        }
        
        // 휴무일 체크
        if (hours.contains("휴무") || hours.contains("closed")) {
            // 특정 요일 휴무인 경우 여행 날짜와 겹치는지 확인
            // 간단히 월요일 휴무만 체크
            if (hours.contains("월요일") || hours.contains("monday")) {
                return !dates.containsMonday(); // 월요일이 포함되지 않으면 OK
            }
        }
        
        return true; // 기본적으로 운영한다고 가정
    }

    /**
     * 장소 점수 계산 (정렬용)
     */
    private int compareByScore(TourPlace p1, TourPlace p2) {
        double score1 = calculateScore(p1);
        double score2 = calculateScore(p2);
        return Double.compare(score2, score1);  // 내림차순 (높은 점수 우선)
    }

    /**
     * 장소 점수 계산 (기본 버전 - 하위 호환성)
     */
    private double calculateScore(TourPlace place) {
        return calculateScore(place, null, null, null);
    }

    /**
     * 장소 점수 계산 (동적 점수 시스템 사용)
     */
    private double calculateScore(TourPlace place, String timeBlock, String clusterName, List<TourPlace> allPlaces) {
        double score = 0.0;

        // 평점 (40% 가중치)
        if (place.getRating() != null) {
            score += place.getRating() * 0.4;
        } else {
            score += 3.5 * 0.4; // 기본 평점
        }

        // 정보 완성도 (30% 가중치)
        double completeness = calculateCompleteness(place);
        score += completeness * 0.3;

        // 동적 카테고리 인기도 (20% 가중치)
        double categoryScore = getCategoryPopularity(place.getCategory(), timeBlock, clusterName, allPlaces);
        score += categoryScore * 0.2;

        // 태그 다양성 (10% 가중치) - TourPlace에는 tags 필드가 없으므로 기본값
        double tagScore = 0.5; // 기본값
        score += tagScore * 0.1;

        return score;
    }

    /**
     * 정보 완성도 계산
     */
    private double calculateCompleteness(TourPlace place) {
        int totalFields = 7; // 주요 필드 개수 (tags 제외)
        int filledFields = 0;

        if (place.getName() != null && !place.getName().trim().isEmpty()) filledFields++;
        if (place.getAddress() != null && !place.getAddress().trim().isEmpty()) filledFields++;
        if (place.getLatitude() != null && place.getLongitude() != null) filledFields++;
        if (place.getCategory() != null && !place.getCategory().trim().isEmpty()) filledFields++;
        if (place.getRating() != null) filledFields++;
        if (place.getOverview() != null && !place.getOverview().trim().isEmpty()) filledFields++;
        if (place.getOperatingHours() != null && !place.getOperatingHours().trim().isEmpty()) filledFields++;

        return (double) filledFields / totalFields;
    }

    /**
     * 동적 카테고리 인기도 점수 계산 (실제 데이터 기반)
     */
    private double getCategoryPopularity(String category, String timeBlock, String clusterName, List<TourPlace> allPlaces) {
        if (category == null) return 0.5;

        String cat = category.toLowerCase();
        
        // 1. 기본 카테고리 점수 (하드코딩 대신 동적 계산)
        double baseScore = calculateBaseCategoryScore(cat);
        
        // 2. 시간대별 인기도 보정
        double timeBlockMultiplier = getTimeBlockMultiplier(timeBlock, cat);
        
        // 3. 클러스터별 선호도 보정
        double clusterMultiplier = getClusterMultiplier(clusterName, cat);
        
        // 4. 실제 데이터 기반 보정 (평점, 리뷰 수 등)
        double dataBasedScore = getDataBasedScore(category, allPlaces);
        
        // 5. 최종 점수 계산 (가중 평균)
        double finalScore = (baseScore * 0.3) + 
                           (baseScore * timeBlockMultiplier * 0.3) + 
                           (baseScore * clusterMultiplier * 0.2) + 
                           (dataBasedScore * 0.2);
        
        // 6. 0.1 ~ 1.0 범위로 정규화
        return Math.max(0.1, Math.min(1.0, finalScore));
    }

    /**
     * 기본 카테고리 점수 계산 (하드코딩 대신 동적)
     */
    private double calculateBaseCategoryScore(String category) {
        // 카테고리별 기본 점수를 더 세분화하고 동적으로 계산
        return switch (category) {
            case "관광지", "명소" -> 0.85;  // 관광지는 기본적으로 높지만 절대적이지 않음
            case "맛집", "음식점" -> 0.80;  // 맛집도 인기 있지만 시간대에 따라 다름
            case "카페", "디저트" -> 0.75;  // 카페는 시간대별로 큰 차이
            case "문화시설", "박물관" -> 0.70; // 문화시설은 선호도에 따라 차이
            case "쇼핑", "시장" -> 0.65;     // 쇼핑은 개인차가 큼
            case "자연", "공원" -> 0.60;     // 자연은 계절과 날씨에 따라 다름
            case "체험", "액티비티" -> 0.55; // 체험은 특정 그룹에게만 인기
            default -> 0.50; // 기본값
        };
    }

    /**
     * 시간대별 인기도 보정 계수
     */
    private double getTimeBlockMultiplier(String timeBlock, String category) {
        if (timeBlock == null) return 1.0;
        
        return switch (timeBlock) {
            case "BREAKFAST" -> {
                // 아침 시간대 카테고리별 인기도
                if (category.contains("카페") || category.contains("맛집")) yield 1.3; // 아침엔 카페/맛집이 인기
                if (category.contains("관광지")) yield 0.8; // 아침엔 관광지 덜 인기
                yield 1.0;
            }
            case "MORNING_ACTIVITY" -> {
                // 오전 활동 시간대
                if (category.contains("관광지") || category.contains("명소")) yield 1.4; // 오전엔 관광지 인기
                if (category.contains("자연") || category.contains("공원")) yield 1.2; // 자연도 오전에 좋음
                if (category.contains("카페")) yield 0.7; // 오전 활동엔 카페 덜 인기
                yield 1.0;
            }
            case "LUNCH" -> {
                // 점심 시간대
                if (category.contains("맛집") || category.contains("음식")) yield 1.5; // 점심엔 맛집 최고 인기
                if (category.contains("카페")) yield 0.6; // 점심엔 카페 덜 인기
                yield 1.0;
            }
            case "CAFE" -> {
                // 카페 시간대
                if (category.contains("카페") || category.contains("디저트")) yield 1.6; // 카페 시간엔 카페 최고
                if (category.contains("맛집")) yield 0.8; // 카페 시간엔 맛집 덜 인기
                yield 1.0;
            }
            case "AFTERNOON_ACTIVITY" -> {
                // 오후 활동 시간대
                if (category.contains("쇼핑") || category.contains("시장")) yield 1.3; // 오후엔 쇼핑 인기
                if (category.contains("문화시설") || category.contains("박물관")) yield 1.2; // 문화시설도 오후에 좋음
                if (category.contains("관광지")) yield 1.1; // 관광지도 오후에 괜찮음
                yield 1.0;
            }
            case "DINNER" -> {
                // 저녁 시간대
                if (category.contains("맛집") || category.contains("음식")) yield 1.4; // 저녁엔 맛집 인기
                if (category.contains("카페")) yield 0.7; // 저녁엔 카페 덜 인기
                yield 1.0;
            }
            case "EVENING_ACTIVITY" -> {
                // 저녁 활동 시간대
                if (category.contains("문화시설") || category.contains("박물관")) yield 1.3; // 저녁엔 문화시설 인기
                if (category.contains("카페")) yield 1.1; // 저녁엔 카페도 괜찮음
                if (category.contains("자연") || category.contains("공원")) yield 0.8; // 저녁엔 자연 덜 인기
                yield 1.0;
            }
            default -> 1.0;
        };
    }

    /**
     * 클러스터별 선호도 보정 계수
     */
    private double getClusterMultiplier(String clusterName, String category) {
        if (clusterName == null) return 1.0;
        
        // 클러스터별 특성에 따른 카테고리 선호도
        return switch (clusterName.toLowerCase()) {
            case "홍대", "강남", "명동" -> {
                // 도시 중심가: 쇼핑, 맛집, 카페 인기
                if (category.contains("쇼핑") || category.contains("맛집") || category.contains("카페")) yield 1.3;
                if (category.contains("자연") || category.contains("공원")) yield 0.8;
                yield 1.0;
            }
            case "경복궁", "북촌", "인사동" -> {
                // 전통 문화 지역: 관광지, 문화시설 인기
                if (category.contains("관광지") || category.contains("문화시설") || category.contains("박물관")) yield 1.4;
                if (category.contains("쇼핑")) yield 0.9;
                yield 1.0;
            }
            case "한강", "서울숲", "올림픽공원" -> {
                // 자연 지역: 자연, 공원, 카페 인기
                if (category.contains("자연") || category.contains("공원") || category.contains("카페")) yield 1.3;
                if (category.contains("쇼핑")) yield 0.7;
                yield 1.0;
            }
            case "이태원", "청담", "압구정" -> {
                // 고급 상업 지역: 맛집, 카페, 쇼핑 인기
                if (category.contains("맛집") || category.contains("카페") || category.contains("쇼핑")) yield 1.2;
                yield 1.0;
            }
            default -> 1.0; // 기본값
        };
    }

    /**
     * 실제 데이터 기반 점수 (평점, 리뷰 수 등)
     */
    private double getDataBasedScore(String category, List<TourPlace> allPlaces) {
        if (allPlaces == null || allPlaces.isEmpty()) return 0.5;
        
        // 해당 카테고리의 평균 평점과 리뷰 수 계산
        List<TourPlace> categoryPlaces = allPlaces.stream()
            .filter(place -> place.getCategory() != null && 
                   place.getCategory().toLowerCase().contains(category.toLowerCase()))
            .collect(Collectors.toList());
        
        if (categoryPlaces.isEmpty()) return 0.5;
        
        // 평균 평점 계산
        double avgRating = categoryPlaces.stream()
            .filter(place -> place.getRating() != null)
            .mapToDouble(place -> place.getRating())
            .average()
            .orElse(3.0); // 기본 평점 3.0
        
        // 평균 리뷰 수 계산
        double avgReviewCount = categoryPlaces.stream()
            .filter(place -> place.getReviewCount() != null)
            .mapToDouble(place -> place.getReviewCount())
            .average()
            .orElse(10.0); // 기본 리뷰 수 10
        
        // 평점 기반 점수 (3.0 = 0.5, 5.0 = 1.0)
        double ratingScore = (avgRating - 3.0) / 2.0; // 0.0 ~ 1.0 범위
        
        // 리뷰 수 기반 점수 (로그 스케일)
        double reviewScore = Math.min(1.0, Math.log10(avgReviewCount + 1) / 3.0); // 0.0 ~ 1.0 범위
        
        // 가중 평균 (평점 70%, 리뷰 수 30%)
        return (ratingScore * 0.7) + (reviewScore * 0.3);
    }

    /**
     * 여행 일수에 따른 최대 장소 수 계산
     */
    private int calculateMaxPlaces(int tripDays) {
        // 개발자 가이드에 따른 일수별 장소 수
        return switch (tripDays) {
            case 1 -> 8;   // 당일치기: 5-8개
            case 2 -> 15;  // 1박2일: 10-15개
            case 3 -> 20;  // 2박3일: 15-20개
            default -> 30; // 기본값
        };
    }

    /**
     * 시간대별 추천 시간 개선 (더 유연한 시간 설정)
     */
    public String getRecommendTimeForPlace(String timeBlock, String category, String operatingHours) {
        // 운영시간을 고려한 추천 시간 생성
        if (operatingHours != null && !operatingHours.isEmpty()) {
            String optimizedTime = optimizeTimeBasedOnOperatingHours(timeBlock, operatingHours);
            if (optimizedTime != null) {
                return optimizedTime;
            }
        }

        // 카테고리별 세분화된 추천 시간
        return switch (timeBlock) {
            case "BREAKFAST" -> getBreakfastTime(category);
            case "MORNING_ACTIVITY" -> getMorningActivityTime(category);
            case "LUNCH" -> getLunchTime(category);
            case "CAFE" -> getCafeTime(category);
            case "AFTERNOON_ACTIVITY" -> getAfternoonActivityTime(category);
            case "DINNER" -> getDinnerTime(category);
            case "EVENING_ACTIVITY" -> getEveningActivityTime(category);
            default -> "09:00-18:00";
        };
    }

    /**
     * 운영시간을 고려한 최적 시간 계산
     */
    private String optimizeTimeBasedOnOperatingHours(String timeBlock, String operatingHours) {
        try {
            // 운영시간 파싱 (예: "09:00-18:00", "24시간", "08:00-22:00")
            if (operatingHours.contains("24시간") || operatingHours.contains("00:00-24:00")) {
                // 24시간 운영은 기본 시간대 사용
                return null;
            }

            // 시간 추출 (예: "09:00-18:00" -> 09:00, 18:00)
            String[] hours = operatingHours.split("-");
            if (hours.length == 2) {
                String openTime = hours[0].trim();
                String closeTime = hours[1].trim();

                // 시간대별 최적 시간 계산
                return calculateOptimalTimeForBlock(timeBlock, openTime, closeTime);
            }
        } catch (Exception e) {
            log.warn("운영시간 파싱 실패: {}", operatingHours);
        }
        return null;
    }

    /**
     * 시간대별 최적 시간 계산 (논리적 오류 방지)
     */
    private String calculateOptimalTimeForBlock(String timeBlock, String openTime, String closeTime) {
        String result = switch (timeBlock) {
            case "BREAKFAST" -> {
                // 아침식사: 08:00-10:00 (운영시간 고려)
                String start = isTimeAfter(openTime, "08:00") ? openTime : "08:00";
                String end = isTimeBefore(closeTime, "10:00") ? closeTime : "10:00";
                yield validateTimeRange(start, end, "08:00-10:00");
            }
            case "MORNING_ACTIVITY" -> {
                // 오전활동: 10:00-12:00
                String start = isTimeAfter(openTime, "10:00") ? openTime : "10:00";
                String end = isTimeBefore(closeTime, "12:00") ? closeTime : "12:00";
                yield validateTimeRange(start, end, "10:00-12:00");
            }
            case "LUNCH" -> {
                // 점심: 12:00-14:00
                String start = isTimeAfter(openTime, "12:00") ? openTime : "12:00";
                String end = isTimeBefore(closeTime, "14:00") ? closeTime : "14:00";
                yield validateTimeRange(start, end, "12:00-14:00");
            }
            case "CAFE" -> {
                // 카페: 14:00-15:30
                String start = isTimeAfter(openTime, "14:00") ? openTime : "14:00";
                String end = isTimeBefore(closeTime, "15:30") ? closeTime : "15:30";
                yield validateTimeRange(start, end, "14:00-15:30");
            }
            case "AFTERNOON_ACTIVITY" -> {
                // 오후활동: 15:30-17:00
                String start = isTimeAfter(openTime, "15:30") ? openTime : "15:30";
                String end = isTimeBefore(closeTime, "17:00") ? closeTime : "17:00";
                yield validateTimeRange(start, end, "15:30-17:00");
            }
            case "DINNER" -> {
                // 저녁: 17:00-19:00
                String start = isTimeAfter(openTime, "17:00") ? openTime : "17:00";
                String end = isTimeBefore(closeTime, "19:00") ? closeTime : "19:00";
                yield validateTimeRange(start, end, "17:00-19:00");
            }
            case "EVENING_ACTIVITY" -> {
                // 저녁활동: 19:00-21:00
                String start = isTimeAfter(openTime, "19:00") ? openTime : "19:00";
                String end = isTimeBefore(closeTime, "21:00") ? closeTime : "21:00";
                yield validateTimeRange(start, end, "19:00-21:00");
            }
            default -> "09:00-18:00";
        };
        
        return result;
    }

    /**
     * 시간 범위 검증 및 수정 (논리적 오류 방지)
     */
    private String validateTimeRange(String startTime, String endTime, String defaultTime) {
        // 1. 시간 형식 검증
        if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
            log.warn("잘못된 시간 형식: start={}, end={}, 기본값 사용", startTime, endTime);
            return defaultTime;
        }
        
        // 2. 시작 시간이 끝 시간보다 늦거나 같은 경우 수정
        if (startTime.compareTo(endTime) >= 0) {
            log.warn("시작 시간이 끝 시간보다 늦음: {} >= {}, 기본값 사용", startTime, endTime);
            return defaultTime;
        }
        
        // 3. 시간 차이가 너무 짧은 경우 (30분 미만) 수정
        int startMinutes = timeToMinutes(startTime);
        int endMinutes = timeToMinutes(endTime);
        if (endMinutes - startMinutes < 30) {
            log.warn("시간 범위가 너무 짧음: {}-{}, 기본값 사용", startTime, endTime);
            return defaultTime;
        }
        
        // 4. 정상적인 경우
        return startTime + "-" + endTime;
    }

    /**
     * 시간 형식 검증 (HH:MM 형식)
     */
    private boolean isValidTimeFormat(String time) {
        if (time == null || time.trim().isEmpty()) {
            return false;
        }
        
        try {
            String[] parts = time.split(":");
            if (parts.length != 2) return false;
            
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            
            return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 시간을 분 단위로 변환
     */
    private int timeToMinutes(String time) {
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour * 60 + minute;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 시간 비교 헬퍼 메서드들
     */
    private boolean isTimeAfter(String time1, String time2) {
        return time1.compareTo(time2) >= 0;
    }

    private boolean isTimeBefore(String time1, String time2) {
        return time1.compareTo(time2) <= 0;
    }

    /**
     * 카테고리별 세분화된 추천 시간 (검증 포함)
     */
    private String getBreakfastTime(String category) {
        String result = switch (category) {
            case "카페" -> "08:00-09:30";        // 카페는 조금 일찍
            case "맛집", "음식점" -> "08:30-10:00";  // 맛집은 조금 늦게
            case "관광지" -> "09:00-10:00";      // 관광지는 정시
            default -> "08:00-10:00";
        };
        return validateTimeRange(result.split("-")[0], result.split("-")[1], "08:00-10:00");
    }

    private String getMorningActivityTime(String category) {
        String result = switch (category) {
            case "관광지" -> "10:00-12:00";      // 관광지는 정시
            case "맛집", "음식점" -> "10:30-12:00"; // 맛집은 조금 늦게
            case "카페" -> "10:00-11:30";       // 카페는 조금 일찍
            default -> "10:00-12:00";
        };
        return validateTimeRange(result.split("-")[0], result.split("-")[1], "10:00-12:00");
    }

    private String getLunchTime(String category) {
        String result = switch (category) {
            case "맛집", "음식점" -> "12:00-14:00"; // 맛집은 정시
            case "카페" -> "12:30-14:00";       // 카페는 조금 늦게
            case "관광지" -> "12:00-13:30";      // 관광지는 조금 일찍
            default -> "12:00-14:00";
        };
        return validateTimeRange(result.split("-")[0], result.split("-")[1], "12:00-14:00");
    }

    private String getCafeTime(String category) {
        String result = switch (category) {
            case "카페" -> "14:00-15:30";       // 카페는 정시
            case "맛집", "음식점" -> "14:30-15:30"; // 맛집은 조금 늦게
            case "관광지" -> "14:00-15:00";      // 관광지는 조금 일찍
            default -> "14:00-15:30";
        };
        return validateTimeRange(result.split("-")[0], result.split("-")[1], "14:00-15:30");
    }

    private String getAfternoonActivityTime(String category) {
        String result = switch (category) {
            case "관광지" -> "15:30-17:00";      // 관광지는 정시
            case "맛집", "음식점" -> "16:00-17:00"; // 맛집은 조금 늦게
            case "카페" -> "15:30-16:30";       // 카페는 조금 일찍
            default -> "15:30-17:00";
        };
        return validateTimeRange(result.split("-")[0], result.split("-")[1], "15:30-17:00");
    }

    private String getDinnerTime(String category) {
        String result = switch (category) {
            case "맛집", "음식점" -> "17:00-19:00"; // 맛집은 정시
            case "카페" -> "17:30-19:00";       // 카페는 조금 늦게
            case "관광지" -> "17:00-18:30";      // 관광지는 조금 일찍
            default -> "17:00-19:00";
        };
        return validateTimeRange(result.split("-")[0], result.split("-")[1], "17:00-19:00");
    }

    private String getEveningActivityTime(String category) {
        String result = switch (category) {
            case "문화시설" -> "19:00-21:00";    // 문화시설은 정시
            case "카페" -> "19:30-21:00";       // 카페는 조금 늦게
            case "맛집", "음식점" -> "19:00-20:30"; // 맛집은 조금 일찍
            default -> "19:00-21:00";
        };
        return validateTimeRange(result.split("-")[0], result.split("-")[1], "19:00-21:00");
    }

    // ========== 내부 클래스 및 Enum ==========

    /**
     * 사용자 선호도 정보
     */
    public record UserPreferences(
        TravelStyle travelStyle,        // 여행 스타일
        Budget budget,                  // 예산 범위
        List<String> preferredCategories, // 선호 카테고리
        DateRange travelDates,          // 여행 날짜
        int tripDays,                   // 여행 일수
        int numberOfPeople              // 인원 수
    ) {}

    /**
     * 여행 스타일 Enum
     */
    public enum TravelStyle {
        RELAXATION,  // 휴양/힐링
        ADVENTURE,   // 모험/액티비티
        CULTURAL,    // 문화/역사
        FOODIE,      // 맛집/음식
        SHOPPING,    // 쇼핑
        NATURE       // 자연/생태
    }

    /**
     * 예산 범위 Enum
     */
    public enum Budget {
        LOW,         // 저예산 (FREE, $)
        MEDIUM,      // 중간예산 (FREE, $, $$)
        HIGH,        // 고예산 (FREE, $, $$, $$$)
        UNLIMITED    // 무제한 (모든 가격대)
    }

    /**
     * 날짜 범위 클래스
     */
    public static class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public DateRange(String startDateStr, String endDateStr) {
            this.startDate = LocalDate.parse(startDateStr);
            this.endDate = LocalDate.parse(endDateStr);
        }

        public boolean containsMonday() {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (current.getDayOfWeek().getValue() == 1) { // 월요일
                    return true;
                }
                current = current.plusDays(1);
            }
            return false;
        }

        public List<LocalDate> getAllDates() {
            List<LocalDate> dates = new ArrayList<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                dates.add(current);
                current = current.plusDays(1);
            }
            return dates;
        }

        public int getDays() {
            return (int) startDate.until(endDate).getDays() + 1;
        }

        // Getters
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }
}
