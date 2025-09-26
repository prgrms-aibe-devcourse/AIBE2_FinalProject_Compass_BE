package com.compass.domain.chat.service;

import com.compass.domain.chat.model.TravelPlace;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// Google Places API를 사용한 고품질 여행지 수집 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlacesCollector {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${google.places.api.key}")
    private String apiKey;

    // Google Places API Base URL
    private static final String PLACES_API_URL = "https://maps.googleapis.com/maps/api/place";
    private static final String NEARBY_SEARCH_URL = "/nearbysearch/json";
    private static final String TEXT_SEARCH_URL = "/textsearch/json";
    private static final String PLACE_DETAILS_URL = "/details/json";

    // 한국 주요 도시별 5개 세부 지역 좌표 (클러스터링 기반)
    private static final Map<String, List<SubRegion>> KOREAN_CITIES_WITH_SUBREGIONS = Map.ofEntries(
        Map.entry("서울", List.of(
            new SubRegion("홍대/신촌", new Location(37.5563, 126.9220)),
            new SubRegion("강남/서초", new Location(37.4979, 127.0276)),
            new SubRegion("종로/명동", new Location(37.5636, 126.9869)),
            new SubRegion("성수/건대", new Location(37.5446, 127.0565)),
            new SubRegion("잠실/송파", new Location(37.5113, 127.0980))
        )),
        Map.entry("부산", List.of(
            new SubRegion("해운대/마린시티", new Location(35.1587, 129.1604)),
            new SubRegion("서면/부산진구", new Location(35.1578, 129.0600)),
            new SubRegion("남포동/중구", new Location(35.0983, 129.0303)),
            new SubRegion("광안리/수영구", new Location(35.1531, 129.1189)),
            new SubRegion("센텀시티", new Location(35.1689, 129.1308))
        )),
        Map.entry("제주", List.of(
            new SubRegion("제주시내", new Location(33.4996, 126.5312)),
            new SubRegion("서귀포시내", new Location(33.2541, 126.5600)),
            new SubRegion("애월/한림", new Location(33.4636, 126.3189)),
            new SubRegion("성산/우도", new Location(33.4587, 126.9425)),
            new SubRegion("중문/모슬포", new Location(33.2461, 126.4127))
        )),
        Map.entry("인천", List.of(
            new SubRegion("송도", new Location(37.3825, 126.6564)),
            new SubRegion("부평", new Location(37.5074, 126.7219)),
            new SubRegion("구월동", new Location(37.4481, 126.7052)),
            new SubRegion("인천공항/영종도", new Location(37.4602, 126.4407)),
            new SubRegion("청라", new Location(37.5341, 126.6254))
        )),
        Map.entry("대구", List.of(
            new SubRegion("동성로/중구", new Location(35.8689, 128.5950)),
            new SubRegion("수성구", new Location(35.8582, 128.6305)),
            new SubRegion("달서구", new Location(35.8298, 128.5325)),
            new SubRegion("북구", new Location(35.8858, 128.5829)),
            new SubRegion("경북대/산격동", new Location(35.8890, 128.6107))
        ))
    );

    // 기타 주요 여행 도시들
    private static final Map<String, Location> SINGLE_POINT_CITIES = Map.ofEntries(
        // 광역시
        Map.entry("대전", new Location(36.3504, 127.3845)),
        Map.entry("광주", new Location(35.1595, 126.8526)),
        Map.entry("울산", new Location(35.5384, 129.3114)),

        // 경기도 주요 도시
        Map.entry("수원", new Location(37.2636, 127.0286)),
        Map.entry("성남", new Location(37.4200, 127.1267)),
        Map.entry("용인", new Location(37.2411, 127.1776)),
        Map.entry("파주", new Location(37.7599, 126.7765)),
        Map.entry("가평", new Location(37.8315, 127.5098)),
        Map.entry("양평", new Location(37.4912, 127.4876)),

        // 강원도 주요 관광지
        Map.entry("강릉", new Location(37.7519, 128.8761)),
        Map.entry("속초", new Location(38.2070, 128.5918)),
        Map.entry("춘천", new Location(37.8813, 127.7298)),
        Map.entry("평창", new Location(37.3708, 128.3906)),
        Map.entry("정선", new Location(37.3808, 128.6609)),
        Map.entry("양양", new Location(38.0753, 128.6189)),

        // 충청도 주요 도시
        Map.entry("천안", new Location(36.8151, 127.1139)),
        Map.entry("청주", new Location(36.6424, 127.4890)),
        Map.entry("보령", new Location(36.3332, 126.6127)),
        Map.entry("태안", new Location(36.7456, 126.2978)),
        Map.entry("단양", new Location(36.9847, 128.3655)),

        // 전라도 주요 관광지
        Map.entry("전주", new Location(35.8242, 127.1480)),
        Map.entry("여수", new Location(34.7604, 127.6622)),
        Map.entry("순천", new Location(34.9507, 127.4873)),
        Map.entry("담양", new Location(35.3211, 126.9882)),
        Map.entry("남원", new Location(35.4164, 127.3903)),
        Map.entry("목포", new Location(34.8118, 126.3922)),
        Map.entry("완도", new Location(34.3114, 126.7549)),

        // 경상도 주요 관광지
        Map.entry("경주", new Location(35.8562, 129.2246)),
        Map.entry("안동", new Location(36.5684, 128.7294)),
        Map.entry("포항", new Location(36.0190, 129.3435)),
        Map.entry("통영", new Location(34.8544, 128.4332)),
        Map.entry("거제", new Location(34.8806, 128.6211)),
        Map.entry("창원", new Location(35.2280, 128.6811)),
        Map.entry("김해", new Location(35.2285, 128.8893)),
        Map.entry("양산", new Location(35.3350, 129.0372)),
        Map.entry("남해", new Location(34.8377, 127.8924))
    );

    // 시간 블럭 정의 (기존 시스템과 통합)
    public enum TimeBlock {
        BREAKFAST("아침식사", 7, 9),
        MORNING_ACTIVITY("오전일과", 9, 12),
        LUNCH("점심식사", 12, 14),
        AFTERNOON_ACTIVITY("오후일과", 14, 18),
        DINNER("저녁식사", 18, 20),
        EVENING_ACTIVITY("저녁일과", 20, 23);

        private final String koreanName;
        private final int startHour;
        private final int endHour;

        TimeBlock(String koreanName, int startHour, int endHour) {
            this.koreanName = koreanName;
            this.startHour = startHour;
            this.endHour = endHour;
        }
    }

    // 시간대별 Google Places 카테고리 매핑
    private static final Map<TimeBlock, List<String>> TIME_BLOCK_CATEGORIES = Map.of(
        TimeBlock.BREAKFAST, List.of("cafe", "bakery", "restaurant"),
        TimeBlock.MORNING_ACTIVITY, List.of("tourist_attraction", "museum", "art_gallery", "park", "hindu_temple", "church"),
        TimeBlock.LUNCH, List.of("restaurant"),
        TimeBlock.AFTERNOON_ACTIVITY, List.of("shopping_mall", "tourist_attraction", "amusement_park", "zoo", "aquarium"),
        TimeBlock.DINNER, List.of("restaurant"),
        TimeBlock.EVENING_ACTIVITY, List.of("night_club", "bar", "spa", "tourist_attraction")
    );

    // 카테고리 태그 정의 (한국어)
    private static final Map<String, Set<String>> CATEGORY_TAGS = Map.of(
        "관광지", Set.of("tourist_attraction", "museum", "art_gallery", "park", "hindu_temple", "church", "zoo", "aquarium", "amusement_park"),
        "맛집", Set.of("restaurant"),
        "카페", Set.of("cafe", "bakery"),
        "쇼핑", Set.of("shopping_mall", "department_store"),
        "액티비티", Set.of("amusement_park", "zoo", "aquarium", "spa"),
        "문화", Set.of("museum", "art_gallery", "hindu_temple", "church"),
        "자연", Set.of("park", "natural_feature"),
        "나이트라이프", Set.of("night_club", "bar")
    );

    // 시간블럭과 카테고리를 고려한 고품질 여행지 수집 (5개 세부 지역별)
    public Map<TimeBlock, List<GooglePlace>> collectPlacesByTimeBlock(String region, int placesPerTimeBlock) {
        log.info("Google Places API로 {} 지역 시간대별 장소 수집 시작 (시간대당 {}개)", region, placesPerTimeBlock);

        Map<TimeBlock, List<GooglePlace>> timeBlockPlaces = new ConcurrentHashMap<>();

        // 5개 세부 지역이 있는 도시인지 확인
        List<SubRegion> subRegions = KOREAN_CITIES_WITH_SUBREGIONS.get(region);

        if (subRegions != null) {
            log.info("{} 지역의 5개 세부 지역에서 수집 시작", region);

            // 시간블럭별로 각 세부 지역에서 수집
            List<CompletableFuture<Void>> futures = Arrays.stream(TimeBlock.values())
                .map(timeBlock -> CompletableFuture.runAsync(() -> {
                    try {
                        List<GooglePlace> allPlacesForTimeBlock = new ArrayList<>();

                        // 5개 세부 지역별로 수집
                        for (SubRegion subRegion : subRegions) {
                            log.debug("{} - {} 지역에서 {} 시간대 수집 중",
                                region, subRegion.name, timeBlock.koreanName);

                            // 각 세부 지역에서 시간대당 장소 수집 (세부 지역당 20개씩)
                            int placesPerSubRegion = 20;  // 각 세부지역에서 20개씩 수집 (더 많은 데이터 확보)
                            List<GooglePlace> subRegionPlaces = collectForTimeBlock(
                                subRegion.location, timeBlock, placesPerSubRegion);

                            // 세부 지역 정보를 장소에 태깅
                            subRegionPlaces.forEach(place -> place.subRegion = subRegion.name);
                            allPlacesForTimeBlock.addAll(subRegionPlaces);
                        }

                        // 전체 수집된 장소를 인기도순으로 정렬 (제한 없이 모두 저장)
                        List<GooglePlace> topPlaces = allPlacesForTimeBlock.stream()
                            .sorted((p1, p2) -> {
                                int reviewComparison = Integer.compare(p2.userRatingsTotal, p1.userRatingsTotal);
                                if (reviewComparison != 0) return reviewComparison;
                                return Double.compare(p2.rating, p1.rating);
                            })
                            // 제한 없이 모든 수집된 장소 유지
                            .collect(Collectors.toList());

                        if (!topPlaces.isEmpty()) {
                            timeBlockPlaces.put(timeBlock, topPlaces);
                            log.info("{} - {} 시간대 수집 완료: {}개 (평균 평점: {}, 평균 리뷰: {})",
                                region, timeBlock.koreanName, topPlaces.size(),
                                String.format("%.1f", topPlaces.stream().mapToDouble(p -> p.rating).average().orElse(0.0)),
                                String.format("%.0f", topPlaces.stream().mapToInt(p -> p.userRatingsTotal).average().orElse(0.0))
                            );
                        }
                    } catch (Exception e) {
                        log.error("{} - {} 시간대 수집 실패: {}", region, timeBlock.koreanName, e.getMessage());
                    }
                }))
                .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } else {
            // 단일 중심점 도시 처리
            Location location = SINGLE_POINT_CITIES.get(region);

            if (location == null) {
                log.warn("{} 지역의 좌표를 찾을 수 없습니다.", region);
                return timeBlockPlaces;
            }

            // 기존 로직 사용
            List<CompletableFuture<Void>> futures = Arrays.stream(TimeBlock.values())
                .map(timeBlock -> CompletableFuture.runAsync(() -> {
                    try {
                        List<GooglePlace> places = collectForTimeBlock(location, timeBlock, placesPerTimeBlock);
                        if (!places.isEmpty()) {
                            timeBlockPlaces.put(timeBlock, places);
                            log.info("{} - {} 시간대 수집 완료: {}개",
                                region, timeBlock.koreanName, places.size());
                        }
                    } catch (Exception e) {
                        log.error("{} - {} 시간대 수집 실패: {}", region, timeBlock.koreanName, e.getMessage());
                    }
                }))
                .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        return timeBlockPlaces;
    }

    // 특정 시간블럭에 맞는 장소 수집
    private List<GooglePlace> collectForTimeBlock(Location location, TimeBlock timeBlock, int limit) {
        List<String> categories = TIME_BLOCK_CATEGORIES.get(timeBlock);
        List<GooglePlace> allPlaces = new ArrayList<>();

        // 해당 시간대의 모든 카테고리에서 장소 수집
        for (String category : categories) {
            List<GooglePlace> categoryPlaces = searchPlacesByCategory(location, category, limit * 2);
            allPlaces.addAll(categoryPlaces);
        }

        // 중복 제거 (place_id 기준)
        Map<String, GooglePlace> uniquePlaces = allPlaces.stream()
            .collect(Collectors.toMap(
                p -> p.placeId,
                p -> p,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));

        // 인기도(리뷰 수)와 평점으로 정렬
        List<GooglePlace> sortedPlaces = new ArrayList<>(uniquePlaces.values());
        sortedPlaces.sort((p1, p2) -> {
            // 1차: 리뷰 수 (인기도)
            int reviewComparison = Integer.compare(p2.userRatingsTotal, p1.userRatingsTotal);
            if (reviewComparison != 0) return reviewComparison;

            // 2차: 평점
            return Double.compare(p2.rating, p1.rating);
        });

        // 상위 N개만 반환
        return sortedPlaces.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    // 카테고리 태그 기반 수집 (기존 메서드 유지)
    public Map<String, List<GooglePlace>> collectByCategories(String region, Set<String> categoryTags, int placesPerCategory) {
        log.info("카테고리 태그 기반 {} 지역 장소 수집: {}", region, categoryTags);

        Map<String, List<GooglePlace>> categoryPlaces = new ConcurrentHashMap<>();

        // 지역의 대표 좌표 가져오기
        Location location = getRegionLocation(region);

        if (location == null) {
            return categoryPlaces;
        }

        // 요청된 카테고리 태그에 해당하는 Google Places 타입 추출
        Set<String> googleTypes = new HashSet<>();
        for (String tag : categoryTags) {
            Set<String> types = CATEGORY_TAGS.get(tag);
            if (types != null) {
                googleTypes.addAll(types);
            }
        }

        // 각 Google 타입별로 수집
        List<CompletableFuture<Void>> futures = googleTypes.stream()
            .map(type -> CompletableFuture.runAsync(() -> {
                try {
                    List<GooglePlace> places = searchPlacesByCategory(location, type, placesPerCategory);
                    if (!places.isEmpty()) {
                        // 한국어 카테고리로 매핑하여 저장
                        String koreanCategory = mapGoogleTypeToKoreanCategory(type);
                        categoryPlaces.merge(koreanCategory, places, (existing, newPlaces) -> {
                            List<GooglePlace> merged = new ArrayList<>(existing);
                            merged.addAll(newPlaces);
                            return merged;
                        });
                    }
                } catch (Exception e) {
                    log.error("카테고리 {} 수집 실패: {}", type, e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 각 카테고리별로 인기도 정렬
        categoryPlaces.forEach((category, places) -> {
            places.sort((p1, p2) -> {
                // 1차: 리뷰 수 (인기도)
                int reviewComparison = Integer.compare(p2.userRatingsTotal, p1.userRatingsTotal);
                if (reviewComparison != 0) return reviewComparison;
                // 2차: 평점
                return Double.compare(p2.rating, p1.rating);
            });
        });

        return categoryPlaces;
    }

    // 카테고리별 장소 검색
    private List<GooglePlace> searchPlacesByCategory(Location location, String category, int limit) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("location", String.format("%f,%f", location.lat, location.lng));
            params.put("radius", "50000"); // 50km 반경
            params.put("type", category);
            params.put("key", apiKey);
            params.put("language", "ko"); // 한국어 결과

            String url = PLACES_API_URL + NEARBY_SEARCH_URL +
                "?location=" + params.get("location") +
                "&radius=" + params.get("radius") +
                "&type=" + params.get("type") +
                "&key=" + params.get("key") +
                "&language=" + params.get("language");

            log.debug("Google Places API URL: {}", url);

            String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return parseGooglePlacesResponse(response, limit);

        } catch (Exception e) {
            log.error("Google Places API 호출 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Google Places API 응답 파싱
    private List<GooglePlace> parseGooglePlacesResponse(String response, int limit) {
        List<GooglePlace> places = new ArrayList<>();

        try {
            var jsonNode = objectMapper.readTree(response);
            var results = jsonNode.get("results");

            if (results != null && results.isArray()) {
                for (var placeNode : results) {
                    GooglePlace place = new GooglePlace();

                    place.placeId = placeNode.path("place_id").asText();
                    place.name = placeNode.path("name").asText();
                    place.rating = placeNode.path("rating").asDouble(0.0);
                    place.userRatingsTotal = placeNode.path("user_ratings_total").asInt(0);
                    place.priceLevel = placeNode.path("price_level").asInt(-1);
                    place.vicinity = placeNode.path("vicinity").asText();

                    var location = placeNode.path("geometry").path("location");
                    place.latitude = location.path("lat").asDouble();
                    place.longitude = location.path("lng").asDouble();

                    var types = placeNode.path("types");
                    if (types.isArray()) {
                        List<String> typeList = new ArrayList<>();
                        types.forEach(type -> typeList.add(type.asText()));
                        place.types = typeList;
                    }

                    // 사진 정보
                    var photos = placeNode.path("photos");
                    if (photos.isArray() && photos.size() > 0) {
                        place.photoReference = photos.get(0).path("photo_reference").asText();
                    }

                    // 영업 상태
                    var openingHours = placeNode.path("opening_hours");
                    if (openingHours != null) {
                        place.openNow = openingHours.path("open_now").asBoolean(false);
                    }

                    places.add(place);
                }
            }

            // 평점과 리뷰 수 기반 정렬 (높은 평점 + 많은 리뷰 우선)
            places.sort((p1, p2) -> {
                // 평점 가중치: 70%, 리뷰 수 가중치: 30%
                double score1 = calculateQualityScore(p1);
                double score2 = calculateQualityScore(p2);
                return Double.compare(score2, score1);
            });

            // 상위 N개만 선택
            if (places.size() > limit) {
                places = places.subList(0, limit);
            }

        } catch (Exception e) {
            log.error("응답 파싱 실패: {}", e.getMessage());
        }

        return places;
    }

    // 장소 품질 점수 계산 (평점과 리뷰 수 고려)
    private double calculateQualityScore(GooglePlace place) {
        // 평점 정규화 (0-5 -> 0-1)
        double normalizedRating = place.rating / 5.0;

        // 리뷰 수 로그 스케일 정규화 (리뷰가 많을수록 신뢰도 높음)
        double normalizedReviews = Math.log10(place.userRatingsTotal + 1) / 4.0; // log10(10000) = 4
        normalizedReviews = Math.min(normalizedReviews, 1.0); // 최대값 1로 제한

        // 가중치 적용 (평점 70%, 리뷰 수 30%)
        return (normalizedRating * 0.7) + (normalizedReviews * 0.3);
    }

    // 한국어 카테고리 매핑 헬퍼 메서드
    private String mapGoogleTypeToKoreanCategory(String googleType) {
        for (Map.Entry<String, Set<String>> entry : CATEGORY_TAGS.entrySet()) {
            if (entry.getValue().contains(googleType)) {
                return entry.getKey();
            }
        }
        return "기타";
    }

    // 전체 지역 일괄 수집 (시간블럭 기반)
    public Map<String, Map<TimeBlock, List<GooglePlace>>> collectAllRegionsByTimeBlock(int placesPerTimeBlock) {
        Map<String, Map<TimeBlock, List<GooglePlace>>> allRegionData = new ConcurrentHashMap<>();

        // 모든 지역명 수집
        Set<String> allRegions = new HashSet<>();
        allRegions.addAll(KOREAN_CITIES_WITH_SUBREGIONS.keySet());
        allRegions.addAll(SINGLE_POINT_CITIES.keySet());

        List<CompletableFuture<Void>> futures = allRegions.stream()
            .map(region -> CompletableFuture.runAsync(() -> {
                try {
                    Map<TimeBlock, List<GooglePlace>> timeBlockData = collectPlacesByTimeBlock(region, placesPerTimeBlock);
                    allRegionData.put(region, timeBlockData);

                    int totalPlaces = timeBlockData.values().stream()
                        .mapToInt(List::size)
                        .sum();
                    log.info("{} 지역 수집 완료: 총 {}개 장소 ({}개 시간대)",
                        region, totalPlaces, timeBlockData.size());

                } catch (Exception e) {
                    log.error("{} 지역 수집 실패: {}", region, e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return allRegionData;
    }

    // 전체 지역 일괄 수집 (기존 메서드 유지 - 하위 호환성)
    public Map<String, List<GooglePlace>> collectAllRegions(int placesPerRegion) {
        Map<String, List<GooglePlace>> allRegionData = new ConcurrentHashMap<>();

        // 모든 지역명 수집
        Set<String> allRegions = new HashSet<>();
        allRegions.addAll(KOREAN_CITIES_WITH_SUBREGIONS.keySet());
        allRegions.addAll(SINGLE_POINT_CITIES.keySet());

        List<CompletableFuture<Void>> futures = allRegions.stream()
            .map(region -> CompletableFuture.runAsync(() -> {
                try {
                    // 시간블럭별로 수집한 후 통합
                    Map<TimeBlock, List<GooglePlace>> timeBlockData = collectPlacesByTimeBlock(region, placesPerRegion / TimeBlock.values().length);

                    // 모든 시간블럭의 장소를 하나의 리스트로 통합
                    List<GooglePlace> allPlaces = timeBlockData.values().stream()
                        .flatMap(List::stream)
                        .distinct()
                        .sorted((p1, p2) -> Double.compare(calculateQualityScore(p2), calculateQualityScore(p1)))
                        .limit(placesPerRegion)
                        .collect(Collectors.toList());

                    allRegionData.put(region, allPlaces);
                    log.info("{} 지역 수집 완료: {}개 고품질 장소", region, allPlaces.size());

                } catch (Exception e) {
                    log.error("{} 지역 수집 실패: {}", region, e.getMessage());
                }
            }))
            .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return allRegionData;
    }

    // Google Place 데이터 모델
    public static class GooglePlace {
        public String placeId;
        public String name;
        public double rating;           // 평점 (0.0 ~ 5.0)
        public int userRatingsTotal;    // 총 리뷰 수
        public int priceLevel;          // 가격대 (0 ~ 4)
        public String vicinity;          // 주소
        public double latitude;
        public double longitude;
        public List<String> types;       // 장소 유형들
        public String photoReference;    // 사진 참조
        public boolean openNow;          // 현재 영업 여부
        public String subRegion;         // 세부 지역명 (예: "홍대/신촌")

        // TravelPlace로 변환
        public TravelPlace toTravelPlace() {
            TravelPlace place = new TravelPlace();
            place.setName(this.name);
            place.setLatitude(this.latitude);
            place.setLongitude(this.longitude);
            place.setAddress(this.vicinity);
            place.setRating(this.rating);
            place.setReviewCount(this.userRatingsTotal);

            // 카테고리 매핑
            if (types != null && !types.isEmpty()) {
                place.setCategory(mapGoogleTypeToCategory(types.get(0)));
            }

            // 설명 생성
            String description = String.format("평점: %.1f (%d개 리뷰)", rating, userRatingsTotal);
            if (priceLevel >= 0) {
                description += " | 가격대: " + "$".repeat(priceLevel + 1);
            }
            place.setDescription(description);

            return place;
        }

        private String mapGoogleTypeToCategory(String googleType) {
            return switch (googleType) {
                case "tourist_attraction", "museum", "art_gallery", "park" -> "관광지";
                case "restaurant" -> "맛집";
                case "cafe", "bakery" -> "카페";
                case "shopping_mall", "department_store" -> "쇼핑";
                case "lodging", "hotel" -> "숙박";
                case "spa", "amusement_park", "zoo", "aquarium" -> "액티비티";
                case "hindu_temple", "church" -> "문화";
                case "night_club", "bar" -> "나이트라이프";
                default -> "기타";
            };
        }

        // 시간 블럭 매핑 추가
        public TimeBlock mapToTimeBlock() {
            // Google 타입에 따른 기본 시간 블럭 할당
            if (types != null && !types.isEmpty()) {
                String primaryType = types.get(0);
                return switch (primaryType) {
                    case "cafe", "bakery" -> TimeBlock.BREAKFAST;
                    case "tourist_attraction", "museum", "art_gallery", "park" -> TimeBlock.MORNING_ACTIVITY;
                    case "restaurant" -> determineMealTimeBlock();
                    case "shopping_mall", "department_store" -> TimeBlock.AFTERNOON_ACTIVITY;
                    case "night_club", "bar" -> TimeBlock.EVENING_ACTIVITY;
                    case "spa" -> TimeBlock.EVENING_ACTIVITY;
                    default -> TimeBlock.AFTERNOON_ACTIVITY;
                };
            }
            return TimeBlock.AFTERNOON_ACTIVITY;
        }

        // 식당의 경우 시간대 결정 (영업시간 기반)
        private TimeBlock determineMealTimeBlock() {
            // 기본적으로 점심으로 설정, 추후 영업시간 정보로 세분화 가능
            if (name.contains("조식") || name.contains("아침")) {
                return TimeBlock.BREAKFAST;
            } else if (name.contains("저녁") || name.contains("dinner")) {
                return TimeBlock.DINNER;
            }
            return TimeBlock.LUNCH;
        }
    }

    // 위치 정보
    private static class Location {
        public final double lat;
        public final double lng;

        public Location(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    // 세부 지역 정보
    private static class SubRegion {
        public final String name;
        public final Location location;

        public SubRegion(String name, Location location) {
            this.name = name;
            this.location = location;
        }
    }

    // 지역의 대표 좌표 가져오기 헬퍼 메서드
    private Location getRegionLocation(String region) {
        // 세부 지역이 있는 도시인 경우 첫 번째 세부 지역의 좌표 반환
        List<SubRegion> subRegions = KOREAN_CITIES_WITH_SUBREGIONS.get(region);
        if (subRegions != null && !subRegions.isEmpty()) {
            return subRegions.get(0).location;
        }

        // 단일 중심점 도시인 경우
        return SINGLE_POINT_CITIES.get(region);
    }

    // 모든 지역 목록 반환
    public List<String> getAllRegions() {
        List<String> regions = new ArrayList<>();

        // 세부 지역이 있는 도시들 추가
        regions.addAll(KOREAN_CITIES_WITH_SUBREGIONS.keySet());

        // 단일 중심점 도시들 추가
        regions.addAll(SINGLE_POINT_CITIES.keySet());

        return regions;
    }
}