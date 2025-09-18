package com.compass.domain.chat.service;

import com.compass.domain.chat.function.external.SearchWithPerplexityFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// 장소 선별 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSelectionService {

    private final SearchWithPerplexityFunction perplexitySearch;

    // 목적지 중심 장소 검색 및 선별
    public List<TourPlace> selectPlaces(String destination, TravelInfo info) {
        log.info("장소 선별 시작: {}", destination);

        try {
            // 병렬로 장소 수집
            var perplexityPlaces = searchPerplexityPlaces(destination);
            var tourAPIPlaces = searchTourAPIPlaces(destination);

            // 모든 검색 완료 대기
            CompletableFuture.allOf(perplexityPlaces, tourAPIPlaces).join();

            // 선호도 기반 장소 선별
            return filterByPreferences(
                combinePlaces(perplexityPlaces.join(), tourAPIPlaces.join()), 
                info.travelStyle()
            );

        } catch (Exception e) {
            log.error("장소 선별 실패: {}", destination, e);
            return List.of();
        }
    }

    // Perplexity로 장소 검색
    private CompletableFuture<List<Object>> searchPerplexityPlaces(String destination) {
        return CompletableFuture.supplyAsync(() -> {
            var query = new SearchWithPerplexityFunction.SearchQuery(
                destination + " 여행지",
                destination,
                "관광지"
            );
            return List.of((Object) perplexitySearch.apply(query));
        });
    }

    // Tour API로 장소 검색
    private CompletableFuture<List<Object>> searchTourAPIPlaces(String destination) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: SearchTourAPIFunction 구현 후 연동
            return List.of();
        });
    }

    // 장소 목록 결합
    private List<Object> combinePlaces(List<Object> perplexityPlaces, List<Object> tourAPIPlaces) {
        return List.of(); // TODO: 실제 결합 로직 구현
    }

    // 선호도 기반 필터링
    private List<TourPlace> filterByPreferences(List<Object> allPlaces, String travelStyle) {
        // TODO: 여행 스타일에 맞는 장소 필터링
        return List.of();
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
        Double rating          // 평점
    ) {}
}
