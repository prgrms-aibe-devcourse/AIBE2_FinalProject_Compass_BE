package com.compass.domain.chat.stage3.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Stage3TravelCandidateEnrichmentService {

    private final TravelCandidateRepository travelCandidateRepository;

    // Google Places Enhanced 데이터로 보강된 후보지 조회 (여행 스타일 필터링 포함)
    @Transactional(readOnly = true)
    public List<TravelCandidate> getEnrichedCandidates(String region, List<String> travelStyle) {
        log.info("Fetching enriched candidates for region: {} with travel style: {}", region, travelStyle);

        List<TravelCandidate> candidates = travelCandidateRepository
            .findByRegionAndIsActiveTrue(region);

        // Google Places 데이터가 있고 여행 스타일에 맞는 것들만
        List<TravelCandidate> enrichedCandidates = candidates.stream()
            .filter(c -> c.getGooglePlaceId() != null || c.getAiEnriched() != null)
            .filter(c -> matchesTravelStyle(c, travelStyle))
            .sorted((a, b) -> {
                // AI 보강 여부로 1차 정렬
                int aiCompare = Boolean.compare(
                    b.getAiEnriched() != null && b.getAiEnriched(),
                    a.getAiEnriched() != null && a.getAiEnriched()
                );
                if (aiCompare != 0) return aiCompare;

                // 품질 점수로 2차 정렬
                return Double.compare(
                    b.getQualityScore() != null ? b.getQualityScore() : 0,
                    a.getQualityScore() != null ? a.getQualityScore() : 0
                );
            })
            .collect(Collectors.toList());

        log.info("Found {} enriched candidates out of {} total (filtered by travel style)",
            enrichedCandidates.size(), candidates.size());

        return enrichedCandidates;
    }

    // 하위 호환성을 위한 오버로드 메서드
    @Transactional(readOnly = true)
    public List<TravelCandidate> getEnrichedCandidates(String region) {
        return getEnrichedCandidates(region, null);
    }

    // 여행 스타일 매칭 (Stage1과 동일한 로직)
    private boolean matchesTravelStyle(TravelCandidate candidate, List<String> travelStyle) {
        if (travelStyle == null || travelStyle.isEmpty()) return true;

        for (String style : travelStyle) {
            switch (style) {
                case "관광":
                    if (candidate.getCategory() != null &&
                        (candidate.getCategory().contains("관광") ||
                         candidate.getCategory().contains("명소") ||
                         candidate.getCategory().contains("랜드마크") ||
                         candidate.getCategory().contains("전망") ||
                         candidate.getCategory().contains("야경"))) {
                        return true;
                    }
                    break;
                case "맛집":
                    if (candidate.getCategory() != null &&
                        (candidate.getCategory().contains("맛집") ||
                         candidate.getCategory().contains("음식") ||
                         candidate.getCategory().contains("레스토랑") ||
                         candidate.getCategory().contains("식당"))) {
                        return true;
                    }
                    break;
                case "편안한":
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

        return true;
    }

    // 특정 카테고리의 보강된 후보지 조회
    @Transactional(readOnly = true)
    public List<TravelCandidate> getEnrichedCandidatesByCategory(String region, String category) {
        log.info("Fetching enriched candidates for region: {} and category: {}", region, category);

        return travelCandidateRepository
            .findByRegionAndCategoryAndIsActiveTrue(region, category)
            .stream()
            .filter(c -> c.getGooglePlaceId() != null || c.getAiEnriched() != null)
            .sorted((a, b) -> Double.compare(
                b.getQualityScore() != null ? b.getQualityScore() : 0,
                a.getQualityScore() != null ? a.getQualityScore() : 0
            ))
            .collect(Collectors.toList());
    }

    // 시간 블록별 보강된 후보지 조회
    @Transactional(readOnly = true)
    public List<TravelCandidate> getEnrichedCandidatesByTimeBlock(
            String region,
            TravelCandidate.TimeBlock timeBlock) {

        log.info("Fetching enriched candidates for region: {} and time block: {}",
            region, timeBlock.getKoreanName());

        return travelCandidateRepository
            .findByRegionAndTimeBlockAndIsActiveTrue(region, timeBlock)
            .stream()
            .filter(c -> c.getGooglePlaceId() != null || c.getAiEnriched() != null)
            .sorted((a, b) -> Double.compare(
                b.getQualityScore() != null ? b.getQualityScore() : 0,
                a.getQualityScore() != null ? a.getQualityScore() : 0
            ))
            .collect(Collectors.toList());
    }

    // 품질 점수 기준 상위 N개 조회
    @Transactional(readOnly = true)
    public List<TravelCandidate> getTopEnrichedCandidates(String region, int limit) {
        return getEnrichedCandidates(region).stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    // 신뢰도 레벨별 조회
    @Transactional(readOnly = true)
    public List<TravelCandidate> getHighReliabilityCandidates(String region) {
        return getEnrichedCandidates(region).stream()
            .filter(c -> "매우높음".equals(c.getReliabilityLevel()) ||
                        "높음".equals(c.getReliabilityLevel()))
            .collect(Collectors.toList());
    }
}