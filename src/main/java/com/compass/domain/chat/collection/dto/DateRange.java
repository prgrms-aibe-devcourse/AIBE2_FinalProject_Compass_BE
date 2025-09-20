package com.compass.domain.collection.dto;

import java.time.LocalDate;

// 날짜 범위(시작일, 종료일)를 나타내는 불변 데이터 객체
public record DateRange(
    LocalDate startDate,
    LocalDate endDate
) {
    // 생성자에서 날짜 유효성 검증 로직 추가
    public DateRange {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
    }
}
