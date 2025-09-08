package com.compass.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 여행 스타일 선호도 설정/수정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여행 스타일 선호도 설정 요청")
public class TravelStylePreferenceRequest {

    @Schema(
        description = "여행 스타일 선호도 목록",
        required = true
    )
    @NotEmpty(message = "선호도 목록은 비어있을 수 없습니다")
    @Size(min = 1, max = 3, message = "선호도는 1개 이상 3개 이하로 설정해야 합니다")
    @Valid
    @JsonProperty("preferences")
    private List<TravelStyleItem> preferences;

    /**
     * 가중치 합계 계산
     * 
     * @return 가중치 합계
     */
    public BigDecimal getTotalWeight() {
        if (preferences == null || preferences.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return preferences.stream()
                .map(TravelStyleItem::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 가중치 합계가 1.0인지 검증
     * 
     * @return 유효 여부
     */
    public boolean isValidWeightSum() {
        BigDecimal total = getTotalWeight();
        return total.compareTo(BigDecimal.ONE) == 0;
    }

    /**
     * 중복된 여행 스타일이 있는지 확인
     * 
     * @return 중복 여부
     */
    public boolean hasDuplicateTravelStyles() {
        if (preferences == null || preferences.isEmpty()) {
            return false;
        }
        
        Set<String> travelStyles = preferences.stream()
                .map(TravelStyleItem::getTravelStyle)
                .collect(Collectors.toSet());
        
        return travelStyles.size() != preferences.size();
    }

    /**
     * 중복된 여행 스타일 목록 반환
     * 
     * @return 중복된 여행 스타일 목록
     */
    public List<String> getDuplicateTravelStyles() {
        if (preferences == null || preferences.isEmpty()) {
            return List.of();
        }
        
        return preferences.stream()
                .map(TravelStyleItem::getTravelStyle)
                .collect(Collectors.groupingBy(style -> style, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    /**
     * 모든 여행 스타일이 유효한지 검증
     * 
     * @return 유효 여부
     */
    public boolean areAllTravelStylesValid() {
        if (preferences == null || preferences.isEmpty()) {
            return false;
        }
        
        return preferences.stream()
                .allMatch(TravelStyleItem::isValidTravelStyle);
    }

    /**
     * 유효하지 않은 여행 스타일 목록 반환
     * 
     * @return 유효하지 않은 여행 스타일 목록
     */
    public List<String> getInvalidTravelStyles() {
        if (preferences == null || preferences.isEmpty()) {
            return List.of();
        }
        
        return preferences.stream()
                .filter(item -> !item.isValidTravelStyle())
                .map(TravelStyleItem::getTravelStyle)
                .collect(Collectors.toList());
    }
}
