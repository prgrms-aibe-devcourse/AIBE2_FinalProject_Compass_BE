package com.compass.domain.chat2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StartFollowUpResponse - Follow-up 시작 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartFollowUpResponse {

    /**
     * 처리 상태
     */
    private String status;

    /**
     * Follow-up 질문
     */
    private String question;

    /**
     * 진행률 (0.0 ~ 1.0)
     */
    private double progress;

    /**
     * 대화 스레드 ID
     */
    private String threadId;

    /**
     * 추가 정보 필요 여부
     */
    private boolean requiresMoreInfo;

    /**
     * 예상 질문 개수
     */
    private int estimatedQuestions;

    /**
     * 에러 코드 (에러 시)
     */
    private String errorCode;

    /**
     * 메시지
     */
    private String message;
}