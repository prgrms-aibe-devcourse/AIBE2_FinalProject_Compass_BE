package com.compass.domain.chat.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentType {
    // 우선순위 1: 시간이 고정된 예약
    FLIGHT_RESERVATION(1, "항공권"),
    HOTEL_RESERVATION(1, "호텔 예약"),
    TRAIN_TICKET(1, "기차표"),
    EVENT_TICKET(1, "공연/콘서트 티켓"),

    // 우선순위 2: 시간 조정 가능하지만 제한적
    CAR_RENTAL(2, "렌터카"),
    ATTRACTION_TICKET(2, "관광지 입장권"),

    // 우선순위 3: 시간 조정 가능
    RESTAURANT_RESERVATION(3, "레스토랑 예약"),

    // 미분류
    UNKNOWN(99, "미분류");

    private final int priority;
    private final String description;

    // 고정 일정 여부 (변경 불가능한 일정)
    public boolean isFixed() {
        return priority == 1;
    }
}
