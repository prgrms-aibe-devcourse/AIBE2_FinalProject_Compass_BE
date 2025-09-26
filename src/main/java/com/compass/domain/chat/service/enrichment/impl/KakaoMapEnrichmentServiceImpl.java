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
import java.util.Map;

/**
 * 카카오맵 API를 활용한 여행지 정보 보강 서비스
 */
@Service
@Slf4j
public class KakaoMapEnrichmentServiceImpl extends AbstractEnrichmentService {

    private final RestTemplate restTemplate;
    private final ApiRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${kakao.rest.key:}")
    private String kakaoRestKey;

    private static final String KAKAO_KEYWORD_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final String KAKAO_COORD_TO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/geo/coord2address.json";

    public KakaoMapEnrichmentServiceImpl(
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
        return "KakaoMapEnrichment";
    }

    @Override
    public int getPriority() {
        return 2; // Google 다음 우선순위
    }

    @Override
    public int getRateLimitDelay() {
        return 35; // 30 QPS = 33ms delay, 약간 여유
    }

    @Override
    public boolean isEligible(TravelCandidate candidate) {
        // 이름이 있고, 한국 장소인 경우
        return candidate.getName() != null &&
               !candidate.getName().isEmpty() &&
               EnrichmentUtils.containsKorean(candidate.getName());
    }

    @Override
    public boolean enrichSingle(TravelCandidate candidate) {
        try {
            rateLimiter.acquire("kakao_map");

            // 키워드 검색으로 장소 정보 찾기
            JsonNode placeInfo = searchPlaceByKeyword(candidate.getName(), candidate.getAddress());
            if (placeInfo == null) {
                log.debug("카카오맵에서 장소를 찾을 수 없음: {}", candidate.getName());
                return false;
            }

            boolean updated = false;

            // 카테고리 정제
            if (placeInfo.has("category_name")) {
                String category = EnrichmentUtils.refineCategory(placeInfo.get("category_name").asText());
                if (category != null && (candidate.getCategory() == null || candidate.getCategory().isEmpty())) {
                    candidate.setCategory(category);
                    updated = true;
                }
            }

            // 전화번호 보완
            if (placeInfo.has("phone") && !placeInfo.get("phone").asText().isEmpty()) {
                String phone = EnrichmentUtils.normalizePhoneNumber(placeInfo.get("phone").asText());
                if (phone != null && (candidate.getPhoneNumber() == null || candidate.getPhoneNumber().isEmpty())) {
                    candidate.setPhoneNumber(phone);
                    updated = true;
                }
            }

            // 좌표 보정 (Google보다 정확할 수 있음)
            if (placeInfo.has("x") && placeInfo.has("y")) {
                double lng = placeInfo.get("x").asDouble();
                double lat = placeInfo.get("y").asDouble();

                // 좌표가 없거나, 기존 좌표와 거리 차이가 큰 경우 업데이트
                if (candidate.getLatitude() == null || candidate.getLongitude() == null ||
                    calculateDistance(candidate.getLatitude(), candidate.getLongitude(), lat, lng) > 0.1) {
                    candidate.setLatitude(lat);
                    candidate.setLongitude(lng);
                    updated = true;
                }
            }

            // 상세 주소 업데이트 (더 정확한 한국 주소)
            if (placeInfo.has("road_address_name") && !placeInfo.get("road_address_name").asText().isEmpty()) {
                String roadAddress = placeInfo.get("road_address_name").asText();
                // 기존 주소가 없거나 간략한 경우 업데이트
                if (candidate.getAddress() == null ||
                    candidate.getAddress().length() < roadAddress.length()) {
                    candidate.setAddress(roadAddress);
                    updated = true;
                }
            } else if (placeInfo.has("address_name") && !placeInfo.get("address_name").asText().isEmpty()) {
                String address = placeInfo.get("address_name").asText();
                if (candidate.getAddress() == null ||
                    candidate.getAddress().length() < address.length()) {
                    candidate.setAddress(address);
                    updated = true;
                }
            }

            // 카카오 Place ID 저장
            if (placeInfo.has("id")) {
                candidate.setKakaoPlaceId(placeInfo.get("id").asText());
                updated = true;
            }

            // 지역 정보 추출
            if (candidate.getRegion() == null && placeInfo.has("address_name")) {
                String region = extractRegion(placeInfo.get("address_name").asText());
                if (region != null) {
                    candidate.setRegion(region);
                    updated = true;
                }
            }

            // 업데이트 시간 기록
            if (updated) {
                candidate.setUpdatedAt(LocalDateTime.now());
            }

            return updated;

        } catch (Exception e) {
            rateLimiter.handleBackoff("kakao_map", e);
            log.error("카카오맵 보강 실패: {} - {}", candidate.getName(), e.getMessage());
            return false;
        }
    }

    private JsonNode searchPlaceByKeyword(String name, String address) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestKey);

            // 검색 쿼리 구성
            String query = name;
            if (address != null && !address.isEmpty()) {
                // 지역명이 포함된 경우 더 정확한 검색
                query = name + " " + extractMainLocation(address);
            }

            String url = UriComponentsBuilder.fromUriString(KAKAO_KEYWORD_SEARCH_URL)
                .queryParam("query", query)
                .queryParam("size", 5)
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.has("documents") && root.get("documents").size() > 0) {
                // 가장 유사한 결과 찾기
                JsonNode bestMatch = null;
                double bestSimilarity = 0;

                for (JsonNode doc : root.get("documents")) {
                    if (doc.has("place_name")) {
                        double similarity = EnrichmentUtils.calculateSimilarity(
                            name, doc.get("place_name").asText()
                        );
                        if (similarity > bestSimilarity) {
                            bestSimilarity = similarity;
                            bestMatch = doc;
                        }
                    }
                }

                // 유사도가 0.6 이상인 경우만 반환
                if (bestSimilarity > 0.6) {
                    return bestMatch;
                }
            }

        } catch (Exception e) {
            log.error("카카오맵 키워드 검색 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractMainLocation(String address) {
        // 주요 지역명 추출 (시/구 단위)
        String[] parts = address.split(" ");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        }
        return parts.length > 0 ? parts[0] : "";
    }

    private String extractRegion(String address) {
        // 주소에서 지역 추출
        if (address.contains("서울")) return "서울";
        if (address.contains("부산")) return "부산";
        if (address.contains("대구")) return "대구";
        if (address.contains("인천")) return "인천";
        if (address.contains("광주")) return "광주";
        if (address.contains("대전")) return "대전";
        if (address.contains("울산")) return "울산";
        if (address.contains("세종")) return "세종";
        if (address.contains("경기")) return "경기";
        if (address.contains("강원")) return "강원";
        if (address.contains("충북") || address.contains("충청북")) return "충북";
        if (address.contains("충남") || address.contains("충청남")) return "충남";
        if (address.contains("전북") || address.contains("전라북")) return "전북";
        if (address.contains("전남") || address.contains("전라남")) return "전남";
        if (address.contains("경북") || address.contains("경상북")) return "경북";
        if (address.contains("경남") || address.contains("경상남")) return "경남";
        if (address.contains("제주")) return "제주";
        return null;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 하버사인 공식으로 거리 계산 (km)
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> baseStats = super.getStatistics();

        long totalCandidates = travelCandidateRepository.count();
        long withCategory = travelCandidateRepository.countByCategoryIsNotNull();
        long withPhone = travelCandidateRepository.countByPhoneNumberIsNotNull();
        long withAddress = travelCandidateRepository.countByAddressIsNotNull();

        baseStats.put("categoryCompletionRate", (double) withCategory / totalCandidates * 100);
        baseStats.put("phoneCompletionRate", (double) withPhone / totalCandidates * 100);
        baseStats.put("addressCompletionRate", (double) withAddress / totalCandidates * 100);

        return baseStats;
    }
}