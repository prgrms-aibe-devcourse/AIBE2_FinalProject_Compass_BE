package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.service.enrichment.EnrichmentUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class GooglePlacesEnrichmentService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;

    public GooglePlacesEnrichmentService(
        TravelCandidateRepository travelCandidateRepository,
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        @Autowired(required = false) @Qualifier("vertexAiGeminiChat") ChatModel chatModel
    ) {
        this.travelCandidateRepository = travelCandidateRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
    }

    @Value("${google.places.api.key:}")
    private String googleApiKey;

    private static final String PLACE_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json";
    private static final String PLACE_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";
    private static final String PLACE_PHOTO_URL = "https://maps.googleapis.com/maps/api/place/photo";

    // 전체 TravelCandidate Google Places 정보 보강
    @Transactional
    public int enrichAllWithGooglePlaces() {
        log.info("Google Places API를 통한 전체 데이터 보강 시작");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<TravelCandidate> candidates = travelCandidateRepository.findAll();
        log.info("보강 대상 전체 수: {}", candidates.size());

        candidates.forEach(candidate -> {
            try {
                boolean enriched = enrichSingleCandidate(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                    log.debug("Google Places 보강 성공: {}", candidate.getName());
                } else {
                    failCount.incrementAndGet();
                }

                // API 할당량 제한 (QPS: 10)
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Google Places 보강 실패 - {}: {}", candidate.getName(), e.getMessage());
                failCount.incrementAndGet();
            }
        });

        log.info("Google Places 보강 완료 - 성공: {}, 실패: {}", successCount.get(), failCount.get());
        return successCount.get();
    }

    // 페이지 단위 보강 (대량 처리용)
    @Transactional
    public int enrichByPage(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<TravelCandidate> page = travelCandidateRepository.findAll(pageable);

        AtomicInteger successCount = new AtomicInteger(0);

        page.getContent().forEach(candidate -> {
            try {
                boolean enriched = enrichSingleCandidate(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                }
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("페이지 보강 실패: {}", e.getMessage());
            }
        });

        log.info("페이지 {} 보강 완료 - 성공: {}/{}", pageNumber, successCount.get(), page.getContent().size());
        return successCount.get();
    }

    // 비동기 배치 보강
    @Async
    @Transactional
    public CompletableFuture<Integer> enrichBatchAsync(List<Long> candidateIds) {
        log.info("비동기 Google Places 보강 시작 - {} 개", candidateIds.size());

        AtomicInteger successCount = new AtomicInteger(0);
        List<TravelCandidate> candidates = travelCandidateRepository.findAllById(candidateIds);

        candidates.forEach(candidate -> {
            try {
                boolean enriched = enrichSingleCandidate(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                }
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("비동기 보강 실패: {}", e.getMessage());
            }
        });

        return CompletableFuture.completedFuture(successCount.get());
    }

    // Google Places로 채울 수 있는 빈 필드 우선 보강
    @Transactional
    public int enrichMissingGoogleDetails() {
        List<TravelCandidate> candidates = travelCandidateRepository.findCandidatesNeedingEnrichment();
        if (candidates.isEmpty()) {
            log.info("Google Places로 채울 빈 필드가 없습니다.");
            return 0;
        }

        log.info("Google Places 빈 필드 보강 대상: {}개", candidates.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        for (TravelCandidate candidate : candidates) {
            if (!hasMissingFields(candidate)) {
                skipCount.incrementAndGet();
                continue;
            }

            try {
                boolean enriched = enrichSingleCandidate(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                } else {
                    skipCount.incrementAndGet();
                }

                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Google Places 빈 필드 보강 실패 - {}: {}", candidate.getName(), e.getMessage());
                skipCount.incrementAndGet();
            }
        }

        log.info("Google Places 빈 필드 보강 완료 - 성공: {}, 건너뜀: {}",
            successCount.get(), skipCount.get());

        return successCount.get();
    }

    // 개별 후보 보강
    private boolean enrichSingleCandidate(TravelCandidate candidate) {
        try {
            // Step 1: 기존 Google Place ID 확인 또는 검색
            String placeId = candidate.getGooglePlaceId();
            boolean lookedUpPlaceId = false;

            if (placeId == null || placeId.isBlank()) {
                placeId = candidate.getPlaceId();
            }

            if (placeId == null || placeId.isBlank()) {
                placeId = findPlaceId(candidate.getName(), candidate.getRegion());
                lookedUpPlaceId = placeId != null;
            }

            if (placeId == null || placeId.isBlank()) {
                log.warn("Place ID를 찾을 수 없음: {}", candidate.getName());
                return false;
            }

            if (!placeId.equals(candidate.getGooglePlaceId())) {
                candidate.setGooglePlaceId(placeId);
            }

            if (lookedUpPlaceId || candidate.getPlaceId() == null || candidate.getPlaceId().isBlank()) {
                candidate.setPlaceId(placeId);
            }

            boolean updated = false;

            // Step 2: Place Details로 상세 정보 가져오기
            Map<String, Object> details = getPlaceDetails(placeId);

            if (details != null) {
                // 좌표 정보
                if (details.containsKey("geometry")) {
                    Map<String, Object> geometry = (Map<String, Object>) details.get("geometry");
                    Map<String, Object> location = (Map<String, Object>) geometry.get("location");

                    candidate.setLatitude((Double) location.get("lat"));
                    candidate.setLongitude((Double) location.get("lng"));
                    updated = true;
                }

                // 평점 및 리뷰
                if (details.containsKey("rating")) {
                    candidate.setRating(((Number) details.get("rating")).doubleValue());
                    updated = true;
                }
                if (details.containsKey("user_ratings_total")) {
                    candidate.setReviewCount(((Number) details.get("user_ratings_total")).intValue());
                    updated = true;
                }

                // 가격대
                if (details.containsKey("price_level")) {
                    candidate.setPriceLevel(((Number) details.get("price_level")).intValue());
                    updated = true;
                }

                // 연락처 정보
                if (details.containsKey("formatted_phone_number")) {
                    candidate.setPhoneNumber((String) details.get("formatted_phone_number"));
                    updated = true;
                }
                if (details.containsKey("website")) {
                    candidate.setWebsite((String) details.get("website"));
                    updated = true;
                }

                // 주소 (Google 형식)
                if (details.containsKey("formatted_address")) {
                    String googleAddress = (String) details.get("formatted_address");
                    if (candidate.getAddress() == null || candidate.getAddress().length() < 20) {
                        candidate.setAddress(googleAddress);
                        updated = true;
                    }
                }

                // 사진 URL
                if (details.containsKey("photos")) {
                    List<Map<String, Object>> photos = (List<Map<String, Object>>) details.get("photos");
                    if (!photos.isEmpty()) {
                        String photoReference = (String) photos.get(0).get("photo_reference");
                        String photoUrl = buildPhotoUrl(photoReference);
                        candidate.setPhotoUrl(photoUrl);
                        updated = true;
                    }
                }

                // 영업 정보
                if (details.containsKey("opening_hours")) {
                    Map<String, Object> openingHours = (Map<String, Object>) details.get("opening_hours");
                    if (openingHours.containsKey("open_now")) {
                        candidate.setOpenNow((Boolean) openingHours.get("open_now"));
                        updated = true;
                    }
                    if (openingHours.containsKey("weekday_text")) {
                        List<String> weekdayText = (List<String>) openingHours.get("weekday_text");
                        candidate.setBusinessHours(String.join("\n", weekdayText));
                        updated = true;
                    }
                }

                // Google Types
                if (details.containsKey("types")) {
                    List<String> types = (List<String>) details.get("types");
                    candidate.setGoogleTypes(String.join(",", types));
                    updated = true;
                }
            }

            String description = generateDescriptionWithGemini(candidate);
            if (description != null && !description.isBlank() && !description.equals(candidate.getDescription())) {
                candidate.setDescription(description);
                candidate.setAiEnriched(Boolean.TRUE);
                updated = true;
            }

            if (updated) {
                candidate.calculateScores();
                log.debug("Google Places 보강 완료: {}", candidate.getName());
            }

            return updated;

        } catch (Exception e) {
            log.error("Google Places 보강 중 오류 - {}: {}", candidate.getName(), e.getMessage());
            return false;
        }
    }

    private boolean hasMissingFields(TravelCandidate candidate) {
        return isBlank(candidate.getAddress()) ||
            isBlank(candidate.getPhoneNumber()) ||
            isBlank(candidate.getWebsite()) ||
            isBlank(candidate.getPhotoUrl()) ||
            isBlank(candidate.getBusinessHours()) ||
            isBlank(candidate.getGoogleTypes()) ||
            candidate.getRating() == null ||
            candidate.getReviewCount() == null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String generateDescriptionWithGemini(TravelCandidate candidate) {
        if (chatModel == null) {
            log.debug("ChatModel 미등록 - Gemini 설명 보강 불가");
            return null;
        }

        try {
            String systemPrompt = "당신은 한국 여행 전문 큐레이터입니다. 여행자를 위해 간결하지만 매력적인 소개를 제공합니다.";
            String userPrompt = String.format("""
                다음 장소에 대한 3문장 이내의 한국어 소개를 작성하세요.
                - 장소명: %s
                - 지역: %s
                - 카테고리: %s
                - 주소: %s
                설명에는 방문할 만한 이유 두 가지를 포함하고, 과장된 표현은 피하세요.
                """,
                safeValue(candidate.getName()),
                safeValue(candidate.getRegion()),
                safeValue(candidate.getCategory()),
                safeValue(candidate.getAddress())
            );

            var prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            ));

            var response = chatModel.call(prompt);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return null;
            }

            String content = response.getResult().getOutput().getContent();
            if (content == null) {
                return null;
            }

            content = content.trim();
            if (content.isEmpty()) {
                return null;
            }

            content = content.replaceAll("^[\"'`]+|[\"'`]+$", "").trim();

            return EnrichmentUtils.truncateString(content, 600);

        } catch (Exception e) {
            log.error("Gemini 설명 생성 실패 - {}: {}", candidate.getName(), e.getMessage());
            return null;
        }
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "정보 없음" : value;
    }

    @Transactional
    public int refreshRatingsAndReviews() {
        log.info("Google Place ID 기반 핵심 지표(위도/경도, 평점, 리뷰수) 갱신 시작");

        List<TravelCandidate> candidates = travelCandidateRepository.findByGooglePlaceIdIsNotNullAndIsActiveTrue();
        log.info("갱신 대상: {}개", candidates.size());

        AtomicInteger updatedCount = new AtomicInteger(0);
        AtomicInteger unchangedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        for (TravelCandidate candidate : candidates) {
            String placeId = candidate.getGooglePlaceId();
            if (placeId == null || placeId.isBlank()) {
                continue;
            }

            try {
                Map<String, Object> details = getPlaceDetails(placeId);
                if (details == null || details.isEmpty()) {
                    log.debug("세부 정보를 가져오지 못했습니다: {} (placeId={})", candidate.getName(), placeId);
                    failedCount.incrementAndGet();
                    continue;
                }

                boolean updated = updateCoreMetrics(candidate, details);

                if (updated) {
                    candidate.calculateScores();
                    travelCandidateRepository.save(candidate);
                    updatedCount.incrementAndGet();
                } else {
                    unchangedCount.incrementAndGet();
                }

            } catch (Exception e) {
                log.error("Google Places 평점/리뷰 갱신 실패 - {}: {}", candidate.getName(), e.getMessage());
                failedCount.incrementAndGet();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Google Place 평점 갱신이 인터럽트되어 중단되었습니다");
                break;
            }
        }

        log.info("Google Place ID 기반 핵심 지표 갱신 완료 - 성공: {}, 변화 없음: {}, 실패: {}",
            updatedCount.get(), unchangedCount.get(), failedCount.get());

        return updatedCount.get();
    }

    private boolean updateCoreMetrics(TravelCandidate candidate, Map<String, Object> details) {
        boolean updated = false;

        Object ratingValue = details.get("rating");
        if (ratingValue instanceof Number ratingNumber) {
            double newRating = ratingNumber.doubleValue();
            if (!Objects.equals(candidate.getRating(), newRating)) {
                candidate.setRating(newRating);
                updated = true;
            }
        }

        Object reviewValue = details.get("user_ratings_total");
        if (reviewValue instanceof Number reviewNumber) {
            int newReviewCount = reviewNumber.intValue();
            if (!Objects.equals(candidate.getReviewCount(), newReviewCount)) {
                candidate.setReviewCount(newReviewCount);
                updated = true;
            }
        }

        Object geometryObj = details.get("geometry");
        if (geometryObj instanceof Map<?, ?> geometryMap) {
            Object locationObj = geometryMap.get("location");
            if (locationObj instanceof Map<?, ?> locationMap) {
                Object latObj = locationMap.get("lat");
                Object lngObj = locationMap.get("lng");

                Double newLat = latObj instanceof Number ? ((Number) latObj).doubleValue() : null;
                Double newLng = lngObj instanceof Number ? ((Number) lngObj).doubleValue() : null;

                if (newLat != null && !Objects.equals(candidate.getLatitude(), newLat)) {
                    candidate.setLatitude(newLat);
                    updated = true;
                }
                if (newLng != null && !Objects.equals(candidate.getLongitude(), newLng)) {
                    candidate.setLongitude(newLng);
                    updated = true;
                }
            }
        }

        return updated;
    }

    // Place ID 검색
    private String findPlaceId(String name, String region) {
        try {
            String query = region != null ? region + " " + name : name;
            String url = String.format("%s?input=%s&inputtype=textquery&fields=place_id&language=ko&key=%s",
                PLACE_SEARCH_URL,
                URLEncoder.encode(query, StandardCharsets.UTF_8),
                googleApiKey
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("candidates") && root.get("candidates").size() > 0) {
                return root.get("candidates").get(0).get("place_id").asText();
            }

        } catch (Exception e) {
            log.error("Place ID 검색 실패: {}", e.getMessage());
        }

        return null;
    }

    // Place Details 조회
    private Map<String, Object> getPlaceDetails(String placeId) {
        try {
            String fields = "place_id,name,rating,user_ratings_total,price_level,formatted_phone_number," +
                           "website,formatted_address,geometry,photos,opening_hours,types";

            String url = String.format("%s?place_id=%s&fields=%s&language=ko&key=%s",
                PLACE_DETAILS_URL,
                placeId,
                fields,
                googleApiKey
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (root.has("result")) {
                return objectMapper.convertValue(root.get("result"), Map.class);
            }

        } catch (Exception e) {
            log.error("Place Details 조회 실패: {}", e.getMessage());
        }

        return null;
    }

    // 사진 URL 생성
    private String buildPhotoUrl(String photoReference) {
        return String.format("%s?maxwidth=800&photoreference=%s&key=%s",
            PLACE_PHOTO_URL,
            photoReference,
            googleApiKey
        );
    }

    // 통계 정보
    public Map<String, Object> getEnrichmentStatistics() {
        long total = travelCandidateRepository.count();
        long withPlaceId = travelCandidateRepository.count(); // placeId가 있는 개수 쿼리 추가 필요
        long withRating = travelCandidateRepository.count(); // rating이 있는 개수 쿼리 추가 필요
        long withPhoto = travelCandidateRepository.count(); // photo가 있는 개수 쿼리 추가 필요

        return Map.of(
            "total", total,
            "withPlaceId", withPlaceId,
            "withRating", withRating,
            "withPhoto", withPhoto,
            "completionRate", (double) withPlaceId / total * 100
        );
    }
}
