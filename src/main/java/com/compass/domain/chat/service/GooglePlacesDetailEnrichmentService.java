package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesDetailEnrichmentService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.places.api.key:dummy-key}")
    private String googlePlacesApiKey;

    @Value("${perplexity.api.key:dummy-key}")
    private String perplexityApiKey;

    private static final String GOOGLE_PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";
    private static final String PERPLEXITY_API_URL = "https://api.perplexity.ai/chat/completions";

    @Transactional
    public void enrichCandidatesWithAdditionalInfo() {
        List<TravelCandidate> candidates = travelCandidateRepository.findAll();
        log.info("Starting Google Places enrichment for {} candidates", candidates.size());

        int processed = 0;
        int enrichedWithGoogle = 0;
        int enrichedWithPerplexity = 0;

        for (TravelCandidate candidate : candidates) {
            try {
                boolean googleEnriched = false;
                boolean perplexityEnriched = false;

                // Google Places API로 상세 정보 가져오기
                if (candidate.getGooglePlaceId() != null) {
                    googleEnriched = enrichWithGooglePlacesDetails(candidate);
                    if (googleEnriched) {
                        enrichedWithGoogle++;
                    }
                }

                // 반려동물 동반 정보나 입장료 정보가 없으면 Perplexity로 보완
                if (candidate.getPetFriendly() == null || candidate.getAdmissionFee() == null) {
                    perplexityEnriched = enrichWithPerplexityDetails(candidate);
                    if (perplexityEnriched) {
                        enrichedWithPerplexity++;
                    }
                }

                // 변경사항이 있으면 저장
                if (googleEnriched || perplexityEnriched) {
                    candidate.setAiEnriched(true);
                    travelCandidateRepository.save(candidate);
                }

                processed++;
                if (processed % 10 == 0) {
                    log.info("Progress: {}/{} processed, {} Google enriched, {} Perplexity enriched",
                        processed, candidates.size(), enrichedWithGoogle, enrichedWithPerplexity);
                }

                // API 호출 제한을 위한 딜레이
                TimeUnit.MILLISECONDS.sleep(200);

            } catch (Exception e) {
                log.error("Error enriching candidate {}: {}", candidate.getName(), e.getMessage());
            }
        }

        log.info("Enrichment completed: Google={}, Perplexity={} out of {} candidates",
            enrichedWithGoogle, enrichedWithPerplexity, candidates.size());
    }

    private boolean enrichWithGooglePlacesDetails(TravelCandidate candidate) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_PLACES_DETAILS_URL)
                .queryParam("place_id", candidate.getGooglePlaceId())
                .queryParam("fields", "opening_hours,price_level,wheelchair_accessible_entrance,types,editorial_summary,current_opening_hours,business_status")
                .queryParam("key", googlePlacesApiKey)
                .queryParam("language", "ko")
                .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode result = root.path("result");

            boolean updated = false;

            if (!result.isMissingNode()) {
                // 영업 시간 정보 (현재 영업 시간 우선)
                JsonNode currentOpeningHours = result.path("current_opening_hours");
                if (!currentOpeningHours.isMissingNode()) {
                    JsonNode weekdayText = currentOpeningHours.path("weekday_text");
                    if (weekdayText.isArray() && weekdayText.size() > 0) {
                        StringBuilder hours = new StringBuilder();
                        for (JsonNode day : weekdayText) {
                            hours.append(day.asText()).append("\n");
                        }
                        candidate.setBusinessHours(hours.toString().trim());
                        updated = true;
                    }
                } else {
                    // current_opening_hours가 없으면 opening_hours 사용
                    JsonNode openingHours = result.path("opening_hours");
                    if (!openingHours.isMissingNode()) {
                        JsonNode weekdayText = openingHours.path("weekday_text");
                        if (weekdayText.isArray() && weekdayText.size() > 0) {
                            StringBuilder hours = new StringBuilder();
                            for (JsonNode day : weekdayText) {
                                hours.append(day.asText()).append("\n");
                            }
                            candidate.setBusinessHours(hours.toString().trim());
                            updated = true;
                        }
                    }
                }

                // 휠체어 접근성
                JsonNode wheelchair = result.path("wheelchair_accessible_entrance");
                if (!wheelchair.isMissingNode()) {
                    candidate.setWheelchairAccessible(wheelchair.asBoolean());
                    updated = true;
                }

                // 비즈니스 상태 확인
                JsonNode businessStatus = result.path("business_status");
                if (!businessStatus.isMissingNode()) {
                    String status = businessStatus.asText();
                    // OPERATIONAL, CLOSED_TEMPORARILY, CLOSED_PERMANENTLY
                    if ("CLOSED_PERMANENTLY".equals(status)) {
                        candidate.setIsActive(false);
                        log.warn("Place {} is permanently closed", candidate.getName());
                    }
                }

                // Google Places types를 통한 반려동물 동반 추론
                JsonNode types = result.path("types");
                if (types.isArray() && candidate.getPetFriendly() == null) {
                    for (JsonNode type : types) {
                        String typeValue = type.asText();
                        // 야외 장소는 대체로 반려동물 허용
                        if (typeValue.equals("park") || typeValue.equals("beach") ||
                            typeValue.equals("hiking_area") || typeValue.equals("campground") ||
                            typeValue.equals("natural_feature")) {
                            candidate.setPetFriendly(true);
                            updated = true;
                            break;
                        }
                        // 실내 문화시설은 대체로 반려동물 불가
                        else if (typeValue.equals("museum") || typeValue.equals("art_gallery") ||
                                 typeValue.equals("library") || typeValue.equals("movie_theater") ||
                                 typeValue.equals("church") || typeValue.equals("mosque") ||
                                 typeValue.equals("synagogue") || typeValue.equals("hindu_temple") ||
                                 typeValue.equals("buddhist_temple")) {
                            candidate.setPetFriendly(false);
                            updated = true;
                            break;
                        }
                        // 음식점은 테라스가 있는 경우가 아니면 대체로 불가
                        else if (typeValue.equals("restaurant") || typeValue.equals("cafe")) {
                            candidate.setPetFriendly(false);
                            updated = true;
                            break;
                        }
                    }
                }

                log.debug("Enriched {} with Google Places data", candidate.getName());
            }

            return updated;

        } catch (Exception e) {
            log.error("Google Places API error for {}: {}", candidate.getName(), e.getMessage());
            return false;
        }
    }

    private boolean enrichWithPerplexityDetails(TravelCandidate candidate) {
        try {
            String query = String.format(
                "%s %s에 대해 다음 정보를 JSON 형식으로 알려주세요: " +
                "1) 반려동물 동반 가능 여부 (pet_friendly: true/false/null) " +
                "2) 입장료 (admission_fee: '무료'/'성인 5000원' 등 한국어로, 없으면 null) " +
                "3) 영업시간이 있다면 (hours: '09:00-18:00' 형식 또는 null) " +
                "응답은 반드시 JSON 형식 {pet_friendly:, admission_fee:, hours:}로 해주세요.",
                candidate.getName(),
                candidate.getAddress() != null ? candidate.getAddress() : candidate.getRegion()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + perplexityApiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.1-sonar-small-128k-online");
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 200);
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful Korean travel information assistant. Always respond with valid JSON format."),
                Map.of("role", "user", "content", query)
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                PERPLEXITY_API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode content = root.path("choices").path(0).path("message").path("content");

                if (!content.isMissingNode()) {
                    String contentText = content.asText();

                    // JSON 부분 추출
                    int startIdx = contentText.indexOf("{");
                    int endIdx = contentText.lastIndexOf("}");

                    if (startIdx != -1 && endIdx != -1) {
                        String jsonStr = contentText.substring(startIdx, endIdx + 1)
                            .replaceAll("'", "\"")  // 작은따옴표를 큰따옴표로 변경
                            .replaceAll("(\\w+):", "\"$1\":");  // 키에 따옴표 추가

                        try {
                            JsonNode info = objectMapper.readTree(jsonStr);
                            boolean updated = false;

                            // 반려동물 동반 정보
                            if (candidate.getPetFriendly() == null &&
                                !info.path("pet_friendly").isMissingNode() &&
                                !info.path("pet_friendly").isNull()) {
                                candidate.setPetFriendly(info.path("pet_friendly").asBoolean());
                                updated = true;
                            }

                            // 입장료 정보
                            if (candidate.getAdmissionFee() == null &&
                                !info.path("admission_fee").isMissingNode() &&
                                !info.path("admission_fee").isNull()) {
                                String fee = info.path("admission_fee").asText();
                                if (!fee.equals("unknown") && !fee.isEmpty()) {
                                    candidate.setAdmissionFee(fee);
                                    updated = true;
                                }
                            }

                            // 운영시간 정보 (기존 정보가 없는 경우만)
                            if (candidate.getBusinessHours() == null &&
                                !info.path("hours").isMissingNode() &&
                                !info.path("hours").isNull()) {
                                String hours = info.path("hours").asText();
                                if (!hours.equals("unknown") && !hours.isEmpty()) {
                                    candidate.setBusinessHours(hours);
                                    updated = true;
                                }
                            }

                            if (updated) {
                                log.debug("Enriched {} with Perplexity data", candidate.getName());
                            }

                            return updated;
                        } catch (Exception jsonError) {
                            log.error("Failed to parse Perplexity JSON response: {}", jsonError.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Perplexity API error for {}: {}", candidate.getName(), e.getMessage());
        }

        return false;
    }

    @Transactional
    public void enrichSpecificRegion(String region) {
        List<TravelCandidate> candidates = travelCandidateRepository.findByRegionOrderByQualityScore(region);
        log.info("Starting enrichment for {} candidates in region {}", candidates.size(), region);

        int enriched = 0;
        for (TravelCandidate candidate : candidates) {
            try {
                boolean updated = false;

                if (candidate.getGooglePlaceId() != null) {
                    updated = enrichWithGooglePlacesDetails(candidate);
                }

                if (candidate.getPetFriendly() == null || candidate.getAdmissionFee() == null) {
                    updated = updated || enrichWithPerplexityDetails(candidate);
                }

                if (updated) {
                    candidate.setAiEnriched(true);
                    travelCandidateRepository.save(candidate);
                    enriched++;
                }

                // API 제한
                TimeUnit.MILLISECONDS.sleep(200);

            } catch (Exception e) {
                log.error("Error enriching candidate: {}", e.getMessage());
            }
        }

        log.info("Enriched {} out of {} candidates in region {}", enriched, candidates.size(), region);
    }
}