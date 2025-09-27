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

    // Google Places Enhanced 데이터로 보강된 후보지 조회
    @Transactional(readOnly = true)
    public List<TravelCandidate> getEnrichedCandidates(String region) {
        log.info("Fetching enriched candidates for region: {}", region);

        List<TravelCandidate> candidates = travelCandidateRepository
            .findByRegionAndIsActiveTrue(region);

        // Google Places 데이터가 있는 것들 우선
        List<TravelCandidate> enrichedCandidates = candidates.stream()
            .filter(c -> c.getGooglePlaceId() != null || c.getAiEnriched() != null)
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

        log.info("Found {} enriched candidates out of {} total",
            enrichedCandidates.size(), candidates.size());

        return enrichedCandidates;
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