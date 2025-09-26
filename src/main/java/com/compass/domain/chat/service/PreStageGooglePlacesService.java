package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.entity.TravelCandidate.TimeBlock;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pre-Stage: Google Places API를 통한 여행지 데이터 수집 및 DB 저장 서비스
 * Stage 1 실행 전에 미리 데이터를 수집하여 DB에 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PreStageGooglePlacesService {

    private final GooglePlacesCollector googlePlacesCollector;
    private final TravelCandidateRepository travelCandidateRepository;
    private final ObjectMapper objectMapper;

    /**
     * Pre-Stage 메인 메서드: 지역별 여행지 데이터 수집 및 DB 저장
     *
     * @param region 대상 지역
     * @param forceUpdate 기존 데이터 강제 업데이트 여부
     * @return 저장된 장소 수
     */
    @Transactional
    public int collectAndSaveForRegion(String region, boolean forceUpdate) {
        log.info("=== Pre-Stage 시작: {} 지역 Google Places 데이터 수집 ===", region);

        // 기존 데이터 확인
        if (!forceUpdate) {
            List<String> existingPlaceIds = travelCandidateRepository.findExistingPlaceIds(region);
            if (!existingPlaceIds.isEmpty()) {
                log.info("{} 지역에 이미 {}개의 장소가 저장되어 있습니다.", region, existingPlaceIds.size());

                // 7일 이상 된 데이터만 업데이트
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                List<TravelCandidate> oldData = travelCandidateRepository.findOldData(sevenDaysAgo);

                if (oldData.isEmpty()) {
                    log.info("최신 데이터가 이미 존재합니다. 수집을 건너뜁니다.");
                    return 0;
                }

                log.info("{}개의 오래된 데이터를 업데이트합니다.", oldData.size());
            }
        }

        // 시간블럭별 데이터 수집
        Map<GooglePlacesCollector.TimeBlock, List<GooglePlacesCollector.GooglePlace>> collectedData =
            googlePlacesCollector.collectPlacesByTimeBlock(region, 20);

        // 엔티티로 변환 및 저장
        int savedCount = 0;
        for (Map.Entry<GooglePlacesCollector.TimeBlock, List<GooglePlacesCollector.GooglePlace>> entry : collectedData.entrySet()) {
            GooglePlacesCollector.TimeBlock collectorTimeBlock = entry.getKey();
            List<GooglePlacesCollector.GooglePlace> places = entry.getValue();

            // Collector의 TimeBlock을 Entity의 TimeBlock으로 변환
            TimeBlock timeBlock = TimeBlock.valueOf(collectorTimeBlock.name());

            for (GooglePlacesCollector.GooglePlace collectorPlace : places) {
                try {
                    TravelCandidate entity = convertToEntity(collectorPlace, region, timeBlock);

                    // 중복 체크
                    Optional<TravelCandidate> existing = travelCandidateRepository
                        .findByPlaceIdAndRegion(entity.getPlaceId(), region);

                    if (existing.isPresent()) {
                        // 기존 데이터 업데이트
                        TravelCandidate existingEntity = existing.get();
                        updateEntity(existingEntity, collectorPlace);
                        travelCandidateRepository.save(existingEntity);
                        log.debug("기존 장소 업데이트: {} ({})", existingEntity.getName(), region);
                    } else {
                        // 신규 저장
                        travelCandidateRepository.save(entity);
                        savedCount++;
                        log.debug("신규 장소 저장: {} ({})", entity.getName(), region);
                    }
                } catch (Exception e) {
                    log.error("장소 저장 실패: {}", e.getMessage());
                }
            }
        }

        log.info("=== Pre-Stage 완료: {} 지역, {}개 신규 저장 ===", region, savedCount);
        logStatistics(region);

        return savedCount;
    }

    /**
     * 모든 주요 지역 데이터 일괄 수집 (비동기)
     */
    @Async
    public void collectAllRegionsAsync() {
        List<String> regions = List.of("서울", "부산", "인천", "대구", "대전",
                                       "광주", "울산", "제주", "경주", "강릉", "전주", "여수");

        log.info("=== Pre-Stage 전체 지역 수집 시작: {}개 지역 ===", regions.size());

        int totalSaved = 0;
        for (String region : regions) {
            try {
                int saved = collectAndSaveForRegion(region, false);
                totalSaved += saved;

                // API 제한을 위한 지연
                Thread.sleep(2000); // 2초 대기
            } catch (Exception e) {
                log.error("{} 지역 수집 실패: {}", region, e.getMessage());
            }
        }

        log.info("=== Pre-Stage 전체 완료: 총 {}개 장소 신규 저장 ===", totalSaved);
    }

    /**
     * 특정 도시의 여행지 데이터 수집 (Controller에서 호출)
     */
    public List<TravelCandidate> collectAndSavePlacesForCity(String city) {
        log.info("도시별 데이터 수집 시작: {}", city);

        // 수집 실행 (강제 업데이트 안함)
        collectAndSaveForRegion(city, false);

        // 저장된 데이터 조회 및 반환
        List<TravelCandidate> candidates = travelCandidateRepository.findByRegion(city);
        log.info("{} 지역에서 {}개 후보 반환", city, candidates.size());

        return candidates;
    }

    /**
     * 모든 도시의 여행지 데이터 일괄 수집 (동기)
     */
    public Map<String, List<TravelCandidate>> collectAndSaveAllCities() {
        List<String> regions = googlePlacesCollector.getAllRegions();
        Map<String, List<TravelCandidate>> result = new HashMap<>();

        log.info("=== Pre-Stage 전체 지역 수집 시작: {}개 지역 ===", regions.size());

        for (String region : regions) {
            try {
                List<TravelCandidate> candidates = collectAndSavePlacesForCity(region);
                result.put(region, candidates);

                // API 제한을 위한 지연
                Thread.sleep(2000); // 2초 대기
            } catch (Exception e) {
                log.error("{} 지역 수집 실패: {}", region, e.getMessage());
                result.put(region, new ArrayList<>());
            }
        }

        return result;
    }

    /**
     * GooglePlacesCollector.GooglePlace를 TravelCandidate 엔티티로 변환
     */
    private TravelCandidate convertToEntity(GooglePlacesCollector.GooglePlace collectorPlace, String region, TimeBlock timeBlock) {
        TravelCandidate entity = TravelCandidate.builder()
            .placeId(collectorPlace.placeId)
            .name(collectorPlace.name)
            .region(region)
            .subRegion(collectorPlace.subRegion)  // 세부 지역 정보 추가
            .category(mapGoogleTypeToCategory(collectorPlace.types))
            .timeBlock(timeBlock)
            .latitude(collectorPlace.latitude)
            .longitude(collectorPlace.longitude)
            .address(collectorPlace.vicinity)
            .rating(collectorPlace.rating)
            .reviewCount(collectorPlace.userRatingsTotal)
            .priceLevel(collectorPlace.priceLevel)
            .openNow(collectorPlace.openNow)
            .isActive(true)
            .build();

        // Google 타입 정보 JSON으로 저장
        if (collectorPlace.types != null) {
            try {
                entity.setGoogleTypes(objectMapper.writeValueAsString(collectorPlace.types));
            } catch (JsonProcessingException e) {
                log.error("Google types 직렬화 실패: {}", e.getMessage());
            }
        }

        // 사진 URL 생성
        if (collectorPlace.photoReference != null) {
            entity.setPhotoUrl(buildPhotoUrl(collectorPlace.photoReference));
        }

        // 설명 생성
        entity.setDescription(buildDescription(collectorPlace, timeBlock));

        return entity;
    }

    /**
     * 기존 엔티티 업데이트
     */
    private void updateEntity(TravelCandidate existing, GooglePlacesCollector.GooglePlace newData) {
        existing.setRating(newData.rating);
        existing.setReviewCount(newData.userRatingsTotal);
        existing.setPriceLevel(newData.priceLevel);
        existing.setOpenNow(newData.openNow);
        existing.setAddress(newData.vicinity);

        if (newData.photoReference != null) {
            existing.setPhotoUrl(buildPhotoUrl(newData.photoReference));
        }
    }

    /**
     * Google 타입을 한국어 카테고리로 매핑
     */
    private String mapGoogleTypeToCategory(List<String> types) {
        if (types == null || types.isEmpty()) return "기타";

        String primaryType = types.get(0);
        return switch (primaryType) {
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

    /**
     * Google Places 사진 URL 생성
     */
    private String buildPhotoUrl(String photoReference) {
        return String.format(
            "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=%s&key=${GOOGLE_PLACES_API_KEY}",
            photoReference
        );
    }

    /**
     * 장소 설명 생성
     */
    private String buildDescription(GooglePlacesCollector.GooglePlace place, TimeBlock timeBlock) {
        StringBuilder desc = new StringBuilder();

        if (place.rating > 0) {
            desc.append(String.format("평점: %.1f", place.rating));
        }

        if (place.userRatingsTotal > 0) {
            desc.append(String.format(" (리뷰 %d개)", place.userRatingsTotal));
        }

        if (place.priceLevel >= 0) {
            desc.append(" | 가격대: ").append("$".repeat(place.priceLevel + 1));
        }

        desc.append(" | 추천시간: ").append(timeBlock.getKoreanName());

        return desc.toString();
    }

    /**
     * 지역별 통계 로깅
     */
    private void logStatistics(String region) {
        log.info("=== {} 지역 Pre-Stage 통계 ===", region);

        // 임시로 통계 로깅 비활성화 (ArrayStoreException 회피)
        try {
            // 카테고리별 통계
            List<Object[]> categoryStats = travelCandidateRepository.getCategoryStatsByRegion(region);

            if (categoryStats != null && !categoryStats.isEmpty()) {
                log.info("카테고리별 분포:");
                categoryStats.forEach(stat -> {
                    if (stat != null && stat.length >= 2) {
                        log.info("  - {}: {}개", stat[0], stat[1]);
                    }
                });
            }

            // 시간블럭별 통계
            List<Object[]> timeBlockStats = travelCandidateRepository.getTimeBlockStatsByRegion(region);

            if (timeBlockStats != null && !timeBlockStats.isEmpty()) {
                log.info("시간블럭별 분포:");
                timeBlockStats.forEach(stat -> {
                    if (stat != null && stat.length >= 2) {
                        log.info("  - {}: {}개", stat[0], stat[1]);
                    }
                });
            }

            // 평균 통계
            Object[] avgStats = travelCandidateRepository.getAverageStatsForRegion(region);

            if (avgStats != null && avgStats.length >= 2 && avgStats[0] != null && avgStats[1] != null) {
                log.info("평균 평점: {}, 평균 리뷰 수: {}",
                         String.format("%.1f", ((Number) avgStats[0]).doubleValue()),
                         String.format("%.0f", ((Number) avgStats[1]).doubleValue()));
            }

            // 상위 5개 인기 장소
            List<TravelCandidate> topPlaces = travelCandidateRepository.findTopPopularPlaces(region, 5);
            if (topPlaces != null && !topPlaces.isEmpty()) {
                log.info("상위 5개 인기 장소:");
                topPlaces.forEach(place ->
                    log.info("  - {} ({}): 평점 {}, 리뷰 {}개",
                            place.getName(),
                            place.getCategory(),
                            place.getRating(),
                            place.getReviewCount())
                );
            }
        } catch (Exception e) {
            log.warn("통계 로깅 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * Stage 1을 위한 데이터 조회 메서드
     * Pre-Stage에서 수집된 데이터를 Stage 1에서 활용
     */
    public List<TravelCandidate> getPlacesForStage1(String region, TimeBlock timeBlock, Double minRating) {
        if (timeBlock != null && minRating != null) {
            return travelCandidateRepository.findByRegionTimeBlockAndMinRating(region, timeBlock, minRating);
        } else if (timeBlock != null) {
            return travelCandidateRepository.findByRegionAndTimeBlock(region, timeBlock);
        } else if (minRating != null) {
            return travelCandidateRepository.findByRegionAndMinRating(region, minRating);
        } else {
            return travelCandidateRepository.findByRegionOrderByQualityScore(region);
        }
    }

    /**
     * 데이터 갱신 스케줄러용 메서드
     * 주기적으로 오래된 데이터 업데이트
     */
    @Transactional
    public void refreshOldData(int daysOld) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(daysOld);
        List<TravelCandidate> oldPlaces = travelCandidateRepository.findOldData(beforeDate);

        log.info("{}일 이상 된 데이터 {}개 갱신 시작", daysOld, oldPlaces.size());

        // 지역별로 그룹핑
        Map<String, List<TravelCandidate>> placesByRegion = oldPlaces.stream()
            .collect(Collectors.groupingBy(TravelCandidate::getRegion));

        for (Map.Entry<String, List<TravelCandidate>> entry : placesByRegion.entrySet()) {
            String region = entry.getKey();
            collectAndSaveForRegion(region, true);
        }
    }
}