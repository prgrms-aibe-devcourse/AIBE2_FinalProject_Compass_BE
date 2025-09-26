package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoMapEnrichmentService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final WebClient webClient;

    @Value("${kakao.rest.key:}")
    private String kakaoRestKey;

    private static final String KAKAO_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final String KAKAO_CATEGORY_URL = "https://dapi.kakao.com/v2/local/search/category.json";
    private static final String KAKAO_COORD_URL = "https://dapi.kakao.com/v2/local/geo/coord2address.json";

    // 전체 카카오맵 보강
    @Transactional
    public int enrichAllWithKakaoMap() {
        log.info("카카오맵 API를 통한 전체 데이터 보강 시작");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<TravelCandidate> candidates = travelCandidateRepository.findAll();
        log.info("카카오맵 보강 대상: {} 개", candidates.size());

        candidates.forEach(candidate -> {
            try {
                boolean enriched = enrichWithKakaoData(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                    log.debug("카카오맵 보강 성공: {}", candidate.getName());
                } else {
                    failCount.incrementAndGet();
                }

                // API 제한 (초당 30건)
                Thread.sleep(35);

            } catch (Exception e) {
                log.error("카카오맵 보강 실패 - {}: {}", candidate.getName(), e.getMessage());
                failCount.incrementAndGet();
            }
        });

        log.info("카카오맵 보강 완료 - 성공: {}, 실패: {}", successCount.get(), failCount.get());
        return successCount.get();
    }

    // 지역별 보강
    @Transactional
    public int enrichByRegion(String region) {
        log.info("{} 지역 카카오맵 보강 시작", region);

        AtomicInteger successCount = new AtomicInteger(0);
        List<TravelCandidate> candidates = travelCandidateRepository.findByRegion(region);

        candidates.forEach(candidate -> {
            try {
                boolean enriched = enrichWithKakaoData(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                }
                Thread.sleep(35);
            } catch (Exception e) {
                log.error("지역별 보강 실패: {}", e.getMessage());
            }
        });

        log.info("{} 지역 보강 완료: {} 개", region, successCount.get());
        return successCount.get();
    }

    // 비동기 배치 보강
    @Async
    @Transactional
    public CompletableFuture<Integer> enrichBatchAsync(List<Long> candidateIds) {
        log.info("비동기 카카오맵 보강 시작 - {} 개", candidateIds.size());

        AtomicInteger successCount = new AtomicInteger(0);
        List<TravelCandidate> candidates = travelCandidateRepository.findAllById(candidateIds);

        candidates.parallelStream().forEach(candidate -> {
            try {
                boolean enriched = enrichWithKakaoData(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                }
                Thread.sleep(35);
            } catch (Exception e) {
                log.error("비동기 보강 실패: {}", e.getMessage());
            }
        });

        return CompletableFuture.completedFuture(successCount.get());
    }

    // 개별 데이터 보강
    private boolean enrichWithKakaoData(TravelCandidate candidate) {
        try {
            // 키워드 검색
            Map<String, Object> searchResult = searchByKeyword(
                candidate.getName(),
                candidate.getRegion()
            );

            if (searchResult != null) {
                // 도로명/지번 주소 업데이트
                String roadAddress = (String) searchResult.get("road_address_name");
                String jibunAddress = (String) searchResult.get("address_name");

                if (roadAddress != null && !roadAddress.isEmpty()) {
                    candidate.setAddress(roadAddress);
                    candidate.setDetailedAddress(roadAddress);
                } else if (jibunAddress != null && !jibunAddress.isEmpty()) {
                    candidate.setAddress(jibunAddress);
                    candidate.setDetailedAddress(jibunAddress);
                }

                // 카테고리 정제
                String categoryName = (String) searchResult.get("category_name");
                if (categoryName != null) {
                    candidate.setCategory(refineCategory(categoryName));
                }

                // 전화번호
                String phone = (String) searchResult.get("phone");
                if (phone != null && !phone.isEmpty()) {
                    candidate.setPhoneNumber(phone);
                }

                // 좌표 (카카오가 더 정확한 경우)
                String x = (String) searchResult.get("x");  // 경도
                String y = (String) searchResult.get("y");  // 위도

                if (x != null && y != null) {
                    double longitude = Double.parseDouble(x);
                    double latitude = Double.parseDouble(y);

                    // 기존 좌표가 없거나 차이가 큰 경우 업데이트
                    if (candidate.getLatitude() == null ||
                        Math.abs(candidate.getLatitude() - latitude) > 0.001) {
                        candidate.setLatitude(latitude);
                        candidate.setLongitude(longitude);
                    }
                }

                // 카카오 Place URL
                String placeUrl = (String) searchResult.get("place_url");
                if (placeUrl != null && candidate.getWebsite() == null) {
                    candidate.setWebsite(placeUrl);
                }

                // 카테고리 그룹 코드
                String categoryGroupCode = (String) searchResult.get("category_group_code");
                if (categoryGroupCode != null) {
                    updateCategoryInfo(candidate, categoryGroupCode);
                }

                log.debug("카카오맵 데이터 보강 완료: {}", candidate.getName());
                return true;
            }

            // 검색 실패 시 좌표로 재시도
            if (candidate.getLatitude() != null && candidate.getLongitude() != null) {
                return enrichByCoordinates(candidate);
            }

            return false;

        } catch (Exception e) {
            log.error("카카오맵 보강 중 오류: {}", e.getMessage());
            return false;
        }
    }

    // 키워드 검색
    private Map<String, Object> searchByKeyword(String name, String region) {
        try {
            String query = region != null && !region.isEmpty()
                ? region + " " + name
                : name;

            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(KAKAO_SEARCH_URL)
                    .queryParam("query", query)
                    .queryParam("size", 1)
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("documents")) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                if (!documents.isEmpty()) {
                    return documents.get(0);
                }
            }

        } catch (Exception e) {
            log.error("카카오 키워드 검색 실패: {}", e.getMessage());
        }

        return null;
    }

    // 좌표로 주소 검색 (역지오코딩)
    private boolean enrichByCoordinates(TravelCandidate candidate) {
        try {
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(KAKAO_COORD_URL)
                    .queryParam("x", candidate.getLongitude())
                    .queryParam("y", candidate.getLatitude())
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("documents")) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");

                if (!documents.isEmpty()) {
                    Map<String, Object> firstResult = documents.get(0);

                    // 도로명 주소
                    Map<String, Object> roadAddress = (Map<String, Object>) firstResult.get("road_address");
                    if (roadAddress != null) {
                        String addressName = (String) roadAddress.get("address_name");
                        if (addressName != null) {
                            candidate.setAddress(addressName);
                            candidate.setDetailedAddress(addressName);
                        }
                    } else {
                        // 지번 주소
                        Map<String, Object> address = (Map<String, Object>) firstResult.get("address");
                        if (address != null) {
                            String addressName = (String) address.get("address_name");
                            if (addressName != null) {
                                candidate.setAddress(addressName);
                                candidate.setDetailedAddress(addressName);
                            }
                        }
                    }

                    return true;
                }
            }

        } catch (Exception e) {
            log.error("역지오코딩 실패: {}", e.getMessage());
        }

        return false;
    }

    // 카테고리 정제
    private String refineCategory(String kakaoCategory) {
        // "음식점 > 한식 > 육류,고기" → "한식"
        if (kakaoCategory == null || kakaoCategory.isEmpty()) {
            return null;
        }

        String[] parts = kakaoCategory.split(">");
        if (parts.length >= 2) {
            return parts[1].trim();
        } else if (parts.length == 1) {
            return parts[0].trim();
        }

        return kakaoCategory;
    }

    // 카테고리 그룹 코드에 따른 추가 정보 업데이트
    private void updateCategoryInfo(TravelCandidate candidate, String categoryGroupCode) {
        // MT1: 대형마트, CS2: 편의점, FD6: 음식점, CE7: 카페, AT4: 관광명소 등
        switch (categoryGroupCode) {
            case "FD6":
                if (candidate.getCategory() == null || candidate.getCategory().equals("식당")) {
                    candidate.setCategory("음식점");
                }
                break;
            case "CE7":
                candidate.setCategory("카페");
                break;
            case "AT4":
                candidate.setCategory("관광명소");
                break;
            case "CT1":
                candidate.setCategory("문화시설");
                break;
            case "AD5":
                candidate.setCategory("숙박");
                break;
        }
    }

    // 통계 정보
    public Map<String, Object> getKakaoEnrichmentStatistics() {
        long total = travelCandidateRepository.count();
        long withAddress = travelCandidateRepository.countByAddressIsNotNull();
        long withPhone = total; // 전화번호가 있는 개수 쿼리 추가 필요
        long withCoordinates = total; // 좌표가 있는 개수 쿼리 추가 필요

        return Map.of(
            "total", total,
            "withAddress", withAddress,
            "withPhone", withPhone,
            "withCoordinates", withCoordinates,
            "addressCompletionRate", (double) withAddress / total * 100
        );
    }
}