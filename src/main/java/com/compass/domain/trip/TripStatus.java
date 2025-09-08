package com.compass.domain.trip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TripStatus {
    PLANNING("계획 중"),
    ONGOING("여행 중"),
    COMPLETED("완료됨");

    private final String description;
}