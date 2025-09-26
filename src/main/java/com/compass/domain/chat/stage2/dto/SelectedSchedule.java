package com.compass.domain.chat.stage2.dto;

import java.time.LocalDateTime;

// Stage 2에서 사용할 사용자 선택 장소 DTO
public record SelectedSchedule(
    String placeId,
    String placeName,
    String category,
    String address,
    Double latitude,
    Double longitude,
    Double rating,
    LocalDateTime scheduledDateTime,  // 배정된 시간 (선택사항)
    boolean isUserSelected           // 사용자가 선택한 장소인지 여부
) {
    // 사용자 선택 장소용 생성자
    public static SelectedSchedule userSelected(
        String placeId,
        String placeName,
        String category,
        String address,
        Double latitude,
        Double longitude,
        Double rating
    ) {
        return new SelectedSchedule(
            placeId, placeName, category, address,
            latitude, longitude, rating,
            null, true
        );
    }

    // 시간이 배정된 사용자 선택 장소
    public static SelectedSchedule withSchedule(
        String placeId,
        String placeName,
        String category,
        String address,
        Double latitude,
        Double longitude,
        Double rating,
        LocalDateTime scheduledDateTime
    ) {
        return new SelectedSchedule(
            placeId, placeName, category, address,
            latitude, longitude, rating,
            scheduledDateTime, true
        );
    }
}