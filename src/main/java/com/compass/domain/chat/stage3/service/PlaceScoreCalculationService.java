package com.compass.domain.chat.stage3.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceScoreCalculationService {

    // Google Places Enhanced 데이터 기반 스코어링
    public List<TravelPlace> calculateScores(
            List<TravelCandidate> candidates,
            String travelStyle,
            String travelCompanion) {

        log.info("Calculating scores for {} candidates with style: {} and companion: {}",
            candidates.size(), travelStyle, travelCompanion);

        return candidates.stream()
            .map(candidate -> {
                double score = calculatePlaceScore(candidate, travelStyle, travelCompanion);
                TravelPlace place = candidate.toTravelPlace();
                // 스코어를 description에 추가
                place.setDescription(String.format("%s (Score: %.2f)",
                    place.getDescription() != null ? place.getDescription() : "", score));
                return place;
            })
            .sorted((a, b) -> {
                // 품질 점수로 정렬
                double scoreA = extractScore(a.getDescription());
                double scoreB = extractScore(b.getDescription());
                return Double.compare(scoreB, scoreA);
            })
            .collect(Collectors.toList());
    }

    // OCR 확정 일정을 고려한 스코어링
    public List<TravelPlace> calculateScoresWithConstraints(
            List<TravelCandidate> candidates,
            String travelStyle,
            String travelCompanion,
            List<ConfirmedSchedule> confirmedSchedules) {

        log.info("Calculating scores with {} confirmed schedules", confirmedSchedules.size());

        // 확정 일정 근처 장소에 가중치 부여
        Set<String> confirmedLocations = confirmedSchedules.stream()
            .map(ConfirmedSchedule::address)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return candidates.stream()
            .map(candidate -> {
                double baseScore = calculatePlaceScore(candidate, travelStyle, travelCompanion);

                // 확정 일정 근처면 보너스 점수
                if (isNearConfirmedLocation(candidate, confirmedLocations)) {
                    baseScore *= 1.2; // 20% 보너스
                }

                TravelPlace place = candidate.toTravelPlace();
                place.setDescription(String.format("%s (Score: %.2f)",
                    place.getDescription() != null ? place.getDescription() : "", baseScore));
                return place;
            })
            .sorted((a, b) -> {
                double scoreA = extractScore(a.getDescription());
                double scoreB = extractScore(b.getDescription());
                return Double.compare(scoreB, scoreA);
            })
            .collect(Collectors.toList());
    }

    // 개별 장소 스코어 계산
    private double calculatePlaceScore(TravelCandidate candidate, String travelStyle, String travelCompanion) {
        double score = 0.0;

        // 1. 품질 점수 (40%)
        if (candidate.getQualityScore() != null) {
            score += candidate.getQualityScore() * 0.4;
        }

        // 2. 여행 스타일 매칭 (30%)
        score += calculateStyleMatch(candidate, travelStyle) * 0.3;

        // 3. 동행자 적합성 (20%)
        score += calculateCompanionMatch(candidate, travelCompanion) * 0.2;

        // 4. Google Places 메타데이터 활용 (10%)
        score += calculateMetadataScore(candidate) * 0.1;

        return score;
    }

    // 여행 스타일 매칭 스코어
    private double calculateStyleMatch(TravelCandidate candidate, String travelStyle) {
        if (travelStyle == null) return 0.5;

        String category = candidate.getCategory();
        return switch (travelStyle.toLowerCase()) {
            case "휴양", "relax", "rest" ->
                category.contains("카페") || category.contains("공원") || category.contains("스파") ? 1.0 : 0.3;
            case "관광", "tour", "sightseeing" ->
                category.contains("관광") || category.contains("명소") || category.contains("박물관") ? 1.0 : 0.5;
            case "미식", "food", "gourmet" ->
                category.contains("맛집") || category.contains("레스토랑") || category.contains("카페") ? 1.0 : 0.3;
            case "액티비티", "activity", "adventure" ->
                category.contains("체험") || category.contains("스포츠") || category.contains("액티비티") ? 1.0 : 0.4;
            case "쇼핑", "shopping" ->
                category.contains("쇼핑") || category.contains("시장") || category.contains("백화점") ? 1.0 : 0.3;
            default -> 0.5;
        };
    }

    // 동행자 적합성 스코어
    private double calculateCompanionMatch(TravelCandidate candidate, String travelCompanion) {
        if (travelCompanion == null) return 0.5;

        // 가족 여행
        if (travelCompanion.contains("가족") || travelCompanion.contains("family")) {
            if (Boolean.TRUE.equals(candidate.getWheelchairAccessible()) ||
                Boolean.TRUE.equals(candidate.getParkingAvailable())) {
                return 1.0;
            }
            // 가격대가 중간 정도면 적합
            if (candidate.getPriceLevel() != null && candidate.getPriceLevel() <= 2) {
                return 0.8;
            }
        }

        // 연인 여행
        if (travelCompanion.contains("연인") || travelCompanion.contains("couple")) {
            String category = candidate.getCategory();
            if (category.contains("카페") || category.contains("야경") ||
                category.contains("레스토랑")) {
                return 1.0;
            }
        }

        // 친구 여행
        if (travelCompanion.contains("친구") || travelCompanion.contains("friend")) {
            if (candidate.getCategory().contains("액티비티") ||
                candidate.getCategory().contains("맛집")) {
                return 1.0;
            }
        }

        // 혼자 여행
        if (travelCompanion.contains("혼자") || travelCompanion.contains("solo")) {
            // 와이파이가 있으면 좋음
            if (Boolean.TRUE.equals(candidate.getWifiAvailable())) {
                return 0.9;
            }
        }

        return 0.5;
    }

    // Google Places 메타데이터 점수
    private double calculateMetadataScore(TravelCandidate candidate) {
        double score = 0.0;
        int factors = 0;

        // AI로 보강된 데이터면 가점
        if (Boolean.TRUE.equals(candidate.getAiEnriched())) {
            score += 1.0;
            factors++;
        }

        // 영업 시간 정보가 있으면 가점
        if (candidate.getBusinessHours() != null && !candidate.getBusinessHours().isEmpty()) {
            score += 0.8;
            factors++;
        }

        // 사진이 있으면 가점
        if (candidate.getPhotoUrl() != null && !candidate.getPhotoUrl().isEmpty()) {
            score += 0.7;
            factors++;
        }

        // 하이라이트 정보가 있으면 가점
        if (candidate.getHighlights() != null && !candidate.getHighlights().isEmpty()) {
            score += 0.9;
            factors++;
        }

        return factors > 0 ? score / factors : 0.5;
    }

    // 확정 일정 근처 여부 확인
    private boolean isNearConfirmedLocation(TravelCandidate candidate, Set<String> confirmedLocations) {
        if (candidate.getAddress() == null) return false;

        // 주소에 같은 지역명이 포함되어 있으면 근처로 판단
        return confirmedLocations.stream()
            .anyMatch(confirmedAddr -> {
                String[] candidateTokens = candidate.getAddress().split(" ");
                String[] confirmedTokens = confirmedAddr.split(" ");

                // 구/동 레벨에서 일치하면 근처로 판단
                for (String cToken : candidateTokens) {
                    for (String fToken : confirmedTokens) {
                        if (cToken.length() > 1 && fToken.length() > 1 &&
                            (cToken.contains(fToken) || fToken.contains(cToken))) {
                            return true;
                        }
                    }
                }
                return false;
            });
    }

    // Description에서 스코어 추출
    private double extractScore(String description) {
        if (description == null) return 0.0;

        try {
            int start = description.lastIndexOf("Score: ");
            if (start != -1) {
                int end = description.indexOf(")", start);
                if (end != -1) {
                    String scoreStr = description.substring(start + 7, end);
                    return Double.parseDouble(scoreStr);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract score from description: {}", description);
        }
        return 0.0;
    }
}