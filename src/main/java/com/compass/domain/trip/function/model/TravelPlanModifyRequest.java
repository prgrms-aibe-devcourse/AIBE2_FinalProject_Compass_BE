package com.compass.domain.trip.function.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 여행 계획 수정 요청 DTO
 * REQ-TRIP-001: 여행 계획 생성 Function
 */
@Data
@Builder
public class TravelPlanModifyRequest {

    /**
     * 수정할 여행 계획 ID
     */
    @JsonProperty("trip_id")
    private Long tripId;

    /**
     * 사용자 ID (권한 확인용)
     */
    @JsonProperty("user_id")
    private Long userId;

    /**
     * 수정 요청 타입
     */
    @JsonProperty("modify_type")
    private ModifyType modifyType;

    /**
     * 수정할 내용
     */
    @JsonProperty("modify_content")
    private String modifyContent;

    /**
     * 새로운 목적지 (목적지 변경 시)
     */
    @JsonProperty("new_destination")
    private String newDestination;

    /**
     * 새로운 시작일 (날짜 변경 시)
     */
    @JsonProperty("new_start_date")
    private LocalDate newStartDate;

    /**
     * 새로운 종료일 (날짜 변경 시)
     */
    @JsonProperty("new_end_date")
    private LocalDate newEndDate;

    /**
     * 새로운 예산 (예산 변경 시)
     */
    @JsonProperty("new_budget")
    private Integer newBudget;

    /**
     * 새로운 여행 스타일 (스타일 변경 시)
     */
    @JsonProperty("new_travel_style")
    private String newTravelStyle;

    /**
     * 추가할 장소들 (장소 추가 시)
     */
    @JsonProperty("places_to_add")
    private List<String> placesToAdd;

    /**
     * 제거할 장소들 (장소 제거 시)
     */
    @JsonProperty("places_to_remove")
    private List<String> placesToRemove;

    /**
     * 수정 요청 타입 열거형
     */
    public enum ModifyType {
        @JsonProperty("change_destination")
        CHANGE_DESTINATION,
        
        @JsonProperty("change_dates")
        CHANGE_DATES,
        
        @JsonProperty("change_budget")
        CHANGE_BUDGET,
        
        @JsonProperty("change_style")
        CHANGE_STYLE,
        
        @JsonProperty("add_places")
        ADD_PLACES,
        
        @JsonProperty("remove_places")
        REMOVE_PLACES,
        
        @JsonProperty("reorder_schedule")
        REORDER_SCHEDULE,
        
        @JsonProperty("custom")
        CUSTOM
    }
}
