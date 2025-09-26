package com.compass.domain.chat.service.enrichment.impl;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.service.enrichment.AbstractEnrichmentService;
import com.compass.domain.chat.service.enrichment.ApiRateLimiter;
import com.compass.domain.chat.service.enrichment.EnrichmentUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Google Places API를 활용한 여행지 정보 보강 서비스
 */
@Service
@Slf4j
public class GooglePlacesEnrichmentServiceImpl extends AbstractEnrichmentService {

    private final RestTemplate restTemplate;
    private final ApiRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${google.places.api.key:}")
    private String apiKey;

    private static final String PLACES_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json";
    private static final String PLACE_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";
    private static final String PLACE_PHOTO_URL = "https://maps.googleapis.com/maps/api/place/photo";

    public GooglePlacesEnrichmentServiceImpl(
            TravelCandidateRepository travelCandidateRepository,
            RestTemplate restTemplate,
            ApiRateLimiter rateLimiter,
            ObjectMapper objectMapper) {
        super(travelCandidateRepository);
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getServiceName() {
        return "GooglePlacesEnrichment";
    }

    @Override
    public int getPriority() {
        return 1; // 최우선순위
    }

    @Override
    public int getRateLimitDelay() {
        return 100; // 10 QPS = 100ms delay
    }

    @Override
    public boolean isEligible(TravelCandidate candidate) {
        // Google Places는 모든 장소에 적용 가능
        return candidate.getName() != null && !candidate.getName().isEmpty();
    }

    @Override
    public boolean enrichSingle(TravelCandidate candidate) {
        try {
            rateLimiter.acquire("google_places");

            // 1. Place Search로 Place ID 찾기
            String placeId = findPlaceId(candidate.getName(), candidate.getAddress());
            if (placeId == null) {
                log.warn("Place ID를 찾을 수 없음: {}", candidate.getName());
                return false;
            }

            // 2. Place Details로 상세 정보 가져오기
            JsonNode details = getPlaceDetails(placeId);
            if (details == null) {
                return false;
            }

            // 3. 정보 추출 및 업데이트
            boolean updated = false;

            // 좌표
            if (details.has("geometry")) {
                JsonNode location = details.path("geometry").path("location");
                if (location.has("lat") && location.has("lng")) {
                    candidate.setLatitude(location.get("lat").asDouble());
                    candidate.setLongitude(location.get("lng").asDouble());
                    updated = true;
                }
            }

            // 평점 및 리뷰 수
            if (details.has("rating")) {
                candidate.setRating(details.get("rating").asDouble());
                updated = true;
            }
            if (details.has("user_ratings_total")) {
                candidate.setReviewCount(details.get("user_ratings_total").asInt());
                updated = true;
            }

            // 가격대
            if (details.has("price_level")) {
                candidate.setPriceLevel(details.get("price_level").asInt());
                updated = true;
            }

            // 사진 URL
            if (details.has("photos") && details.get("photos").isArray()) {
                List<String> photoUrls = extractPhotoUrls(details.get("photos"));
                if (!photoUrls.isEmpty()) {
                    candidate.setPhotoUrl(photoUrls.get(0)); // 첫 번째 사진
                    updated = true;
                }
            }

            // 전화번호
            if (details.has("formatted_phone_number")) {
                String phone = details.get("formatted_phone_number").asText();
                candidate.setPhoneNumber(EnrichmentUtils.normalizePhoneNumber(phone));
                updated = true;
            }

            // 웹사이트
            if (details.has("website")) {
                candidate.setWebsite(details.get("website").asText());
                updated = true;
            }

            // 영업시간
            if (details.has("opening_hours")) {
                JsonNode hours = details.get("opening_hours");
                if (hours.has("weekday_text")) {
                    String businessHours = formatBusinessHours(hours.get("weekday_text"));
                    candidate.setBusinessHours(EnrichmentUtils.truncateString(businessHours, 500));
                    updated = true;
                }
            }

            // Google Place ID 저장
            if (details.has("place_id")) {
                candidate.setGooglePlaceId(details.get("place_id").asText());
                updated = true;
            }

            // 카테고리 (types)
            if (details.has("types") && details.get("types").isArray()) {
                String category = extractCategory(details.get("types"));
                if (category != null && candidate.getCategory() == null) {
                    candidate.setCategory(category);
                    updated = true;
                }
            }

            // 업데이트 시간 기록
            if (updated) {
                candidate.setUpdatedAt(LocalDateTime.now());
            }

            return updated;

        } catch (Exception e) {
            rateLimiter.handleBackoff("google_places", e);
            log.error("Google Places 보강 실패: {} - {}", candidate.getName(), e.getMessage());
            return false;
        }
    }

    private String findPlaceId(String name, String address) {
        try {
            String query = name;
            if (address != null && !address.isEmpty()) {
                query = name + " " + address;
            }

            String url = UriComponentsBuilder.fromUriString(PLACES_SEARCH_URL)
                .queryParam("input", query)
                .queryParam("inputtype", "textquery")
                .queryParam("fields", "place_id,name,formatted_address")
                .queryParam("language", "ko")
                .queryParam("key", apiKey)
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("candidates") && root.get("candidates").size() > 0) {
                JsonNode candidate = root.get("candidates").get(0);

                // 이름 유사도 검증
                if (candidate.has("name")) {
                    String foundName = candidate.get("name").asText();
                    double similarity = EnrichmentUtils.calculateSimilarity(name, foundName);
                    if (similarity > 0.7) {
                        return candidate.get("place_id").asText();
                    }
                }

                // 유사도가 낮아도 주소가 일치하면 수용
                return candidate.get("place_id").asText();
            }

        } catch (Exception e) {
            log.error("Place ID 검색 실패: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode getPlaceDetails(String placeId) {
        try {
            String url = UriComponentsBuilder.fromUriString(PLACE_DETAILS_URL)
                .queryParam("place_id", placeId)
                .queryParam("fields", "name,rating,user_ratings_total,price_level,photos," +
                    "formatted_phone_number,website,opening_hours,geometry,types,place_id")
                .queryParam("language", "ko")
                .queryParam("key", apiKey)
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("result")) {
                return root.get("result");
            }

        } catch (Exception e) {
            log.error("Place Details 조회 실패: {}", e.getMessage());
        }
        return null;
    }

    private List<String> extractPhotoUrls(JsonNode photos) {
        List<String> urls = new ArrayList<>();
        for (JsonNode photo : photos) {
            if (photo.has("photo_reference")) {
                String photoUrl = UriComponentsBuilder.fromUriString(PLACE_PHOTO_URL)
                    .queryParam("maxwidth", 800)
                    .queryParam("photo_reference", photo.get("photo_reference").asText())
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();
                urls.add(photoUrl);
                if (urls.size() >= 3) break; // 최대 3개
            }
        }
        return urls;
    }

    private String formatBusinessHours(JsonNode weekdayText) {
        if (!weekdayText.isArray()) return null;

        StringBuilder sb = new StringBuilder();
        for (JsonNode day : weekdayText) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(day.asText());
        }
        return sb.toString();
    }

    private String extractCategory(JsonNode types) {
        // Google Places types를 한국어 카테고리로 변환
        for (JsonNode type : types) {
            String typeStr = type.asText();
            String category = mapGoogleTypeToCategory(typeStr);
            if (category != null) {
                return category;
            }
        }
        return null;
    }

    private String mapGoogleTypeToCategory(String googleType) {
        return switch (googleType) {
            case "restaurant" -> "음식점";
            case "cafe" -> "카페";
            case "tourist_attraction" -> "관광명소";
            case "museum" -> "박물관";
            case "park" -> "공원";
            case "shopping_mall" -> "쇼핑";
            case "lodging" -> "숙박";
            case "amusement_park" -> "테마파크";
            case "beach" -> "해변";
            case "temple", "church" -> "종교시설";
            case "spa" -> "스파";
            case "night_club", "bar" -> "나이트라이프";
            default -> null;
        };
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> baseStats = super.getStatistics();

        long totalCandidates = travelCandidateRepository.count();
        long withCoordinates = travelCandidateRepository.countByLatitudeIsNotNullAndLongitudeIsNotNull();
        long withRating = travelCandidateRepository.countByRatingIsNotNull();
        long withPhotos = travelCandidateRepository.countByPhotoUrlIsNotNull();

        baseStats.put("coordinatesCompletionRate", (double) withCoordinates / totalCandidates * 100);
        baseStats.put("ratingCompletionRate", (double) withRating / totalCandidates * 100);
        baseStats.put("photoCompletionRate", (double) withPhotos / totalCandidates * 100);
        baseStats.put("completionRate",
            (withCoordinates + withRating + withPhotos) / (totalCandidates * 3.0) * 100);

        return baseStats;
    }
}