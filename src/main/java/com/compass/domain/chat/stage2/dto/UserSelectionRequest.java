package com.compass.domain.chat.stage2.dto;

import java.util.List;

// Stage 2 사용자 선택 요청 DTO
public record UserSelectionRequest(
    String threadId,
    List<SelectedPlace> selectedPlaces,  // 사용자가 선택한 장소들
    int tripDays                         // 여행 일수
) {
    // 선택된 장소 DTO
    public record SelectedPlace(
        String placeId,
        String name,
        String category,
        double latitude,
        double longitude,
        String address,
        Double rating
    ) {}
}