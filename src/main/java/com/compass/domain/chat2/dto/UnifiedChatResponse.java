package com.compass.domain.chat2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * UnifiedChatResponse - 통합 채팅 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChatResponse {

    /**
     * 대화 스레드 ID
     */
    private String threadId;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 응답 시간
     */
    private LocalDateTime timestamp;

    /**
     * 처리 상태 (SUCCESS, ERROR, PARTIAL)
     */
    private String status;

    /**
     * 에러 코드 (에러 시)
     */
    private String errorCode;

    /**
     * 추가 데이터 (선택)
     * 여행 계획, OCR 결과 등
     */
    private Map<String, Object> data;

    /**
     * Follow-up 필요 여부
     */
    private boolean requiresFollowUp;

    /**
     * Follow-up 질문 (있는 경우)
     */
    private String followUpQuestion;
}