package com.compass.domain.trip.function.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 여행 계획 생성 요청 DTO
 * REQ-TRIP-001: 여행 계획 생성 Function
 */
@Data
@Builder
public class TravelPlanRequest {

    /**
     * 사용자 ID (JWT에서 추출)
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 목적지
     */
    @JsonProperty("destination")
    private String destination;

    /**
     * 여행 시작일
     */
    @JsonProperty("start_date")
    private LocalDate startDate;

    /**
     * 여행 종료일
     */
    @JsonProperty("end_date")
    private LocalDate endDate;

    /**
     * 여행 인원수
     */
    @JsonProperty("number_of_people")
    private Integer numberOfPeople;

    /**
     * 총 예산 (원)
     */
    @JsonProperty("total_budget")
    private Integer totalBudget;

    /**
     * 여행 스타일 (예: 문화관광, 자연여행, 휴양, 액티비티 등)
     */
    @JsonProperty("travel_style")
    private String travelStyle;

    /**
     * 선호하는 활동 카테고리
     */
    @JsonProperty("preferred_categories")
    private List<String> preferredCategories;

    /**
     * 특별 요청사항
     */
    @JsonProperty("special_requests")
    private String specialRequests;

    /**
     * ChatThread ID (대화 컨텍스트 연결)
     */
    @JsonProperty("thread_id")
    private Long threadId;
}
