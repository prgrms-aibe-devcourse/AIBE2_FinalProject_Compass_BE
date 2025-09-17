package com.compass.domain.chat.function.planning;

import com.compass.domain.chat.model.request.TravelPlanRequest;
import com.compass.domain.chat.model.response.TravelPlanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// 여행 계획 생성 함수
@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateTravelPlanFunction implements Function<TravelPlanRequest, TravelPlanResponse> {

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
    private CompletableFuture<List<Object>> searchInDatabase() {
        return CompletableFuture.supplyAsync(() -> {
            return List.of();
        });
    }

    // Perplexity로 트렌디한 장소 검색
    private CompletableFuture<List<Object>> searchTrendyPlaces() {
        return CompletableFuture.supplyAsync(() -> {
            return List.of();
        });
    }

    // 날씨 정보 조회
    private CompletableFuture<Object> getWeatherInfo() {
        return CompletableFuture.supplyAsync(() -> {
            return new Object();
        });
    }

    // LLM으로 일정 생성
    private Object generateItinerary(List<Object> dbPlaces, List<Object> trendyPlaces, Object weather) {
        return new Object();
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
