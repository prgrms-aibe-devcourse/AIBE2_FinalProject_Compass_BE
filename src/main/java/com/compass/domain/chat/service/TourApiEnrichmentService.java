package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourApiEnrichmentService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${tour.api.key:}")
    private String tourApiKey;

    private static final String TOUR_BASE_URL = "http://apis.data.go.kr/B551011/KorService1";
    private static final String SEARCH_KEYWORD_PATH = "/searchKeyword1";
    private static final String LOCATION_BASED_PATH = "/locationBasedList1";
    private static final String DETAIL_COMMON_PATH = "/detailCommon1";
    private static final String DETAIL_INTRO_PATH = "/detailIntro1";

    // 전체 Tour API 보강
    @Transactional
    public int enrichAllWithTourApi() {
        log.info("Tour API를 통한 전체 데이터 보강 시작");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 주로 관광지 카테고리만 대상
        List<TravelCandidate> candidates = travelCandidateRepository.findAll().stream()
            .filter(c -> isEligibleForTourApi(c))
            .toList();

        log.info("Tour API 보강 대상: {} 개", candidates.size());

        candidates.forEach(candidate -> {
            try {
                boolean enriched = enrichWithTourData(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                    log.debug("Tour API 보강 성공: {}", candidate.getName());
                } else {
                    failCount.incrementAndGet();
                }

                // API 제한 (초당 5건)
                Thread.sleep(200);

            } catch (Exception e) {
                log.error("Tour API 보강 실패 - {}: {}", candidate.getName(), e.getMessage());
                failCount.incrementAndGet();
            }
        });

        log.info("Tour API 보강 완료 - 성공: {}, 실패: {}", successCount.get(), failCount.get());
        return successCount.get();
    }

    // 관광지만 보강
    @Transactional
    public int enrichTouristAttractions() {
        log.info("관광지 Tour API 보강 시작");

        AtomicInteger successCount = new AtomicInteger(0);

        List<TravelCandidate> attractions = travelCandidateRepository.findAll().stream()
            .filter(c -> c.getCategory() != null &&
                        (c.getCategory().contains("관광") ||
                         c.getCategory().contains("전시") ||
                         c.getCategory().contains("체험") ||
                         c.getCategory().contains("공원")))
            .toList();

        log.info("관광지 보강 대상: {} 개", attractions.size());

        attractions.forEach(candidate -> {
            try {
                boolean enriched = enrichWithTourData(candidate);
                if (enriched) {
                    candidate.setEnrichmentStatus(1);
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                }
                Thread.sleep(200);
            } catch (Exception e) {
                log.error("관광지 보강 실패: {}", e.getMessage());
            }
        });

        log.info("관광지 보강 완료: {} 개", successCount.get());
        return successCount.get();
    }

    // 개별 데이터 Tour API 보강
    private boolean enrichWithTourData(TravelCandidate candidate) {
        try {
            // 1. 키워드로 검색
            String contentId = searchByKeyword(candidate.getName(), candidate.getRegion());

            if (contentId == null && candidate.getLatitude() != null && candidate.getLongitude() != null) {
                // 2. 좌표로 검색 (키워드 검색 실패 시)
                contentId = searchByLocation(candidate.getLatitude(), candidate.getLongitude(), candidate.getName());
            }

            if (contentId != null) {
                // 3. 상세 정보 조회
                enrichWithDetailInfo(candidate, contentId);

                // 4. 소개 정보 조회
                enrichWithIntroInfo(candidate, contentId);

                log.debug("Tour API 상세 정보 보강 완료: {}", candidate.getName());
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("Tour API 보강 중 오류: {}", e.getMessage());
            return false;
        }
    }

    // 키워드로 콘텐츠 ID 검색
    private String searchByKeyword(String name, String region) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TOUR_BASE_URL + SEARCH_KEYWORD_PATH)
                .queryParam("serviceKey", tourApiKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "CompassTravel")
                .queryParam("_type", "json")
                .queryParam("keyword", name)
                .queryParam("numOfRows", "1")
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (items.isArray() && items.size() > 0) {
                JsonNode item = items.get(0);
                String title = item.path("title").asText("");

                // 이름 유사도 체크
                if (calculateSimilarity(title, name) > 0.6) {
                    return item.path("contentid").asText();
                }
            }

        } catch (Exception e) {
            log.error("Tour API 키워드 검색 실패: {}", e.getMessage());
        }

        return null;
    }

    // 좌표로 콘텐츠 ID 검색
    private String searchByLocation(Double latitude, Double longitude, String name) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TOUR_BASE_URL + LOCATION_BASED_PATH)
                .queryParam("serviceKey", tourApiKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "CompassTravel")
                .queryParam("_type", "json")
                .queryParam("mapX", longitude)
                .queryParam("mapY", latitude)
                .queryParam("radius", "500")  // 500m 반경
                .queryParam("numOfRows", "10")
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (items.isArray()) {
                // 가장 유사한 이름 찾기
                String bestMatch = null;
                double bestSimilarity = 0;

                for (JsonNode item : items) {
                    String title = item.path("title").asText("");
                    double similarity = calculateSimilarity(title, name);

                    if (similarity > bestSimilarity && similarity > 0.5) {
                        bestSimilarity = similarity;
                        bestMatch = item.path("contentid").asText();
                    }
                }

                return bestMatch;
            }

        } catch (Exception e) {
            log.error("Tour API 좌표 검색 실패: {}", e.getMessage());
        }

        return null;
    }

    // 상세 공통 정보 조회
    private void enrichWithDetailInfo(TravelCandidate candidate, String contentId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(TOUR_BASE_URL + DETAIL_COMMON_PATH)
                .queryParam("serviceKey", tourApiKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "CompassTravel")
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .queryParam("defaultYN", "Y")
                .queryParam("addrinfoYN", "Y")
                .queryParam("overviewYN", "Y")
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode item = root.path("response").path("body").path("items").path("item").get(0);
            if (item != null) {
                // 주소 정보
                String addr1 = item.path("addr1").asText("");
                String addr2 = item.path("addr2").asText("");
                if (!addr1.isEmpty()) {
                    String fullAddress = addr1 + (addr2.isEmpty() ? "" : " " + addr2);
                    if (candidate.getAddress() == null || candidate.getAddress().isEmpty()) {
                        candidate.setAddress(fullAddress);
                        candidate.setDetailedAddress(fullAddress);
                    }
                }

                // 우편번호
                String zipcode = item.path("zipcode").asText("");
                if (!zipcode.isEmpty()) {
                    candidate.setPostalCode(zipcode);
                }

                // 전화번호
                String tel = item.path("tel").asText("");
                if (!tel.isEmpty() && candidate.getPhoneNumber() == null) {
                    candidate.setPhoneNumber(tel);
                }

                // 홈페이지
                String homepage = item.path("homepage").asText("");
                if (!homepage.isEmpty() && candidate.getWebsite() == null) {
                    // HTML 태그 제거
                    homepage = homepage.replaceAll("<[^>]*>", "").trim();
                    candidate.setWebsite(homepage);
                }

                // 개요 (설명)
                String overview = item.path("overview").asText("");
                if (!overview.isEmpty()) {
                    // HTML 태그 제거 및 길이 제한
                    overview = overview.replaceAll("<[^>]*>", "").trim();
                    if (overview.length() > 500) {
                        overview = overview.substring(0, 497) + "...";
                    }
                    if (candidate.getDescription() == null || candidate.getDescription().isEmpty()) {
                        candidate.setDescription(overview);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Tour API 상세 정보 조회 실패: {}", e.getMessage());
        }
    }

    // 소개 정보 조회 (편의시설 등)
    private void enrichWithIntroInfo(TravelCandidate candidate, String contentId) {
        try {
            // 콘텐츠 타입 결정 (12: 관광지, 14: 문화시설, 15: 축제/행사, 39: 음식점)
            String contentTypeId = determineContentType(candidate.getCategory());

            String url = UriComponentsBuilder.fromHttpUrl(TOUR_BASE_URL + DETAIL_INTRO_PATH)
                .queryParam("serviceKey", tourApiKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "CompassTravel")
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", contentTypeId)
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode item = root.path("response").path("body").path("items").path("item").get(0);
            if (item != null) {
                // 공통 편의시설 정보
                parseCommonFacilities(candidate, item);

                // 콘텐츠 타입별 특수 정보
                parseContentSpecificInfo(candidate, item, contentTypeId);
            }

        } catch (Exception e) {
            log.error("Tour API 소개 정보 조회 실패: {}", e.getMessage());
        }
    }

    // 공통 편의시설 파싱
    private void parseCommonFacilities(TravelCandidate candidate, JsonNode item) {
        // 주차 가능 여부
        String parking = item.path("parking").asText("");
        if (parking.contains("가능") || parking.contains("있음")) {
            candidate.setParkingAvailable(true);
        } else if (parking.contains("불가") || parking.contains("없음")) {
            candidate.setParkingAvailable(false);
        }

        // 애완동물 동반
        String chkpet = item.path("chkpet").asText("");
        if (chkpet.contains("가능") || chkpet.contains("허용")) {
            candidate.setPetFriendly(true);
        } else if (chkpet.contains("불가") || chkpet.contains("금지")) {
            candidate.setPetFriendly(false);
        }

        // 휠체어
        String wheelchair = item.path("wheelchair").asText("");
        if (wheelchair.contains("가능") || wheelchair.contains("있음")) {
            candidate.setWheelchairAccessible(true);
        } else if (wheelchair.contains("불가") || wheelchair.contains("없음")) {
            candidate.setWheelchairAccessible(false);
        }

        // 이용시간
        String usetime = item.path("usetime").asText("");
        if (!usetime.isEmpty()) {
            candidate.setBusinessHours(usetime);
        }

        // 휴무일
        String restdate = item.path("restdate").asText("");
        if (!restdate.isEmpty()) {
            candidate.setRestDay(restdate);
        }

        // 이용요금
        String usefee = item.path("usefee").asText("");
        String chkcreditcard = item.path("chkcreditcard").asText("");
        if (!usefee.isEmpty()) {
            String fee = usefee;
            if (!chkcreditcard.isEmpty()) {
                fee += " (카드: " + chkcreditcard + ")";
            }
            candidate.setAdmissionFee(fee);
        }
    }

    // 콘텐츠별 특수 정보 파싱
    private void parseContentSpecificInfo(TravelCandidate candidate, JsonNode item, String contentTypeId) {
        switch (contentTypeId) {
            case "12":  // 관광지
                String expguide = item.path("expguide").asText("");
                if (!expguide.isEmpty() && candidate.getTips() == null) {
                    candidate.setTips(expguide);
                }
                break;

            case "14":  // 문화시설
                String scale = item.path("scale").asText("");
                String program = item.path("program").asText("");
                if (!scale.isEmpty() || !program.isEmpty()) {
                    String info = scale + (program.isEmpty() ? "" : "\n프로그램: " + program);
                    candidate.setHighlights(info);
                }
                break;

            case "39":  // 음식점
                String firstmenu = item.path("firstmenu").asText("");
                String treatmenu = item.path("treatmenu").asText("");
                if (!firstmenu.isEmpty() || !treatmenu.isEmpty()) {
                    String menuInfo = "대표메뉴: " + firstmenu +
                                     (treatmenu.isEmpty() ? "" : "\n취급메뉴: " + treatmenu);
                    candidate.setHighlights(menuInfo);
                }

                // 음식점은 와이파이 정보도 있을 수 있음
                String wifi = item.path("wifi").asText("");
                if (wifi.contains("가능") || wifi.contains("있음")) {
                    candidate.setWifiAvailable(true);
                }
                break;
        }
    }

    // 콘텐츠 타입 결정
    private String determineContentType(String category) {
        if (category == null) return "12";  // 기본값: 관광지

        if (category.contains("음식") || category.contains("식당") || category.contains("맛집")) {
            return "39";  // 음식점
        } else if (category.contains("문화") || category.contains("박물관") || category.contains("미술관")) {
            return "14";  // 문화시설
        } else if (category.contains("축제") || category.contains("행사") || category.contains("이벤트")) {
            return "15";  // 축제/행사
        } else if (category.contains("숙박") || category.contains("호텔") || category.contains("펜션")) {
            return "32";  // 숙박
        } else if (category.contains("쇼핑") || category.contains("시장")) {
            return "38";  // 쇼핑
        } else {
            return "12";  // 관광지
        }
    }

    // Tour API 적합 여부 판단
    private boolean isEligibleForTourApi(TravelCandidate candidate) {
        if (candidate.getCategory() == null) return false;

        String category = candidate.getCategory();
        return category.contains("관광") || category.contains("전시") ||
               category.contains("체험") || category.contains("공원") ||
               category.contains("문화") || category.contains("박물관") ||
               category.contains("미술관") || category.contains("축제");
    }

    // 문자열 유사도 계산 (Levenshtein Distance 기반)
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;

        s1 = s1.replaceAll("[^가-힣a-zA-Z0-9]", "");
        s2 = s2.replaceAll("[^가-힣a-zA-Z0-9]", "");

        if (s1.equals(s2)) return 1.0;
        if (s1.length() == 0 || s2.length() == 0) return 0;

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - (double) dp[s1.length()][s2.length()] / maxLen;
    }

    // 통계 정보
    public Map<String, Object> getTourApiStatistics() {
        long total = travelCandidateRepository.count();
        long enriched = travelCandidateRepository.findAll().stream()
            .filter(c -> c.getEnrichmentStatus() != null && c.getEnrichmentStatus() >= 1)
            .count();

        return Map.of(
            "total", total,
            "enriched", enriched,
            "enrichmentRate", (double) enriched / total * 100
        );
    }
}
