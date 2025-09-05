package com.compass.domain.trip.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum BudgetLevel {
    BUDGET("알뜰 여행", "일일 약 5만원 이하"),
    STANDARD("일반 여행", "일일 약 10만원 ~ 20만원"),
    LUXURY("고급 여행", "일일 약 20만원 이상");

    private final String koreanName;
    private final String description;

    public static BudgetLevel fromString(String text) {
        return Arrays.stream(BudgetLevel.values())
                .filter(level -> level.name().equalsIgnoreCase(text))
                .findFirst()
                .orElse(null);
    }
}
