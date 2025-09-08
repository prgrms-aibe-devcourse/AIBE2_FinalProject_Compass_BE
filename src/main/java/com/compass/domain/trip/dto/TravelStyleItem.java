package com.compass.domain.trip.dto;

import com.compass.domain.trip.enums.TravelStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 여행 스타일 선호도 항목 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여행 스타일 선호도 항목")
public class TravelStyleItem {

    @Schema(
        description = "여행 스타일",
        example = "RELAXATION",
        allowableValues = {"RELAXATION", "SIGHTSEEING", "ACTIVITY"},
        required = true
    )
    @NotNull(message = "여행 스타일은 필수입니다")
    @JsonProperty("travelStyle")
    private String travelStyle;

    @Schema(
        description = "선호도 가중치 (0.0 ~ 1.0)",
        example = "0.5",
        minimum = "0.0",
        maximum = "1.0",
        required = true
    )
    @NotNull(message = "가중치는 필수입니다")
    @DecimalMin(value = "0.0", message = "가중치는 0.0 이상이어야 합니다")
    @DecimalMax(value = "1.0", message = "가중치는 1.0 이하여야 합니다")
    private BigDecimal weight;

    @Schema(
        description = "여행 스타일 설명",
        example = "휴양 및 힐링을 중심으로 한 여행"
    )
    private String description;

    @Schema(
        description = "생성 시간",
        example = "2024-08-01T10:00:00"
    )
    private LocalDateTime createdAt;

    @Schema(
        description = "수정 시간",
        example = "2024-08-01T10:00:00"
    )
    private LocalDateTime updatedAt;

    /**
     * TravelStyle ENUM으로부터 TravelStyleItem 생성
     * 
     * @param travelStyle TravelStyle ENUM
     * @param weight 가중치
     * @param createdAt 생성 시간
     * @param updatedAt 수정 시간
     * @return TravelStyleItem
     */
    public static TravelStyleItem from(TravelStyle travelStyle, BigDecimal weight, 
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return TravelStyleItem.builder()
                .travelStyle(travelStyle.name())
                .weight(weight)
                .description(travelStyle.getDescription())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * 여행 스타일 유효성 검증
     * 
     * @return 유효 여부
     */
    public boolean isValidTravelStyle() {
        return TravelStyle.isValid(this.travelStyle);
    }

    /**
     * TravelStyle ENUM 반환
     * 
     * @return TravelStyle ENUM
     */
    public TravelStyle getTravelStyleEnum() {
        return TravelStyle.fromString(this.travelStyle);
    }
}
