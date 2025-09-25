package com.compass.domain.chat.function.planning;

import com.compass.domain.chat.entity.TourPlace;
import com.compass.domain.chat.model.request.TravelPlanRequest;
import com.compass.domain.chat.model.response.TravelPlanResponse;
import com.compass.domain.chat.service.PlaceFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// 여행 계획 생성 함수
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateTravelPlanFunction implements Function<TravelPlanRequest, TravelPlanResponse> {

    private final PlaceFilterService placeFilterService;

    @Override
    public TravelPlanResponse apply(TravelPlanRequest request) {
        log.info("여행 계획 생성 시작");

        try {
            // 병렬 데이터 수집
            var dbPlaces = searchInDatabase();
            var trendyPlaces = searchTrendyPlaces();
            var weather = getWeatherInfo();

            // 모든 데이터 수집 완료 대기
            CompletableFuture.allOf(dbPlaces, trendyPlaces, weather).join();

            // 일정 생성
            var plan = generateItinerary(
                dbPlaces.join(),
                trendyPlaces.join(), 
                weather.join()
            );

            return createResponse(plan);

        } catch (Exception e) {
            log.error("여행 계획 생성 실패", e);
            return createErrorResponse();
        }
    }

    // DB에서 기존 장소 정보 검색
    private CompletableFuture<List<TourPlace>> searchInDatabase() {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: 실제 DB에서 장소 검색 구현
            // 현재는 빈 리스트 반환
            return List.<TourPlace>of();
        });
    }

    // Perplexity로 트렌디한 장소 검색
    private CompletableFuture<List<TourPlace>> searchTrendyPlaces() {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Perplexity API로 트렌디한 장소 검색 구현
            // 현재는 빈 리스트 반환
            return List.<TourPlace>of();
        });
    }

    // 날씨 정보 조회
    private CompletableFuture<Object> getWeatherInfo() {
        return CompletableFuture.supplyAsync(() -> {
            return new Object();
        });
    }

    // Stage 1: 클러스터링을 적용한 장소 선별 및 일정 생성
    private Object generateItinerary(List<TourPlace> dbPlaces, List<TourPlace> trendyPlaces, Object weather) {
        log.info("Stage 1: 장소 선별 및 일정 생성 시작");
        
        // 1. 모든 장소 통합
        List<TourPlace> allPlaces = new ArrayList<>();
        allPlaces.addAll(dbPlaces);
        allPlaces.addAll(trendyPlaces);
        
        // 2. 사용자 선호도 추출 (request에서)
        // TODO: TravelPlanRequest에서 사용자 선호도 정보 추출
        
        // 3. 클러스터링 적용된 장소 선별
        // TODO: 실제 사용자 선호도 정보를 사용하여 클러스터링 적용
        // List<TourPlace> selectedPlaces = placeFilterService.filterByPreferencesWithClustering(
        //     allPlaces, userPreferences);
        
        log.info("Stage 1 완료: {} 개 장소에서 선별 완료", allPlaces.size());
        
        return new Object(); // 임시 반환
    }

    // 성공 응답 생성
    private TravelPlanResponse createResponse(Object plan) {
        return TravelPlanResponse.success("", plan, 0);
    }

    // 에러 응답 생성
    private TravelPlanResponse createErrorResponse() {
        return TravelPlanResponse.error("여행 계획 생성 중 오류가 발생했습니다");
    }
}
