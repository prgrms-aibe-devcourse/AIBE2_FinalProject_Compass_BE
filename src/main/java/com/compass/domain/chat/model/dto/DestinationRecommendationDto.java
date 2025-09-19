package com.compass.domain.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;


// 목적지 추천 결과를 담는 DTO.
// Record 타입을 사용하여 불변 객체로 설계.

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DestinationRecommendationDto(
        String title,
        String description,
        List<String> distanceOptions,
        List<RecommendedDestination> recommendedDestinations
) {

    // 개별 추천 목적지 정보를 담는 내부 Record.
    public record RecommendedDestination(
            String cityName,
            String country,
            String description,
            String imageUrl,
            List<String> tags
    ) {}
}