package com.compass.domain.chat.stage3.dto;

import com.compass.domain.chat.model.TravelPlace;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 일별 여행 일정
@Getter
@Builder
public class DailyItinerary {
    private final LocalDate date;                // 날짜
    private final int dayNumber;                  // 여행 일차 (1일차, 2일차...)
    private final List<TravelPlace> places;       // 방문 장소 목록
    private final long estimatedDuration;         // 예상 소요 시간 (분)
    private final boolean hasFixedSchedules;      // OCR 고정 일정 포함 여부
    private final List<TimeSlot> timeSlots;       // 시간대별 일정
    private final Map<String, List<TravelPlace>> timeBlocks; // 시간 블록별 장소

    // 시간대별 일정 관리
    @Getter
    @Builder
    public static class TimeSlot {
        private final String timeBlock;           // BREAKFAST, MORNING, LUNCH, etc
        private final String startTime;           // "09:00"
        private final String endTime;             // "11:00"
        private final TravelPlace place;          // 해당 시간대 장소
        private final boolean isFixed;            // 고정 일정 여부 (항공, 호텔 체크인 등)
        private final String notes;               // 추가 메모
    }

    // 통계 정보
    public int getTotalPlaces() {
        return places != null ? places.size() : 0;
    }

    public int getFixedPlacesCount() {
        return (int) places.stream()
            .filter(p -> p.getIsFixed() != null && p.getIsFixed())
            .count();
    }

    public int getFlexiblePlacesCount() {
        return getTotalPlaces() - getFixedPlacesCount();
    }
}