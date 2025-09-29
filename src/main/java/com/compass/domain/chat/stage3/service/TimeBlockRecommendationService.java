package com.compass.domain.chat.stage3.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.TravelPlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeBlockRecommendationService {

    // 시간 블록별 적합 카테고리 정의
    private static final Map<String, List<String>> TIME_BLOCK_CATEGORIES = Map.of(
        "09:00-12:00", List.of(
            "카페", "cafe", "관광지", "고궁", "역사", "박물관", "미술관",
            "공원", "정원", "산책", "문화", "전망대"
        ),
        "12:00-15:00", List.of(
            "맛집", "restaurant", "식당", "전통시장", "쇼핑", "백화점",
            "관광지", "박물관", "미술관", "문화체험"
        ),
        "15:00-18:00", List.of(
            "관광지", "테마파크", "쇼핑", "체험", "액티비티", "전시",
            "갤러리", "공원", "산책", "카페", "디저트"
        ),
        "18:00-21:00", List.of(
            "맛집", "저녁", "야경", "전망대", "거리", "마켓", "나이트마켓",
            "공연", "문화", "클럽", "바", "라운지", "야간개장"
        )
    );

    // 시간 블록별 카테고리 가중치
    private static final Map<String, Map<String, Double>> TIME_BLOCK_WEIGHTS = Map.of(
        "09:00-12:00", Map.of(
            "카페", 1.5,
            "관광지", 1.3,
            "고궁", 1.4,
            "박물관", 1.2
        ),
        "12:00-15:00", Map.of(
            "맛집", 1.5,
            "식당", 1.5,
            "전통시장", 1.3,
            "쇼핑", 1.2
        ),
        "15:00-18:00", Map.of(
            "관광지", 1.3,
            "테마파크", 1.4,
            "체험", 1.3,
            "카페", 1.2
        ),
        "18:00-21:00", Map.of(
            "맛집", 1.5,
            "야경", 1.4,
            "전망대", 1.3,
            "야간개장", 1.4
        )
    );

    /**
     * 시간 블록별로 최적화된 장소 추천
     */
    public Map<String, List<TravelPlace>> recommendByTimeBlocks(
            List<TravelCandidate> candidates,
            List<TravelPlace> userSelectedPlaces,
            String travelStyle,
            int placesPerBlock) {

        Map<String, List<TravelPlace>> timeBlockRecommendations = new LinkedHashMap<>();
        Set<String> usedPlaceIds = new HashSet<>();

        // 사용자 선택 장소 ID 수집
        userSelectedPlaces.forEach(place -> {
            if (place.getPlaceId() != null) {
                usedPlaceIds.add(place.getPlaceId());
            }
        });

        log.info("Starting time block recommendations. User selected places: {}", userSelectedPlaces.size());

        for (String timeBlock : TIME_BLOCK_CATEGORIES.keySet()) {
            List<TravelPlace> blockRecommendations = recommendForTimeBlock(
                candidates,
                timeBlock,
                travelStyle,
                usedPlaceIds,
                placesPerBlock
            );

            timeBlockRecommendations.put(timeBlock, blockRecommendations);

            // 추천된 장소 ID를 사용 목록에 추가
            blockRecommendations.forEach(place -> {
                if (place.getPlaceId() != null) {
                    usedPlaceIds.add(place.getPlaceId());
                }
            });

            log.info("Time block {} - Recommended {} places", timeBlock, blockRecommendations.size());
        }

        return timeBlockRecommendations;
    }

    /**
     * 특정 시간 블록에 적합한 장소 추천
     */
    private List<TravelPlace> recommendForTimeBlock(
            List<TravelCandidate> candidates,
            String timeBlock,
            String travelStyle,
            Set<String> usedPlaceIds,
            int maxPlaces) {

        List<String> preferredCategories = TIME_BLOCK_CATEGORIES.get(timeBlock);
        Map<String, Double> categoryWeights = TIME_BLOCK_WEIGHTS.get(timeBlock);

        return candidates.stream()
            // 중복 제거
            .filter(c -> c.getPlaceId() == null || !usedPlaceIds.contains(c.getPlaceId()))
            // 품질 필터링
            .filter(c -> c.getQualityScore() != null && c.getQualityScore() >= 0.7)
            .filter(c -> c.getRating() != null && c.getRating() >= 4.0)
            // 시간대별 카테고리 매칭
            .filter(c -> matchesTimeBlockCategory(c, preferredCategories))
            // 점수 계산 및 정렬
            .sorted((a, b) -> {
                double scoreA = calculateTimeBlockScore(a, preferredCategories, categoryWeights, travelStyle);
                double scoreB = calculateTimeBlockScore(b, preferredCategories, categoryWeights, travelStyle);
                return Double.compare(scoreB, scoreA);
            })
            .limit(maxPlaces)
            .map(c -> {
                TravelPlace place = c.toTravelPlace();
                place.setIsUserSelected(false);
                return place;
            })
            .collect(Collectors.toList());
    }

    /**
     * 시간 블록 카테고리 매칭 확인
     */
    private boolean matchesTimeBlockCategory(TravelCandidate candidate, List<String> categories) {
        String candidateCategory = candidate.getCategory() != null ?
            candidate.getCategory().toLowerCase() : "";
        String candidateName = candidate.getName() != null ?
            candidate.getName().toLowerCase() : "";

        return categories.stream().anyMatch(cat -> {
            String lowerCat = cat.toLowerCase();
            return candidateCategory.contains(lowerCat) ||
                   candidateName.contains(lowerCat);
        });
    }

    /**
     * 시간 블록별 점수 계산
     */
    private double calculateTimeBlockScore(
            TravelCandidate candidate,
            List<String> preferredCategories,
            Map<String, Double> weights,
            String travelStyle) {

        double baseScore = candidate.getQualityScore() != null ?
            candidate.getQualityScore() : 0.0;

        // 카테고리 가중치 적용
        String category = candidate.getCategory() != null ?
            candidate.getCategory().toLowerCase() : "";

        double categoryWeight = 1.0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            if (category.contains(entry.getKey().toLowerCase())) {
                categoryWeight = Math.max(categoryWeight, entry.getValue());
            }
        }

        // 여행 스타일 가중치
        double styleWeight = matchesTravelStyle(candidate, travelStyle) ? 1.2 : 1.0;

        // 리뷰 수 가중치 (로그 스케일)
        int reviewCount = candidate.getReviewCount() != null ?
            candidate.getReviewCount() : 0;
        double reviewWeight = 1.0 + Math.log10(Math.max(1, reviewCount)) * 0.1;

        return baseScore * categoryWeight * styleWeight * reviewWeight;
    }

    /**
     * 여행 스타일 매칭 확인
     */
    private boolean matchesTravelStyle(TravelCandidate candidate, String travelStyle) {
        if (travelStyle == null || travelStyle.isEmpty()) {
            return true;
        }

        String category = candidate.getCategory() != null ?
            candidate.getCategory().toLowerCase() : "";
        String name = candidate.getName() != null ?
            candidate.getName().toLowerCase() : "";
        String description = candidate.getDescription() != null ?
            candidate.getDescription().toLowerCase() : "";

        String styleLower = travelStyle.toLowerCase();

        // 여행 스타일별 키워드 매칭
        Map<String, List<String>> styleKeywords = Map.of(
            "문화", List.of("문화", "역사", "고궁", "박물관", "미술관", "전통"),
            "자연", List.of("자연", "공원", "산", "숲", "정원", "생태"),
            "쇼핑", List.of("쇼핑", "백화점", "시장", "아울렛", "면세점"),
            "음식", List.of("맛집", "식당", "카페", "디저트", "전통음식"),
            "활동", List.of("체험", "액티비티", "테마파크", "놀이", "스포츠")
        );

        for (Map.Entry<String, List<String>> entry : styleKeywords.entrySet()) {
            if (styleLower.contains(entry.getKey())) {
                return entry.getValue().stream().anyMatch(keyword ->
                    category.contains(keyword) ||
                    name.contains(keyword) ||
                    description.contains(keyword)
                );
            }
        }

        return true;
    }

    /**
     * 시간 블록별 장소 분배 (사용자 선택 포함)
     */
    public Map<String, List<TravelPlace>> distributeWithUserSelection(
            List<TravelPlace> userSelectedPlaces,
            Map<String, List<TravelPlace>> aiRecommendations) {

        Map<String, List<TravelPlace>> finalDistribution = new LinkedHashMap<>();
        List<TravelPlace> remainingUserPlaces = new ArrayList<>(userSelectedPlaces);

        // 중복 방지를 위한 Set
        Set<String> globalUsedPlaceIds = new HashSet<>();

        for (String timeBlock : TIME_BLOCK_CATEGORIES.keySet()) {
            List<TravelPlace> blockPlaces = new ArrayList<>();

            // 1. 해당 시간대에 적합한 사용자 선택 장소 우선 배치
            List<String> blockCategories = TIME_BLOCK_CATEGORIES.get(timeBlock);
            List<TravelPlace> matchingUserPlaces = remainingUserPlaces.stream()
                .filter(place -> {
                    String placeKey = getPlaceKey(place);
                    return !globalUsedPlaceIds.contains(placeKey);
                })
                .filter(place -> matchesTimeBlock(place, blockCategories))
                .limit(1)  // 블록당 최대 1개의 사용자 선택
                .collect(Collectors.toList());

            blockPlaces.addAll(matchingUserPlaces);
            matchingUserPlaces.forEach(p -> globalUsedPlaceIds.add(getPlaceKey(p)));
            remainingUserPlaces.removeAll(matchingUserPlaces);

            // 2. AI 추천으로 보충 (블록당 총 2개까지)
            if (blockPlaces.size() < 2) {
                List<TravelPlace> aiPlaces = aiRecommendations.get(timeBlock);
                if (aiPlaces != null && !aiPlaces.isEmpty()) {
                    int needed = 2 - blockPlaces.size();
                    List<TravelPlace> aiToAdd = aiPlaces.stream()
                        .filter(place -> {
                            String placeKey = getPlaceKey(place);
                            return !globalUsedPlaceIds.contains(placeKey);
                        })
                        .limit(needed)
                        .collect(Collectors.toList());

                    blockPlaces.addAll(aiToAdd);
                    aiToAdd.forEach(p -> globalUsedPlaceIds.add(getPlaceKey(p)));
                }
            }

            finalDistribution.put(timeBlock, blockPlaces);

            log.info("Time block {} - Final distribution: {} places (User: {}, AI: {})",
                timeBlock, blockPlaces.size(),
                matchingUserPlaces.size(),
                blockPlaces.size() - matchingUserPlaces.size());
        }

        // 3. 남은 사용자 선택 장소들을 빈 블록 우선으로 분배
        distributeRemainingPlaces(remainingUserPlaces, finalDistribution, globalUsedPlaceIds);

        return finalDistribution;
    }

    // 장소 고유 키 생성 (중복 방지용)
    private String getPlaceKey(TravelPlace place) {
        if (place.getPlaceId() != null && !place.getPlaceId().isEmpty()) {
            return place.getPlaceId();
        }
        if (place.getName() != null && !place.getName().isEmpty()) {
            // 정규화된 이름 사용 (공백 제거, 소문자)
            return place.getName().toLowerCase().replaceAll("\\s+", "");
        }
        // 최후의 수단: 좌표 기반
        if (place.getLatitude() != null && place.getLongitude() != null) {
            return String.format("%.6f_%.6f", place.getLatitude(), place.getLongitude());
        }
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 장소가 시간 블록에 적합한지 확인
     */
    private boolean matchesTimeBlock(TravelPlace place, List<String> categories) {
        String placeCategory = place.getCategory() != null ?
            place.getCategory().toLowerCase() : "";
        String placeName = place.getName() != null ?
            place.getName().toLowerCase() : "";

        return categories.stream().anyMatch(cat -> {
            String lowerCat = cat.toLowerCase();
            return placeCategory.contains(lowerCat) || placeName.contains(lowerCat);
        });
    }

    /**
     * 남은 장소들을 빈 블록 우선으로 분배
     */
    private void distributeRemainingPlaces(
            List<TravelPlace> remainingPlaces,
            Map<String, List<TravelPlace>> distribution,
            Set<String> globalUsedPlaceIds) {

        if (remainingPlaces.isEmpty()) {
            return;
        }

        List<String> timeBlocks = new ArrayList<>(distribution.keySet());

        for (TravelPlace place : remainingPlaces) {
            String placeKey = getPlaceKey(place);

            // 이미 사용된 장소는 스킵
            if (globalUsedPlaceIds.contains(placeKey)) {
                continue;
            }

            // 1순위: 빈 블록 채우기
            Optional<String> emptyBlock = timeBlocks.stream()
                .filter(block -> distribution.get(block).isEmpty())
                .findFirst();

            if (emptyBlock.isPresent()) {
                distribution.get(emptyBlock.get()).add(place);
                globalUsedPlaceIds.add(placeKey);
                log.info("Added remaining place {} to empty block {}", place.getName(), emptyBlock.get());
                continue;
            }

            // 2순위: 1개만 있는 블록 채우기 (최대 2개까지)
            Optional<String> singleBlock = timeBlocks.stream()
                .filter(block -> distribution.get(block).size() == 1)
                .findFirst();

            if (singleBlock.isPresent()) {
                distribution.get(singleBlock.get()).add(place);
                globalUsedPlaceIds.add(placeKey);
                log.info("Added remaining place {} to single block {}", place.getName(), singleBlock.get());
            }

            // 모든 블록이 2개 이상이면 추가하지 않음
        }
    }
}