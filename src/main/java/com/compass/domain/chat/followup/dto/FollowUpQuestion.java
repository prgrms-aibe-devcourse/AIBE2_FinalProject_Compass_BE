package com.compass.domain.chat.followup.dto;

// 후속 질문의 내용을 담는 불변 데이터 객체
public record FollowUpQuestion(
    String missingField, // 어떤 정보가 누락되었는지 (예: "DATE_RANGE", "DESTINATION")
    String questionText  // 사용자에게 전달할 실제 질문 문장
) {}
