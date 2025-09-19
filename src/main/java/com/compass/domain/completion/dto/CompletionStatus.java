package com.compass.domain.completion.dto;

// 여행 정보 수집 진행률과 상태 메시지를 담는 불변 데이터 객체
public record CompletionStatus(
    int progressPercentage, // 계산된 진행률 (0-100)
    String message           // 현재 상태에 대한 메시지
) {}
