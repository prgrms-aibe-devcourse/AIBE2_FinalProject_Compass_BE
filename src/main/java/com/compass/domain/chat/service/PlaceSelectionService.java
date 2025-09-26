package com.compass.domain.chat.service;

import com.compass.domain.chat.function.external.SearchWithPerplexityFunction;
import com.compass.domain.chat.function.external.SearchGooglePlacesFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// 장소 선별 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSelectionService {

    private final SearchWithPerplexityFunction perplexitySearch;
    private final SearchGooglePlacesFunction googlePlacesSearch;

    // 목적지 중심 장소 검색 및 선별
    public List<TourPlace> selectPlaces(String destination, TravelInfo info) {
        log.info("장소 선별 시작: {}", destination);

        try {
            // 병렬로 장소 수집
            var perplexityPlaces = searchPerplexityPlaces(destination);
            var googlePlaces = searchGooglePlaces(destination);

            // 모든 검색 완료 대기
            CompletableFuture.allOf(perplexityPlaces, googlePlaces).join();

            // 선호도 기반 장소 선별
            return filterByPreferences(
                combinePlaces(perplexityPlaces.join(), googlePlaces.join()), 
                info.travelStyle()
            );

        } catch (Exception e) {
            log.error("장소 선별 실패: {}", destination, e);
            return List.of();
        }
    }

    // Perplexity로 장소 검색
    private CompletableFuture<List<TourPlace>> searchPerplexityPlaces(String destination) {
        return CompletableFuture.supplyAsync(() -> {
            var query = new SearchWithPerplexityFunction.SearchQuery(
                destination + " 여행지",
                destination,
                "관광지"
            );
            return perplexitySearch.apply(query).stream()
                .map(result -> {
                    var address = result.address();
                    var resolvedAddress = address == null || address.isBlank()
                        ? result.location()
                        : address;
                    var rating = result.rating() == null ? 4.0 : result.rating();
                    return new TourPlace(result.name(), resolvedAddress, "Perplexity 추천", rating, result.tips());
                })
                .toList();
        });
    }

    // Google Places로 장소 검색
    private CompletableFuture<List<TourPlace>> searchGooglePlaces(String destination) {
        var query = new SearchGooglePlacesFunction.Query(
            destination + " 관광지",
            destination,
            "관광지"
        );

        return googlePlacesSearch.searchAsync(query)
            .thenApply(results -> results.stream()
                .map(place -> {
                    var address = place.address() == null || place.address().isBlank()
                        ? destination
                        : place.address();
                    var rating = place.rating() == null ? 4.2 : place.rating();
                    return new TourPlace(place.name(), address, place.category(), rating, null);
                })
                .toList());
    }

    // 장소 목록 결합
    private List<TourPlace> combinePlaces(List<TourPlace> perplexityPlaces, List<TourPlace> googlePlaces) {
        Map<String, TourPlace> combined = new LinkedHashMap<>();
        addUnique(combined, perplexityPlaces);
        addUnique(combined, googlePlaces);
        return combined.values().stream().toList();
    }

    // 선호도 기반 필터링
    private List<TourPlace> filterByPreferences(List<TourPlace> allPlaces, String travelStyle) {
        if (allPlaces.isEmpty()) {
            return List.of();
        }
        if (travelStyle == null || travelStyle.isBlank()) {
            return limitResults(allPlaces);
        }
        var style = travelStyle.toLowerCase();
        var filtered = allPlaces.stream()
            .filter(place -> matchesStyle(place, style))
            .toList();
        return filtered.isEmpty() ? limitResults(allPlaces) : limitResults(filtered);
    }

    private void addUnique(Map<String, TourPlace> combined, List<TourPlace> source) {
        source.forEach(place -> combined.putIfAbsent(normalizeKey(place), place));
    }

    private String normalizeKey(TourPlace place) {
        var name = place.name() == null ? "" : place.name();
        var address = place.address() == null ? "" : place.address();
        return (name + "|" + address).toLowerCase();
    }

    private boolean matchesStyle(TourPlace place, String style) {
        var category = place.category() == null ? "" : place.category().toLowerCase();
        if (style.contains("힐링") || style.contains("휴식")) {
            return category.contains("관광") || category.contains("공원");
        }
        if (style.contains("문화") || style.contains("역사")) {
            return category.contains("문화") || category.contains("역사");
        }
        if (style.contains("맛집") || style.contains("미식")) {
            return category.contains("음식") || category.contains("쇼핑");
        }
        if (style.contains("액티비티") || style.contains("모험")) {
            return category.contains("레포츠") || category.contains("기타");
        }
        return true;
    }

    private List<TourPlace> limitResults(List<TourPlace> places) {
        return places.stream().limit(10).toList();
    }

    // 여행 정보 Record
    public record TravelInfo(
        String travelStyle,      // 여행 스타일
        Integer budget,          // 예산
        Integer companions       // 동행자 수
    ) {}

    // 관광지 Record
    public record TourPlace(
        String name,            // 장소명
        String address,         // 주소
        String category,        // 카테고리
        Double rating,          // 평점
        String tips             // 꿀팁/추천 포인트
    ) {}
}
