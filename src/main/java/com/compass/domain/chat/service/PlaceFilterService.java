package com.compass.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 장소 필터링 서비스
 * 
 * 사용자 선호도에 따른 장소 필터링을 담당합니다.
 * - 예산 범위 필터링
 * - 여행 스타일 매칭
 * - 선호 카테고리 필터링
 * - 운영 시간 및 휴무일 체크
 * - 점수 기반 정렬
 */
@Slf4j
@Service
public class PlaceFilterService {

    // 예산 범위별 가격대 매핑
    private static final Map<Budget, Set<String>> BUDGET_PRICE_MAPPING = Map.of(
        Budget.LOW, Set.of("FREE", "$"),
        Budget.MEDIUM, Set.of("FREE", "$", "$$"),
        Budget.HIGH, Set.of("FREE", "$", "$$", "$$$"),
        Budget.UNLIMITED, Set.of("FREE", "$", "$$", "$$$", "$$$$")
    );

    // 여행 스타일별 선호 카테고리 매핑
    private static final Map<TravelStyle, Set<String>> STYLE_CATEGORY_MAPPING = Map.of(
        TravelStyle.RELAXATION, Set.of("카페", "공원", "스파", "해변", "온천", "휴양지"),
        TravelStyle.ADVENTURE, Set.of("액티비티", "등산", "체험", "스포츠", "모험", "아웃도어"),
        TravelStyle.CULTURAL, Set.of("박물관", "미술관", "전통", "역사", "문화재", "궁궐", "사찰"),
        TravelStyle.FOODIE, Set.of("맛집", "시장", "카페", "베이커리", "전통음식", "로컬푸드"),
        TravelStyle.SHOPPING, Set.of("쇼핑", "백화점", "아울렛", "시장", "면세점", "브랜드"),
        TravelStyle.NATURE, Set.of("산", "바다", "공원", "자연", "숲", "계곡", "폭포", "해변")
    );

    /**
     * 사용자 선호도 기반 필터링
     * 
     * @param places 원본 장소 리스트
     * @param preferences 사용자 선호도
     * @return 필터링된 장소 리스트
     */
    public List<PlaceDeduplicator.TourPlace> filterByPreferences(
        List<PlaceDeduplicator.TourPlace> places,
        UserPreferences preferences
    ) {
        if (places == null || places.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("장소 필터링 시작: {} 개 장소, 스타일: {}, 예산: {}", 
                places.size(), preferences.travelStyle(), preferences.budget());

        List<PlaceDeduplicator.TourPlace> filtered = places.stream()
            // 1. 예산 범위 필터링
            .filter(place -> matchesBudget(place, preferences.budget()))
            // 2. 여행 스타일 매칭
            .filter(place -> matchesStyle(place, preferences.travelStyle()))
            // 3. 선호 카테고리 필터링
            .filter(place -> matchesCategories(place, preferences.preferredCategories()))
            // 4. 운영시간 확인
            .filter(place -> isOperatingDuringTrip(place, preferences.travelDates()))
            // 5. 점수 기반 정렬
            .sorted(this::compareByScore)
            // 6. 여행 일수에 따른 개수 제한
            .limit(calculateMaxPlaces(preferences.tripDays()))
            .collect(Collectors.toList());

        log.info("장소 필터링 완료: {} -> {} 개 장소 ({}% 필터링)", 
                places.size(), filtered.size(), 
                Math.round((1.0 - (double)filtered.size() / places.size()) * 100));

        return filtered;
    }

    /**
     * 예산 범위 체크
     */
    private boolean matchesBudget(PlaceDeduplicator.TourPlace place, Budget budget) {
        if (budget == null || place.priceRange() == null) {
            return true; // 정보가 없으면 통과
        }

        Set<String> allowedPrices = BUDGET_PRICE_MAPPING.get(budget);
        return allowedPrices.contains(place.priceRange());
    }

    /**
     * 여행 스타일 매칭
     */
    private boolean matchesStyle(PlaceDeduplicator.TourPlace place, TravelStyle style) {
        if (style == null || place.category() == null) {
            return true; // 정보가 없으면 통과
        }

        Set<String> preferredCategories = STYLE_CATEGORY_MAPPING.get(style);
        
        // 카테고리가 선호 카테고리와 매칭되는지 확인
        return preferredCategories.stream()
                .anyMatch(preferred -> place.category().toLowerCase().contains(preferred.toLowerCase()));
    }

    /**
     * 선호 카테고리 필터링
     */
    private boolean matchesCategories(PlaceDeduplicator.TourPlace place, List<String> preferredCategories) {
        if (preferredCategories == null || preferredCategories.isEmpty() || place.category() == null) {
            return true; // 선호도가 없거나 카테고리 정보가 없으면 통과
        }

        String category = place.category().toLowerCase();
        return preferredCategories.stream()
                .anyMatch(preferred -> category.contains(preferred.toLowerCase()));
    }

    /**
     * 운영 시간 확인
     */
    private boolean isOperatingDuringTrip(PlaceDeduplicator.TourPlace place, DateRange dates) {
        if (dates == null || place.operatingHours() == null) {
            return true; // 정보가 없으면 통과
        }

        // 간단한 운영시간 체크 (24시간 운영이거나 일반적인 운영시간)
        String hours = place.operatingHours().toLowerCase();
        
        // 24시간 운영
        if (hours.contains("24시간") || hours.contains("00:00-24:00")) {
            return true;
        }
        
        // 휴무일 체크
        if (hours.contains("휴무") || hours.contains("closed")) {
            // 특정 요일 휴무인 경우 여행 날짜와 겹치는지 확인
            // 간단히 월요일 휴무만 체크
            if (hours.contains("월요일") || hours.contains("monday")) {
                return !dates.containsMonday(); // 월요일이 포함되지 않으면 OK
            }
        }
        
        return true; // 기본적으로 운영한다고 가정
    }

    /**
     * 장소 점수 계산 (정렬용)
     */
    private int compareByScore(PlaceDeduplicator.TourPlace p1, PlaceDeduplicator.TourPlace p2) {
        double score1 = calculateScore(p1);
        double score2 = calculateScore(p2);
        return Double.compare(score2, score1);  // 내림차순 (높은 점수 우선)
    }

    /**
     * 장소 점수 계산
     */
    private double calculateScore(PlaceDeduplicator.TourPlace place) {
        double score = 0.0;

        // 평점 (40% 가중치)
        if (place.rating() != null) {
            score += place.rating() * 0.4;
        } else {
            score += 3.5 * 0.4; // 기본 평점
        }

        // 정보 완성도 (30% 가중치)
        double completeness = calculateCompleteness(place);
        score += completeness * 0.3;

        // 카테고리 인기도 (20% 가중치)
        double categoryScore = getCategoryPopularity(place.category());
        score += categoryScore * 0.2;

        // 태그 다양성 (10% 가중치)
        double tagScore = place.tags() != null ? Math.min(place.tags().size() / 5.0, 1.0) : 0.0;
        score += tagScore * 0.1;

        return score;
    }

    /**
     * 정보 완성도 계산
     */
    private double calculateCompleteness(PlaceDeduplicator.TourPlace place) {
        int totalFields = 8; // 주요 필드 개수
        int filledFields = 0;

        if (place.name() != null && !place.name().trim().isEmpty()) filledFields++;
        if (place.address() != null && !place.address().trim().isEmpty()) filledFields++;
        if (place.latitude() != null && place.longitude() != null) filledFields++;
        if (place.category() != null && !place.category().trim().isEmpty()) filledFields++;
        if (place.rating() != null) filledFields++;
        if (place.description() != null && !place.description().trim().isEmpty()) filledFields++;
        if (place.operatingHours() != null && !place.operatingHours().trim().isEmpty()) filledFields++;
        if (place.tags() != null && !place.tags().isEmpty()) filledFields++;

        return (double) filledFields / totalFields;
    }

    /**
     * 카테고리별 인기도 점수
     */
    private double getCategoryPopularity(String category) {
        if (category == null) return 0.5;

        String cat = category.toLowerCase();
        
        // 인기 카테고리 순으로 점수 부여
        if (cat.contains("관광지") || cat.contains("명소")) return 1.0;
        if (cat.contains("맛집") || cat.contains("음식")) return 0.9;
        if (cat.contains("카페") || cat.contains("디저트")) return 0.8;
        if (cat.contains("쇼핑") || cat.contains("시장")) return 0.7;
        if (cat.contains("문화") || cat.contains("박물관")) return 0.8;
        if (cat.contains("자연") || cat.contains("공원")) return 0.7;
        if (cat.contains("체험") || cat.contains("액티비티")) return 0.6;
        
        return 0.5; // 기본 점수
    }

    /**
     * 여행 일수에 따른 최대 장소 수 계산
     */
    private int calculateMaxPlaces(int tripDays) {
        // 개발자 가이드에 따른 일수별 장소 수
        return switch (tripDays) {
            case 1 -> 8;   // 당일치기: 5-8개
            case 2 -> 15;  // 1박2일: 10-15개
            case 3 -> 20;  // 2박3일: 15-20개
            default -> 30; // 기본값
        };
    }

    // ========== 내부 클래스 및 Enum ==========

    /**
     * 사용자 선호도 정보
     */
    public record UserPreferences(
        TravelStyle travelStyle,        // 여행 스타일
        Budget budget,                  // 예산 범위
        List<String> preferredCategories, // 선호 카테고리
        DateRange travelDates,          // 여행 날짜
        int tripDays,                   // 여행 일수
        int numberOfPeople              // 인원 수
    ) {}

    /**
     * 여행 스타일 Enum
     */
    public enum TravelStyle {
        RELAXATION,  // 휴양/힐링
        ADVENTURE,   // 모험/액티비티
        CULTURAL,    // 문화/역사
        FOODIE,      // 맛집/음식
        SHOPPING,    // 쇼핑
        NATURE       // 자연/생태
    }

    /**
     * 예산 범위 Enum
     */
    public enum Budget {
        LOW,         // 저예산 (FREE, $)
        MEDIUM,      // 중간예산 (FREE, $, $$)
        HIGH,        // 고예산 (FREE, $, $$, $$$)
        UNLIMITED    // 무제한 (모든 가격대)
    }

    /**
     * 날짜 범위 클래스
     */
    public static class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;

        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public DateRange(String startDateStr, String endDateStr) {
            this.startDate = LocalDate.parse(startDateStr);
            this.endDate = LocalDate.parse(endDateStr);
        }

        public boolean containsMonday() {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (current.getDayOfWeek().getValue() == 1) { // 월요일
                    return true;
                }
                current = current.plusDays(1);
            }
            return false;
        }

        public List<LocalDate> getAllDates() {
            List<LocalDate> dates = new ArrayList<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                dates.add(current);
                current = current.plusDays(1);
            }
            return dates;
        }

        public int getDays() {
            return (int) startDate.until(endDate).getDays() + 1;
        }

        // Getters
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }
}
