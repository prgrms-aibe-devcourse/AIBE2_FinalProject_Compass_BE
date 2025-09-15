package com.compass.domain.trip.function.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 여행 계획 생성 응답 DTO
 * REQ-TRIP-001: 여행 계획 생성 Function
 */
@Data
@Builder
public class TravelPlanResponse {

    /**
     * 생성된 여행 계획 ID
     */
    @JsonProperty("trip_id")
    private Long tripId;

    /**
     * 여행 계획 UUID
     */
    @JsonProperty("trip_uuid")
    private String tripUuid;

    /**
     * 여행 계획 제목
     */
    @JsonProperty("title")
    private String title;

    /**
     * 목적지
     */
    @JsonProperty("destination")
    private String destination;

    /**
     * 여행 기간
     */
    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    /**
     * 여행 인원수
     */
    @JsonProperty("number_of_people")
    private Integer numberOfPeople;

    /**
     * 총 예산
     */
    @JsonProperty("total_budget")
    private Integer totalBudget;

    /**
     * 예상 총 비용
     */
    @JsonProperty("estimated_cost")
    private Integer estimatedCost;

    /**
     * 여행 스타일
     */
    @JsonProperty("travel_style")
    private String travelStyle;

    /**
     * 일별 상세 일정
     */
    @JsonProperty("daily_plans")
    private List<DailyPlan> dailyPlans;

    /**
     * 여행 계획 요약
     */
    @JsonProperty("summary")
    private String summary;

    /**
     * 응답 상태 (v2.0 표준)
     */
    @JsonProperty("status")
    private String status; // SUCCESS, ERROR, PARTIAL

    /**
     * 에러 코드 (에러 시)
     */
    @JsonProperty("error_code")
    private String errorCode;

    /**
     * 메시지
     */
    @JsonProperty("message")
    private String message;

    /**
     * 일별 상세 일정 DTO
     */
    @Data
    @Builder
    public static class DailyPlan {
        /**
         * 일차
         */
        @JsonProperty("day_number")
        private Integer dayNumber;

        /**
         * 날짜
         */
        @JsonProperty("date")
        private LocalDate date;

        /**
         * 일정 목록
         */
        @JsonProperty("activities")
        private List<Activity> activities;

        /**
         * 일일 예상 비용
         */
        @JsonProperty("daily_cost")
        private Integer dailyCost;
    }

    /**
     * 활동 DTO
     */
    @Data
    @Builder
    public static class Activity {
        /**
         * 활동명
         */
        @JsonProperty("place_name")
        private String placeName;

        /**
         * 카테고리
         */
        @JsonProperty("category")
        private String category;

        /**
         * 시작 시간
         */
        @JsonProperty("start_time")
        private LocalTime startTime;

        /**
         * 종료 시간
         */
        @JsonProperty("end_time")
        private LocalTime endTime;

        /**
         * 설명
         */
        @JsonProperty("description")
        private String description;

        /**
         * 예상 비용
         */
        @JsonProperty("estimated_cost")
        private Integer estimatedCost;

        /**
         * 주소
         */
        @JsonProperty("address")
        private String address;

        /**
         * 위도
         */
        @JsonProperty("latitude")
        private Double latitude;

        /**
         * 경도
         */
        @JsonProperty("longitude")
        private Double longitude;

        /**
         * 팁
         */
        @JsonProperty("tips")
        private String tips;
    }
}
