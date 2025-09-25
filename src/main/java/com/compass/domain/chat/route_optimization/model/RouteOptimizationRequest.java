package com.compass.domain.chat.route_optimization.model;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.DateSelectionOutput;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.DailyItinerary;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record RouteOptimizationRequest(
    DateSelectionOutput dateSelectionOutput,  // 날짜별 선별에서 받은 전체 출력
    String optimizationStrategy, // DISTANCE, TIME, BALANCED
    String transportMode,        // CAR, PUBLIC_TRANSPORT
    String accommodationAddress,
    List<ConfirmedSchedule> ocrConfirmedSchedules,  // OCR로 확인된 확정 일정
    String threadId,  // 대화 스레드 ID
    LocalDate startDate  // 여행 시작 날짜
) {
    // DateSelectionOutput을 받아서 생성
    public static RouteOptimizationRequest fromDateSelectionOutput(DateSelectionOutput dateSelectionOutput) {
        return new RouteOptimizationRequest(dateSelectionOutput, "BALANCED", "CAR", null, List.of(), null, LocalDate.now());
    }

    // OCR 확정 일정과 함께 생성
    public static RouteOptimizationRequest withOcrSchedules(
        DateSelectionOutput dateSelectionOutput,
        List<ConfirmedSchedule> ocrSchedules,
        String threadId,
        LocalDate startDate
    ) {
        return new RouteOptimizationRequest(
            dateSelectionOutput,
            "BALANCED",
            "CAR",
            null,
            ocrSchedules != null ? ocrSchedules : List.of(),
            threadId,
            startDate
        );
    }

    // 기존 호환성을 위한 메서드
    public Map<Integer, List<TourPlace>> getDailyPlaceCandidates() {
        Map<Integer, List<TourPlace>> candidates = new HashMap<>();
        if (dateSelectionOutput != null && dateSelectionOutput.dailyItineraries() != null) {
            dateSelectionOutput.dailyItineraries().forEach((day, itinerary) ->
                candidates.put(day, itinerary.places())
            );
        }
        return candidates;
    }

    // 날짜별 장소 리스트 직접 접근
    public List<TourPlace> getPlacesForDay(int day) {
        if (dateSelectionOutput != null && dateSelectionOutput.dailyItineraries() != null) {
            DailyItinerary itinerary = dateSelectionOutput.dailyItineraries().get(day);
            return itinerary != null ? itinerary.places() : List.of();
        }
        return List.of();
    }
}