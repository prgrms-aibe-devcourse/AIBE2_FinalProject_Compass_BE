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
 * 한국관광공사 Tour API를 활용한 편의시설 정보 보강 서비스
 */
@Service
@Slf4j
public class TourApiEnrichmentServiceImpl extends AbstractEnrichmentService {

    private final RestTemplate restTemplate;
    private final ApiRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Value("${tour.api.key:}")
    private String tourApiKey;

    private static final String TOUR_SEARCH_URL = "http://apis.data.go.kr/B551011/KorService1/searchKeyword1";
    private static final String TOUR_DETAIL_URL = "http://apis.data.go.kr/B551011/KorService1/detailCommon1";
    private static final String TOUR_INTRO_URL = "http://apis.data.go.kr/B551011/KorService1/detailIntro1";

    public TourApiEnrichmentServiceImpl(
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
        return "TourApiEnrichment";
    }

    @Override
    public int getPriority() {
        return 3; // Google, Kakao 다음 우선순위
    }

    @Override
    public int getRateLimitDelay() {
        return 200; // 5 QPS = 200ms delay
    }

    @Override
    public boolean isEligible(TravelCandidate candidate) {
        // 관광지 카테고리인 경우만 (Tour API는 관광지 특화)
        String category = candidate.getCategory();
        return category != null && (
            category.contains("관광") ||
            category.contains("명소") ||
            category.contains("박물관") ||
            category.contains("공원") ||
            category.contains("테마") ||
            category.contains("해변") ||
            category.contains("산") ||
            category.contains("문화") ||
            category.contains("역사")
        );
    }

    @Override
    public boolean enrichSingle(TravelCandidate candidate) {
        try {
            rateLimiter.acquire("tour_api");

            // 1. 키워드로 관광지 검색
            String contentId = searchTourPlace(candidate.getName(), candidate.getAddress());
            if (contentId == null) {
                log.debug("Tour API에서 관광지를 찾을 수 없음: {}", candidate.getName());
                return false;
            }

            // 2. 상세 정보 조회
            JsonNode detailInfo = getTourDetail(contentId);
            JsonNode introInfo = getTourIntro(contentId);

            boolean updated = false;

            // 공통 정보 처리
            if (detailInfo != null) {
                // 주차 가능 여부
                if (detailInfo.has("parking")) {
                    String parking = detailInfo.get("parking").asText();
                    candidate.setParkingAvailable(
                        EnrichmentUtils.parseBoolean(parking)
                    );
                    updated = true;
                }

                // 홈페이지
                if (detailInfo.has("homepage") && candidate.getWebsite() == null) {
                    String homepage = EnrichmentUtils.removeHtmlTags(
                        detailInfo.get("homepage").asText()
                    );
                    if (homepage != null && !homepage.isEmpty()) {
                        candidate.setWebsite(homepage);
                        updated = true;
                    }
                }
            }

            // 상세 소개 정보 처리
            if (introInfo != null) {
                // 반려동물 동반 가능
                if (introInfo.has("chkpetleports")) {
                    String petInfo = introInfo.get("chkpetleports").asText();
                    candidate.setPetFriendly(
                        EnrichmentUtils.parseBoolean(petInfo)
                    );
                    updated = true;
                }

                // 휠체어 접근성
                if (introInfo.has("wheelchair")) {
                    String wheelchair = introInfo.get("wheelchair").asText();
                    candidate.setWheelchairAccessible(
                        EnrichmentUtils.parseBoolean(wheelchair)
                    );
                    updated = true;
                }

                // 와이파이
                if (introInfo.has("wifi")) {
                    String wifi = introInfo.get("wifi").asText();
                    candidate.setWifiAvailable(
                        EnrichmentUtils.parseBoolean(wifi)
                    );
                    updated = true;
                }

                // 휴무일
                if (introInfo.has("restdate") && candidate.getClosedDays() == null) {
                    String restDate = introInfo.get("restdate").asText();
                    if (!restDate.isEmpty() && !restDate.equals("연중무휴")) {
                        candidate.setClosedDays(
                            EnrichmentUtils.truncateString(restDate, 100)
                        );
                        updated = true;
                    }
                }

                // 이용시간 (영업시간 보완)
                if (introInfo.has("usetime") && candidate.getBusinessHours() == null) {
                    String useTime = EnrichmentUtils.removeHtmlTags(
                        introInfo.get("usetime").asText()
                    );
                    if (useTime != null && !useTime.isEmpty()) {
                        candidate.setBusinessHours(
                            EnrichmentUtils.truncateString(useTime, 500)
                        );
                        updated = true;
                    }
                }

                // 입장료/이용료
                if (introInfo.has("usefee")) {
                    String fee = EnrichmentUtils.removeHtmlTags(
                        introInfo.get("usefee").asText()
                    );
                    if (fee != null && !fee.isEmpty() && !fee.contains("무료")) {
                        candidate.setAdmissionFee(
                            EnrichmentUtils.truncateString(fee, 200)
                        );
                        updated = true;
                    }
                }

                // 수용인원
                if (introInfo.has("accomcount")) {
                    String capacity = introInfo.get("accomcount").asText();
                    try {
                        int cap = Integer.parseInt(capacity.replaceAll("[^0-9]", ""));
                        candidate.setCapacity(cap);
                        updated = true;
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Tour API Content ID 저장
            candidate.setTourApiContentId(contentId);

            // 업데이트 시간 기록
            if (updated) {
                candidate.setUpdatedAt(LocalDateTime.now());
            }

            return updated;

        } catch (Exception e) {
            rateLimiter.handleBackoff("tour_api", e);
            log.error("Tour API 보강 실패: {} - {}", candidate.getName(), e.getMessage());
            return false;
        }
    }

    private String searchTourPlace(String name, String address) {
        try {
            String url = UriComponentsBuilder.fromUriString(TOUR_SEARCH_URL)
                .queryParam("serviceKey", tourApiKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "CompassApp")
                .queryParam("_type", "json")
                .queryParam("keyword", name)
                .queryParam("numOfRows", 5)
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (items.isArray() && items.size() > 0) {
                // 가장 유사한 결과 찾기
                JsonNode bestMatch = null;
                double bestSimilarity = 0;

                for (JsonNode item : items) {
                    if (item.has("title")) {
                        String title = EnrichmentUtils.removeHtmlTags(item.get("title").asText());
                        double similarity = EnrichmentUtils.calculateSimilarity(name, title);

                        // 주소도 고려
                        if (address != null && item.has("addr1")) {
                            String itemAddr = item.get("addr1").asText();
                            if (itemAddr.contains(address.substring(0, Math.min(address.length(), 5)))) {
                                similarity += 0.2;
                            }
                        }

                        if (similarity > bestSimilarity) {
                            bestSimilarity = similarity;
                            bestMatch = item;
                        }
                    }
                }

                // 유사도가 0.5 이상인 경우만 반환
                if (bestSimilarity > 0.5 && bestMatch != null && bestMatch.has("contentid")) {
                    return bestMatch.get("contentid").asText();
                }
            }

        } catch (Exception e) {
            log.error("Tour API 검색 실패: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode getTourDetail(String contentId) {
        try {
            String url = UriComponentsBuilder.fromUriString(TOUR_DETAIL_URL)
                .queryParam("serviceKey", tourApiKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "CompassApp")
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .queryParam("defaultYN", "Y")
                .queryParam("addrinfoYN", "Y")
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode item = root.path("response").path("body").path("items").path("item");

            if (item.isArray() && item.size() > 0) {
                return item.get(0);
            } else if (!item.isMissingNode()) {
                return item;
            }

        } catch (Exception e) {
            log.error("Tour API 상세정보 조회 실패: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode getTourIntro(String contentId) {
        try {
            // contentTypeId는 12(관광지)로 가정, 실제로는 동적으로 결정 필요
            String url = UriComponentsBuilder.fromUriString(TOUR_INTRO_URL)
                .queryParam("serviceKey", tourApiKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "CompassApp")
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", "12")
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode item = root.path("response").path("body").path("items").path("item");

            if (item.isArray() && item.size() > 0) {
                return item.get(0);
            } else if (!item.isMissingNode()) {
                return item;
            }

        } catch (Exception e) {
            log.error("Tour API 소개정보 조회 실패: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> baseStats = super.getStatistics();

        long totalCandidates = travelCandidateRepository.count();
        long touristAttractions = travelCandidateRepository.countByCategoryContaining("관광");
        long withParking = travelCandidateRepository.countByParkingAvailableIsNotNull();
        long withFacilities = travelCandidateRepository.countByWifiAvailableIsNotNullOrPetFriendlyIsNotNull();

        baseStats.put("touristAttractionCount", touristAttractions);
        baseStats.put("parkingInfoRate", (double) withParking / touristAttractions * 100);
        baseStats.put("facilitiesInfoRate", (double) withFacilities / touristAttractions * 100);

        return baseStats;
    }
}