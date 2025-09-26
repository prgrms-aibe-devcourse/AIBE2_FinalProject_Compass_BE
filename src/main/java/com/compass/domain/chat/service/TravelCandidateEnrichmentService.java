package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 여행 후보지 데이터 보강 서비스
 * Tour API와 Perplexity를 활용하여 빈 컬럼 채우기
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TravelCandidateEnrichmentService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${tour.api.key:dummy-key}")
    private String tourApiKey;

    @Value("${perplexity.api.key:dummy-key}")
    private String perplexityApiKey;

    private static final String TOUR_API_BASE_URL = "http://apis.data.go.kr/B551011/KorService1";
    private static final String PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions";

    /**
     * 지역별 데이터 보강 실행
     *
     * @param region 대상 지역
     * @return 보강된 데이터 수
     */
    @Transactional
    public int enrichRegionData(String region) {
        log.info("=== {} 지역 데이터 보강 시작 ===", region);

        // enrichmentStatus가 0인 데이터 조회
        List<TravelCandidate> candidates = travelCandidateRepository
            .findByRegionOrderByQualityScore(region)
            .stream()
            .filter(tc -> tc.getEnrichmentStatus() == 0)
            .limit(50) // API 제한을 고려하여 배치 처리
            .toList();

        int enrichedCount = 0;

        for (TravelCandidate candidate : candidates) {
            try {
                // Step 1: Tour API로 데이터 보강
                boolean tourEnriched = enrichWithTourAPI(candidate);

                if (tourEnriched) {
                    candidate.setEnrichmentStatus(1);
                    enrichedCount++;
                }

                // Step 2: 아직 빈 컬럼이 있으면 Perplexity로 보강
                if (hasEmptyFields(candidate)) {
                    boolean perplexityEnriched = enrichWithPerplexity(candidate);

                    if (perplexityEnriched) {
                        candidate.setEnrichmentStatus(2);
                    }
                }

                // 저장
                travelCandidateRepository.save(candidate);
                log.debug("데이터 보강 완료: {} (상태: {})", candidate.getName(), candidate.getEnrichmentStatus());

                // API Rate Limit 고려
                Thread.sleep(500); // 0.5초 대기

            } catch (Exception e) {
                log.error("데이터 보강 실패 - {}: {}", candidate.getName(), e.getMessage());
            }
        }

        log.info("=== {} 지역 데이터 보강 완료: {}개 ===", region, enrichedCount);
        return enrichedCount;
    }

    /**
     * Tour API를 통한 데이터 보강
     */
    private boolean enrichWithTourAPI(TravelCandidate candidate) {
        try {
            // 키워드 검색 API 호출
            String url = String.format("%s/searchKeyword1?serviceKey=%s&MobileOS=ETC&MobileApp=Compass&keyword=%s&_type=json",
                TOUR_API_BASE_URL,
                tourApiKey,
                URLEncoder.encode(candidate.getName(), StandardCharsets.UTF_8)
            );

            String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.path("response").path("body").path("items").path("item");

                if (items.isArray() && items.size() > 0) {
                    JsonNode item = items.get(0);

                    // Tour API에서 추출한 정보로 필드 업데이트
                    updateFromTourAPI(candidate, item);

                    // 상세 정보 API 호출 (contentId가 있는 경우)
                    String contentId = item.path("contentid").asText();
                    if (!contentId.isEmpty()) {
                        enrichWithDetailInfo(candidate, contentId);
                    }

                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Tour API 호출 실패: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Tour API 상세 정보 조회
     */
    private void enrichWithDetailInfo(TravelCandidate candidate, String contentId) {
        try {
            // 공통정보 조회
            String url = String.format("%s/detailCommon1?serviceKey=%s&MobileOS=ETC&MobileApp=Compass&contentId=%s&defaultYN=Y&addrinfoYN=Y&_type=json",
                TOUR_API_BASE_URL,
                tourApiKey,
                contentId
            );

            String response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                JsonNode item = root.path("response").path("body").path("items").path("item").get(0);

                // 상세 정보 업데이트
                if (!item.path("zipcode").isMissingNode()) {
                    candidate.setPostalCode(item.path("zipcode").asText());
                }

                // 영업시간, 휴무일 정보
                if (!item.path("usetime").isMissingNode()) {
                    candidate.setBusinessHours(item.path("usetime").asText());
                }

                if (!item.path("restdate").isMissingNode()) {
                    candidate.setRestDay(item.path("restdate").asText());
                }

                // 편의시설 정보 추출
                extractFacilityInfo(candidate, item);
            }
        } catch (Exception e) {
            log.error("Tour API 상세정보 조회 실패: {}", e.getMessage());
        }
    }

    /**
     * Tour API 응답에서 정보 추출
     */
    private void updateFromTourAPI(TravelCandidate candidate, JsonNode item) {
        // 주소 정보
        if (!item.path("addr1").isMissingNode()) {
            candidate.setDetailedAddress(item.path("addr1").asText());
        }

        // 위경도 업데이트 (더 정확한 값으로)
        if (!item.path("mapy").isMissingNode() && !item.path("mapx").isMissingNode()) {
            candidate.setLatitude(item.path("mapy").asDouble());
            candidate.setLongitude(item.path("mapx").asDouble());
        }

        // 전화번호
        if (!item.path("tel").isMissingNode()) {
            String tel = item.path("tel").asText();
            if (candidate.getPhoneNumber() == null || candidate.getPhoneNumber().isEmpty()) {
                candidate.setPhoneNumber(tel);
            }
        }
    }

    /**
     * 편의시설 정보 추출
     */
    private void extractFacilityInfo(TravelCandidate candidate, JsonNode item) {
        // Tour API의 편의시설 정보는 텍스트로 제공되므로 키워드 분석
        String overview = item.path("overview").asText("");

        // 반려동물 관련
        if (overview.contains("반려동물") || overview.contains("애완동물") || overview.contains("펫")) {
            candidate.setPetFriendly(overview.contains("가능") || overview.contains("허용"));
        }

        // 주차 관련
        if (overview.contains("주차")) {
            candidate.setParkingAvailable(overview.contains("가능") || overview.contains("있") || overview.contains("무료"));
        }

        // 휠체어/장애인 관련
        if (overview.contains("휠체어") || overview.contains("장애인")) {
            candidate.setWheelchairAccessible(true);
        }

        // 와이파이 관련
        if (overview.contains("와이파이") || overview.contains("WiFi") || overview.contains("무선인터넷")) {
            candidate.setWifiAvailable(true);
        }
    }

    /**
     * Perplexity API를 통한 데이터 보강
     */
    private boolean enrichWithPerplexity(TravelCandidate candidate) {
        try {
            // 빈 필드 확인 및 질문 생성
            String query = buildPerplexityQuery(candidate);

            if (query.isEmpty()) {
                return false; // 질문할 내용이 없음
            }

            // Perplexity API 호출
            Map<String, Object> request = new HashMap<>();
            request.put("model", "llama-3.1-sonar-small-128k-online");
            request.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful Korean travel assistant. Answer in Korean."),
                Map.of("role", "user", "content", query)
            ));

            String response = webClient.post()
                .uri(PERPLEXITY_API_URL)
                .header("Authorization", "Bearer " + perplexityApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                String content = root.path("choices").get(0).path("message").path("content").asText();

                // 응답 파싱하여 필드 업데이트
                parsePerplexityResponse(candidate, content);
                return true;
            }
        } catch (Exception e) {
            log.error("Perplexity API 호출 실패: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Perplexity 질문 생성
     */
    private String buildPerplexityQuery(TravelCandidate candidate) {
        StringBuilder query = new StringBuilder();
        query.append(String.format("'%s' (%s, %s)에 대한 다음 정보를 알려주세요:\n",
            candidate.getName(), candidate.getRegion(), candidate.getCategory()));

        boolean hasQuestion = false;

        if (candidate.getPetFriendly() == null) {
            query.append("1. 반려동물 동반 가능 여부\n");
            hasQuestion = true;
        }

        if (candidate.getParkingAvailable() == null) {
            query.append("2. 주차 가능 여부\n");
            hasQuestion = true;
        }

        if (candidate.getRecommendedDuration() == null) {
            query.append("3. 추천 방문 시간 (예: 1-2시간)\n");
            hasQuestion = true;
        }

        if (candidate.getHighlights() == null) {
            query.append("4. 주요 특징과 볼거리 (간단히)\n");
            hasQuestion = true;
        }

        if (candidate.getTips() == null) {
            query.append("5. 방문 팁이나 주의사항\n");
            hasQuestion = true;
        }

        if (candidate.getNearbyAttractions() == null) {
            query.append("6. 근처 추천 명소 (2-3곳)\n");
            hasQuestion = true;
        }

        if (!hasQuestion) {
            return "";
        }

        query.append("\n각 항목을 간단명료하게 답변해주세요.");
        return query.toString();
    }

    /**
     * Perplexity 응답 파싱
     */
    private void parsePerplexityResponse(TravelCandidate candidate, String response) {
        // 응답을 줄 단위로 분석
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();

            // 반려동물
            if (line.contains("반려동물") || line.contains("애완동물")) {
                if (candidate.getPetFriendly() == null) {
                    candidate.setPetFriendly(
                        line.contains("가능") || line.contains("허용") || line.contains("동반 가능")
                    );
                }
            }

            // 주차
            if (line.contains("주차")) {
                if (candidate.getParkingAvailable() == null) {
                    candidate.setParkingAvailable(
                        line.contains("가능") || line.contains("있") || line.contains("무료")
                    );
                }
            }

            // 추천 방문 시간
            if (line.contains("시간") && line.contains("추천")) {
                if (candidate.getRecommendedDuration() == null) {
                    // 시간 정보 추출 (예: "1-2시간", "30분", "2시간")
                    String duration = extractDuration(line);
                    if (!duration.isEmpty()) {
                        candidate.setRecommendedDuration(duration);
                    }
                }
            }

            // 주요 특징
            if (line.contains("특징") || line.contains("볼거리")) {
                if (candidate.getHighlights() == null) {
                    candidate.setHighlights(extractAfterKeyword(line, new String[]{"특징", "볼거리"}));
                }
            }

            // 팁
            if (line.contains("팁") || line.contains("주의")) {
                if (candidate.getTips() == null) {
                    candidate.setTips(extractAfterKeyword(line, new String[]{"팁", "주의"}));
                }
            }

            // 근처 명소
            if (line.contains("근처") || line.contains("인근")) {
                if (candidate.getNearbyAttractions() == null) {
                    candidate.setNearbyAttractions(extractAfterKeyword(line, new String[]{"근처", "인근"}));
                }
            }
        }
    }

    /**
     * 시간 정보 추출
     */
    private String extractDuration(String text) {
        // 정규표현식으로 시간 패턴 추출
        if (text.matches(".*\\d+[-~]?\\d*\\s*시간.*")) {
            return text.replaceAll(".*?(\\d+[-~]?\\d*\\s*시간).*", "$1");
        } else if (text.matches(".*\\d+\\s*분.*")) {
            return text.replaceAll(".*?(\\d+\\s*분).*", "$1");
        }
        return "";
    }

    /**
     * 키워드 뒤의 텍스트 추출
     */
    private String extractAfterKeyword(String text, String[] keywords) {
        for (String keyword : keywords) {
            int index = text.indexOf(keyword);
            if (index != -1) {
                String result = text.substring(index + keyword.length()).trim();
                // 콜론이나 대시 제거
                if (result.startsWith(":") || result.startsWith("-")) {
                    result = result.substring(1).trim();
                }
                return result;
            }
        }
        return text; // 키워드를 찾지 못하면 전체 텍스트 반환
    }

    /**
     * 빈 필드 확인
     */
    private boolean hasEmptyFields(TravelCandidate candidate) {
        return candidate.getPetFriendly() == null ||
               candidate.getParkingAvailable() == null ||
               candidate.getWheelchairAccessible() == null ||
               candidate.getWifiAvailable() == null ||
               candidate.getRecommendedDuration() == null ||
               candidate.getHighlights() == null ||
               candidate.getTips() == null ||
               candidate.getNearbyAttractions() == null;
    }

    /**
     * 전체 지역 비동기 데이터 보강
     */
    @Async
    public CompletableFuture<Void> enrichAllRegionsAsync() {
        List<String> regions = List.of("서울", "부산", "인천", "대구", "대전",
                                       "광주", "울산", "제주", "경주", "강릉", "전주", "여수");

        log.info("=== 전체 지역 데이터 보강 시작 ===");

        for (String region : regions) {
            try {
                int enriched = enrichRegionData(region);
                log.info("{} 지역 보강 완료: {}개", region, enriched);

                // API 제한 고려
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("{} 지역 보강 실패: {}", region, e.getMessage());
            }
        }

        log.info("=== 전체 지역 데이터 보강 완료 ===");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 보강 상태별 통계 조회
     */
    public Map<String, Object> getEnrichmentStatistics(String region) {
        Map<String, Object> stats = new HashMap<>();

        List<TravelCandidate> candidates = travelCandidateRepository.findByRegionOrderByQualityScore(region);

        long total = candidates.size();
        long notEnriched = candidates.stream().filter(c -> c.getEnrichmentStatus() == 0).count();
        long tourEnriched = candidates.stream().filter(c -> c.getEnrichmentStatus() == 1).count();
        long fullyEnriched = candidates.stream().filter(c -> c.getEnrichmentStatus() == 2).count();

        stats.put("total", total);
        stats.put("notEnriched", notEnriched);
        stats.put("tourEnriched", tourEnriched);
        stats.put("fullyEnriched", fullyEnriched);
        stats.put("enrichmentRate", total > 0 ? (double)(tourEnriched + fullyEnriched) / total * 100 : 0);

        return stats;
    }
}