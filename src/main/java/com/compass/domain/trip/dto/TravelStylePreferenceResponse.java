package com.compass.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 여행 스타일 선호도 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여행 스타일 선호도 응답")
public class TravelStylePreferenceResponse {

    @Schema(
        description = "사용자 ID",
        example = "1"
    )
    @JsonProperty("userId")
    private Long userId;

    @Schema(
        description = "여행 스타일 선호도 목록"
    )
    @JsonProperty("preferences")
    private List<TravelStyleItem> preferences;

    @Schema(
        description = "총 가중치 합계",
        example = "1.0"
    )
    @JsonProperty("totalWeight")
    private BigDecimal totalWeight;

    @Schema(
        description = "응답 메시지",
        example = "여행 스타일 선호도가 성공적으로 설정되었습니다."
    )
    @JsonProperty("message")
    private String message;

    /**
     * 성공 응답 생성
     * 
     * @param userId 사용자 ID
     * @param preferences 선호도 목록
     * @param message 메시지
     * @return 성공 응답
     */
    public static TravelStylePreferenceResponse success(Long userId, 
                                                       List<TravelStyleItem> preferences, 
                                                       String message) {
        BigDecimal totalWeight = preferences.stream()
                .map(TravelStyleItem::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TravelStylePreferenceResponse.builder()
                .userId(userId)
                .preferences(preferences)
                .totalWeight(totalWeight)
                .message(message)
                .build();
    }

    /**
     * 빈 응답 생성 (선호도 미설정 시)
     * 
     * @param userId 사용자 ID
     * @param message 메시지
     * @return 빈 응답
     */
    public static TravelStylePreferenceResponse empty(Long userId, String message) {
        return TravelStylePreferenceResponse.builder()
                .userId(userId)
                .preferences(List.of())
                .totalWeight(BigDecimal.ZERO)
                .message(message)
                .build();
    }

    /**
     * 선호도가 설정되어 있는지 확인
     * 
     * @return 설정 여부
     */
    public boolean hasPreferences() {
        return preferences != null && !preferences.isEmpty();
    }

    /**
     * 가중치 합계가 유효한지 확인 (1.0)
     * 
     * @return 유효 여부
     */
    public boolean isValidWeightSum() {
        return totalWeight != null && totalWeight.compareTo(BigDecimal.ONE) == 0;
    }
}
