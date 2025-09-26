package com.compass.domain.chat.stage1.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.entity.TravelInfo;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.repository.TravelInfoRepository;
import com.compass.domain.chat.common.constants.StageConstants;
import com.compass.domain.chat.common.utils.TravelPlaceConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

// Stage 1: Phase 2 정보 기반 지역 필터링 및 카테고리별 후보 제공
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Stage1DestinationSelectionService {

    private final TravelInfoRepository travelInfoRepository;
    private final TravelCandidateRepository travelCandidateRepository;
    private final TravelPlaceConverter travelPlaceConverter;

    // Phase 2 정보 기반으로 지역 필터링 및 카테고리별 후보 제공
    public Stage1Response processDestinationSelection(String threadId) {
        log.info("Stage 1: 여행지 선별 시작 - threadId: {}", threadId);

        // 1. Phase 2에서 수집된 정보 로드
        TravelInfo travelInfo = travelInfoRepository.findByThreadId(threadId)
            .orElseThrow(() -> new IllegalArgumentException("여행 정보를 찾을 수 없습니다: " + threadId));

        // 2. 목적지 정보 추출
        List<String> destinations = travelInfo.getDestinations();
        if (destinations == null || destinations.isEmpty()) {
            throw new IllegalStateException("목적지 정보가 없습니다");
        }

        log.info("목적지: {}, 여행 스타일: {}", destinations, travelInfo.getTravelStyle());

        // 3. 지역별 후보지 수집
        Map<String, CategoryCandidates> regionCandidates = new HashMap<>();

        for (String destination : destinations) {
            CategoryCandidates candidates = collectCandidatesByCategory(destination, travelInfo.getTravelStyle());
            regionCandidates.put(destination, candidates);
        }

        // 4. 응답 생성
        return Stage1Response.builder()
            .threadId(threadId)
            .destinations(destinations)
            .regionCandidates(regionCandidates)
            .totalCandidates(countTotalCandidates(regionCandidates))
            .build();
    }

    // 카테고리별 후보 수집 (각 카테고리당 10개) - Google Places 리뷰수 기반
    private CategoryCandidates collectCandidatesByCategory(String destination, List<String> travelStyle) {
        log.debug("{}의 카테고리별 후보 수집", destination);

        // DB에서 지역별 후보 조회
        List<TravelCandidate> allCandidates = travelCandidateRepository.findByRegion(destination);

        if (allCandidates.isEmpty()) {
            log.warn("{} 지역의 후보가 없습니다. Pre-Stage 데이터 수집이 필요합니다.", destination);
            return CategoryCandidates.empty();
        }

        // 카테고리별 그룹화
        Map<String, List<TravelPlace>> byCategory = new HashMap<>();

        // 주요 카테고리 정의 (상수 사용)
        List<String> categories = List.of(
            StageConstants.Category.TOURIST_ATTRACTION,
            StageConstants.Category.RESTAURANT,
            StageConstants.Category.CAFE,
            StageConstants.Category.SHOPPING,
            StageConstants.Category.ACTIVITY,
            StageConstants.Category.CULTURE,
            StageConstants.Category.NATURE,
            StageConstants.Category.THEME_PARK,
            StageConstants.Category.NIGHT_VIEW,
            StageConstants.Category.ACCOMMODATION
        );

        for (String category : categories) {
            List<TravelPlace> categoryPlaces = filterByCategory(allCandidates, category, travelStyle);

            // 리뷰수 기반 상위 N개 선택 (상수 사용)
            // Google Places API에서 수집된 리뷰수를 우선순위로 사용
            List<TravelPlace> topCandidates = categoryPlaces.stream()
                .sorted(Comparator
                    // 1순위: 리뷰수 (많을수록 신뢰도 높음)
                    .comparing((TravelPlace p) -> p.getReviewCount() != null ? p.getReviewCount() : 0)
                    // 2순위: 평점 (리뷰수가 같을 경우)
                    .thenComparing(p -> p.getRating() != null ? p.getRating() : 0.0)
                    .reversed())
                .limit(StageConstants.Limits.CANDIDATES_PER_CATEGORY)
                .collect(Collectors.toList());

            log.debug("카테고리 '{}': {}개 후보 중 상위 {}개 선별 (최고 리뷰수: {})",
                category, categoryPlaces.size(),
                StageConstants.Limits.CANDIDATES_PER_CATEGORY,
                topCandidates.isEmpty() ? 0 : topCandidates.get(0).getReviewCount());

            byCategory.put(category, topCandidates);
        }

        return CategoryCandidates.builder()
            .region(destination)
            .categoryCandidates(byCategory)
            .build();
    }

    // 카테고리와 여행 스타일에 맞는 장소 필터링
    private List<TravelPlace> filterByCategory(List<TravelCandidate> candidates,
                                              String category,
                                              List<String> travelStyle) {
        return candidates.stream()
            .filter(c -> matchesCategory(c, category))
            .filter(c -> matchesTravelStyle(c, travelStyle))
            .map(this::convertToTravelPlace)
            .collect(Collectors.toList());
    }

    // 카테고리 매칭
    private boolean matchesCategory(TravelCandidate candidate, String category) {
        String candidateCategory = candidate.getCategory();
        if (candidateCategory == null) return false;

        return switch (category) {
            case "관광명소" -> candidateCategory.contains("관광") || candidateCategory.contains("명소");
            case "맛집" -> candidateCategory.contains("음식") || candidateCategory.contains("레스토랑")
                        || candidateCategory.contains("맛집");
            case "카페" -> candidateCategory.contains("카페") || candidateCategory.contains("베이커리");
            case "쇼핑" -> candidateCategory.contains("쇼핑") || candidateCategory.contains("시장");
            case "액티비티" -> candidateCategory.contains("액티비티") || candidateCategory.contains("체험");
            case "문화체험" -> candidateCategory.contains("문화") || candidateCategory.contains("전통");
            case "자연경관" -> candidateCategory.contains("자연") || candidateCategory.contains("공원");
            case "테마파크" -> candidateCategory.contains("테마") || candidateCategory.contains("놀이");
            case "야경명소" -> candidateCategory.contains("야경") || candidateCategory.contains("전망");
            case "숙박" -> candidateCategory.contains("호텔") || candidateCategory.contains("숙박");
            default -> false;
        };
    }

    // 여행 스타일 매칭
    private boolean matchesTravelStyle(TravelCandidate candidate, List<String> travelStyle) {
        if (travelStyle == null || travelStyle.isEmpty()) return true;

        // 여행 스타일에 따른 필터링
        for (String style : travelStyle) {
            switch (style) {
                case "편안한":
                    // 평점이 높고 리뷰가 많은 검증된 장소
                    if (candidate.getRating() != null && candidate.getRating() >= 4.0) {
                        return true;
                    }
                    break;
                case "활동적인":
                    if (candidate.getCategory() != null &&
                        (candidate.getCategory().contains("액티비티") ||
                         candidate.getCategory().contains("체험"))) {
                        return true;
                    }
                    break;
                case "문화":
                    if (candidate.getCategory() != null &&
                        (candidate.getCategory().contains("문화") ||
                         candidate.getCategory().contains("박물관") ||
                         candidate.getCategory().contains("전통"))) {
                        return true;
                    }
                    break;
                case "미식":
                    if (candidate.getCategory() != null &&
                        (candidate.getCategory().contains("맛집") ||
                         candidate.getCategory().contains("음식"))) {
                        return true;
                    }
                    break;
                case "쇼핑":
                    if (candidate.getCategory() != null &&
                        candidate.getCategory().contains("쇼핑")) {
                        return true;
                    }
                    break;
            }
        }

        return true; // 특정 스타일이 매치되지 않으면 기본적으로 포함
    }

    // TravelCandidate를 TravelPlace로 변환 (유틸리티 사용)
    private TravelPlace convertToTravelPlace(TravelCandidate candidate) {
        return travelPlaceConverter.fromCandidate(candidate);
    }

    // 전체 후보 개수 계산
    private int countTotalCandidates(Map<String, CategoryCandidates> regionCandidates) {
        return regionCandidates.values().stream()
            .mapToInt(c -> c.categoryCandidates().values().stream()
                .mapToInt(List::size)
                .sum())
            .sum();
    }

    // Stage 1 응답 DTO
    public record Stage1Response(
        String threadId,
        List<String> destinations,
        Map<String, CategoryCandidates> regionCandidates,
        int totalCandidates
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String threadId;
            private List<String> destinations;
            private Map<String, CategoryCandidates> regionCandidates;
            private int totalCandidates;

            public Builder threadId(String threadId) {
                this.threadId = threadId;
                return this;
            }

            public Builder destinations(List<String> destinations) {
                this.destinations = destinations;
                return this;
            }

            public Builder regionCandidates(Map<String, CategoryCandidates> regionCandidates) {
                this.regionCandidates = regionCandidates;
                return this;
            }

            public Builder totalCandidates(int totalCandidates) {
                this.totalCandidates = totalCandidates;
                return this;
            }

            public Stage1Response build() {
                return new Stage1Response(threadId, destinations, regionCandidates, totalCandidates);
            }
        }
    }

    // 카테고리별 후보 DTO
    public record CategoryCandidates(
        String region,
        Map<String, List<TravelPlace>> categoryCandidates
    ) {
        public static CategoryCandidates empty() {
            return new CategoryCandidates("", new HashMap<>());
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String region;
            private Map<String, List<TravelPlace>> categoryCandidates;

            public Builder region(String region) {
                this.region = region;
                return this;
            }

            public Builder categoryCandidates(Map<String, List<TravelPlace>> categoryCandidates) {
                this.categoryCandidates = categoryCandidates;
                return this;
            }

            public CategoryCandidates build() {
                return new CategoryCandidates(region, categoryCandidates);
            }
        }
    }
}