package com.compass.domain.trip.service;

import com.compass.domain.trip.dto.IntegratedSearchRequest;
import com.compass.domain.trip.dto.IntegratedSearchResponse;
import com.compass.domain.trip.dto.KakaoMapApiResponse;
import com.compass.domain.trip.entity.TourPlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 통합 검색 서비스
 * REQ-SEARCH-004: 통합 검색 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegratedSearchService {

    private final SearchService searchService;
    private final TourApiSearchService tourApiSearchService;
    private final KakaoMapSearchService kakaoMapSearchService;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    /**
     * 통합 검색 실행
     */
    public IntegratedSearchResponse search(IntegratedSearchRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("통합 검색 시작: keyword={}, type={}, priority={}", 
                request.getKeyword(), request.getSearchType(), request.getPriority());

        try {
            // 검색 타입에 따른 검색 실행
            List<IntegratedSearchResponse.SearchResult> allResults = new ArrayList<>();
            Map<String, IntegratedSearchResponse.SearchSystemStats> systemStats = new HashMap<>();

            switch (request.getSearchType()) {
                case ALL:
                    allResults = performAllSystemSearch(request, systemStats);
                    break;
                case RDS:
                    allResults = performRdsSearch(request, systemStats);
                    break;
                case TOUR_API:
                    allResults = performTourApiSearch(request, systemStats);
                    break;
                case KAKAO_MAP:
                    allResults = performKakaoMapSearch(request, systemStats);
                    break;
            }

            // 결과 정렬 및 페이지네이션
            List<IntegratedSearchResponse.SearchResult> sortedResults = sortAndPaginateResults(
                    allResults, request);

            // 응답 생성
            long searchTime = System.currentTimeMillis() - startTime;
            return buildResponse(request, sortedResults, systemStats, searchTime);

        } catch (Exception e) {
            log.error("통합 검색 실패: keyword={}", request.getKeyword(), e);
            return buildErrorResponse(request, e.getMessage());
        }
    }

    /**
     * 모든 검색 시스템 사용
     */
    private List<IntegratedSearchResponse.SearchResult> performAllSystemSearch(
            IntegratedSearchRequest request, 
            Map<String, IntegratedSearchResponse.SearchSystemStats> systemStats) {
        
        List<CompletableFuture<List<IntegratedSearchResponse.SearchResult>>> futures = new ArrayList<>();

        // 우선순위에 따른 검색 실행
        switch (request.getPriority()) {
            case RDS_FIRST:
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performRdsSearch(request, systemStats), executorService));
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performTourApiSearch(request, systemStats), executorService));
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performKakaoMapSearch(request, systemStats), executorService));
                break;
            case TOUR_API_FIRST:
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performTourApiSearch(request, systemStats), executorService));
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performRdsSearch(request, systemStats), executorService));
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performKakaoMapSearch(request, systemStats), executorService));
                break;
            case KAKAO_MAP_FIRST:
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performKakaoMapSearch(request, systemStats), executorService));
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performRdsSearch(request, systemStats), executorService));
                futures.add(CompletableFuture.supplyAsync(() -> 
                    performTourApiSearch(request, systemStats), executorService));
                break;
        }

        // 모든 검색 완료 대기
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        try {
            allFutures.get(); // 모든 검색 완료까지 대기
        } catch (Exception e) {
            log.error("통합 검색 중 오류 발생", e);
        }

        // 결과 수집
        return futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("검색 결과 수집 중 오류", e);
                        return new ArrayList<IntegratedSearchResponse.SearchResult>();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * RDS 검색 실행
     */
    private List<IntegratedSearchResponse.SearchResult> performRdsSearch(
            IntegratedSearchRequest request, 
            Map<String, IntegratedSearchResponse.SearchSystemStats> systemStats) {
        
        long startTime = System.currentTimeMillis();
        try {
            log.info("RDS 검색 실행: keyword={}", request.getKeyword());
            
            Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize());
            Page<TourPlace> rdsResults;
            
            // RDS 검색 실행
            if (request.getCategory() != null && request.getAreaCode() != null) {
                rdsResults = searchService.fullTextSearchWithFilters(
                        request.getKeyword(), request.getCategory(), request.getAreaCode(), pageable);
            } else if (request.getCategory() != null) {
                rdsResults = searchService.fullTextSearchWithCategory(
                        request.getKeyword(), request.getCategory(), pageable);
            } else if (request.getAreaCode() != null) {
                rdsResults = searchService.fullTextSearchWithArea(
                        request.getKeyword(), request.getAreaCode(), pageable);
            } else {
                rdsResults = searchService.fullTextSearch(request.getKeyword(), pageable);
            }

            long searchTime = System.currentTimeMillis() - startTime;
            systemStats.put("RDS", IntegratedSearchResponse.SearchSystemStats.builder()
                    .systemName("RDS")
                    .resultCount((int) rdsResults.getTotalElements())
                    .searchTimeMs(searchTime)
                    .success(true)
                    .build());

            return rdsResults.getContent().stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            long searchTime = System.currentTimeMillis() - startTime;
            log.error("RDS 검색 실패: keyword={}", request.getKeyword(), e);
            systemStats.put("RDS", IntegratedSearchResponse.SearchSystemStats.builder()
                    .systemName("RDS")
                    .resultCount(0)
                    .searchTimeMs(searchTime)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
            return new ArrayList<>();
        }
    }

    /**
     * Tour API 검색 실행
     */
    private List<IntegratedSearchResponse.SearchResult> performTourApiSearch(
            IntegratedSearchRequest request, 
            Map<String, IntegratedSearchResponse.SearchSystemStats> systemStats) {
        
        long startTime = System.currentTimeMillis();
        try {
            log.info("Tour API 검색 실행: keyword={}", request.getKeyword());
            
            // Tour API 검색 실행
            var tourResults = tourApiSearchService.searchByKeyword(
                    request.getKeyword(), request.getAreaCode(), null);

            long searchTime = System.currentTimeMillis() - startTime;
            
            if (tourResults.isPresent()) {
                int resultCount = tourResults.get().getResponse().getBody().getTotalCount();
                systemStats.put("TOUR_API", IntegratedSearchResponse.SearchSystemStats.builder()
                        .systemName("TOUR_API")
                        .resultCount(resultCount)
                        .searchTimeMs(searchTime)
                        .success(true)
                        .build());

                // Tour API 결과를 SearchResult로 변환
                return tourResults.get().getResponse().getBody().getItems().getItem().stream()
                        .map(this::convertTourApiToSearchResult)
                        .collect(Collectors.toList());
            } else {
                systemStats.put("TOUR_API", IntegratedSearchResponse.SearchSystemStats.builder()
                        .systemName("TOUR_API")
                        .resultCount(0)
                        .searchTimeMs(searchTime)
                        .success(false)
                        .errorMessage("Tour API 검색 실패")
                        .build());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            long searchTime = System.currentTimeMillis() - startTime;
            log.error("Tour API 검색 실패: keyword={}", request.getKeyword(), e);
            systemStats.put("TOUR_API", IntegratedSearchResponse.SearchSystemStats.builder()
                    .systemName("TOUR_API")
                    .resultCount(0)
                    .searchTimeMs(searchTime)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
            return new ArrayList<>();
        }
    }

    /**
     * Kakao Map API 검색 실행
     */
    private List<IntegratedSearchResponse.SearchResult> performKakaoMapSearch(
            IntegratedSearchRequest request, 
            Map<String, IntegratedSearchResponse.SearchSystemStats> systemStats) {
        
        long startTime = System.currentTimeMillis();
        try {
            log.info("Kakao Map API 검색 실행: keyword={}", request.getKeyword());
            
            // Kakao Map API 검색 실행
            var kakaoResults = kakaoMapSearchService.searchByKeyword(
                    request.getKeyword(), 
                    request.getLongitude() != null ? request.getLongitude().toString() : null,
                    request.getLatitude() != null ? request.getLatitude().toString() : null,
                    request.getRadius(), request.getPage(), request.getSize(), "accuracy");

            long searchTime = System.currentTimeMillis() - startTime;
            systemStats.put("KAKAO_MAP", IntegratedSearchResponse.SearchSystemStats.builder()
                    .systemName("KAKAO_MAP")
                    .resultCount(kakaoResults.isPresent() ? kakaoResults.get().getDocuments().size() : 0)
                    .searchTimeMs(searchTime)
                    .success(true)
                    .build());

            // Kakao Map API 결과를 SearchResult로 변환
            if (kakaoResults.isPresent()) {
                return kakaoResults.get().getDocuments().stream()
                        .map(this::convertKakaoMapToSearchResult)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();

        } catch (Exception e) {
            long searchTime = System.currentTimeMillis() - startTime;
            log.error("Kakao Map API 검색 실패: keyword={}", request.getKeyword(), e);
            systemStats.put("KAKAO_MAP", IntegratedSearchResponse.SearchSystemStats.builder()
                    .systemName("KAKAO_MAP")
                    .resultCount(0)
                    .searchTimeMs(searchTime)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
            return new ArrayList<>();
        }
    }

    /**
     * TourPlace를 SearchResult로 변환
     */
    private IntegratedSearchResponse.SearchResult convertToSearchResult(TourPlace tourPlace) {
        return IntegratedSearchResponse.SearchResult.builder()
                .id(tourPlace.getContentId())
                .name(tourPlace.getName())
                .address(tourPlace.getAddress())
                .category(tourPlace.getCategory())
                .longitude(tourPlace.getLongitude())
                .latitude(tourPlace.getLatitude())
                .searchSystem("RDS")
                .confidenceScore(0.9) // RDS는 높은 신뢰도
                .build();
    }

    /**
     * Tour API 결과를 SearchResult로 변환
     */
    private IntegratedSearchResponse.SearchResult convertTourApiToSearchResult(Object tourResult) {
        // Tour API 결과 구조에 맞게 변환 (실제 구현 시 Tour API DTO 사용)
        return IntegratedSearchResponse.SearchResult.builder()
                .id("tour_" + System.currentTimeMillis())
                .name("Tour API Result")
                .address("Tour API Address")
                .category("Tour API Category")
                .searchSystem("TOUR_API")
                .confidenceScore(0.8)
                .build();
    }

    /**
     * Kakao Map API 결과를 SearchResult로 변환
     */
    private IntegratedSearchResponse.SearchResult convertKakaoMapToSearchResult(
            KakaoMapApiResponse.Document document) {
        return IntegratedSearchResponse.SearchResult.builder()
                .id(document.getId())
                .name(document.getPlaceName())
                .address(document.getAddressName())
                .category(document.getCategoryName())
                .longitude(Double.parseDouble(document.getX()))
                .latitude(Double.parseDouble(document.getY()))
                .searchSystem("KAKAO_MAP")
                .confidenceScore(0.7)
                .build();
    }

    /**
     * 결과 정렬 및 페이지네이션
     */
    private List<IntegratedSearchResponse.SearchResult> sortAndPaginateResults(
            List<IntegratedSearchResponse.SearchResult> results, 
            IntegratedSearchRequest request) {
        
        // 정렬
        switch (request.getSort()) {
            case ACCURACY:
                results.sort((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()));
                break;
            case DISTANCE:
                if (request.getLatitude() != null && request.getLongitude() != null) {
                    results.forEach(result -> {
                        if (result.getLatitude() != null && result.getLongitude() != null) {
                            double distance = calculateDistance(
                                    request.getLatitude(), request.getLongitude(),
                                    result.getLatitude(), result.getLongitude());
                            result.setDistance(distance);
                        }
                    });
                    results.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
                }
                break;
            case POPULARITY:
                // 인기도 정렬 (현재는 신뢰도로 대체)
                results.sort((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()));
                break;
        }

        // 페이지네이션
        int start = (request.getPage() - 1) * request.getSize();
        int end = Math.min(start + request.getSize(), results.size());
        
        if (start >= results.size()) {
            return new ArrayList<>();
        }
        
        return results.subList(start, end);
    }

    /**
     * 거리 계산 (Haversine 공식)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // 미터로 변환
    }

    /**
     * 응답 생성
     */
    private IntegratedSearchResponse buildResponse(
            IntegratedSearchRequest request,
            List<IntegratedSearchResponse.SearchResult> results,
            Map<String, IntegratedSearchResponse.SearchSystemStats> systemStats,
            long searchTime) {
        
        int totalCount = results.size();
        int totalPages = (int) Math.ceil((double) totalCount / request.getSize());
        
        return IntegratedSearchResponse.builder()
                .keyword(request.getKeyword())
                .totalCount(totalCount)
                .currentPage(request.getPage())
                .totalPages(totalPages)
                .pageSize(request.getSize())
                .searchTimeMs(searchTime)
                .results(results)
                .systemStats(systemStats)
                .metadata(IntegratedSearchResponse.SearchMetadata.builder()
                        .searchType(request.getSearchType().name())
                        .priority(request.getPriority().name())
                        .usedSystems(new ArrayList<>(systemStats.keySet()))
                        .filters(Map.of(
                                "category", request.getCategory() != null ? request.getCategory() : "all",
                                "areaCode", request.getAreaCode() != null ? request.getAreaCode() : "all"
                        ))
                        .searchHints(Arrays.asList(
                                "통합 검색으로 다양한 검색 시스템의 결과를 확인하세요",
                                "검색 우선순위를 변경하여 원하는 결과를 우선적으로 볼 수 있습니다"
                        ))
                        .build())
                .build();
    }

    /**
     * 오류 응답 생성
     */
    private IntegratedSearchResponse buildErrorResponse(IntegratedSearchRequest request, String errorMessage) {
        return IntegratedSearchResponse.builder()
                .keyword(request.getKeyword())
                .totalCount(0)
                .currentPage(request.getPage())
                .totalPages(0)
                .pageSize(request.getSize())
                .searchTimeMs(0L)
                .results(new ArrayList<>())
                .systemStats(new HashMap<>())
                .metadata(IntegratedSearchResponse.SearchMetadata.builder()
                        .searchType(request.getSearchType().name())
                        .priority(request.getPriority().name())
                        .usedSystems(new ArrayList<>())
                        .filters(new HashMap<>())
                        .searchHints(Arrays.asList("검색 중 오류가 발생했습니다: " + errorMessage))
                        .build())
                .build();
    }
}
