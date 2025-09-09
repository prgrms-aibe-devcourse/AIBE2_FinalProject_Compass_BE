package com.compass.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 꼬리질문 API 응답 DTO
 * 프론트엔드에서 꼬리질문 플로우를 표시하기 위한 정보를 담습니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpResponseDto {
    
    /**
     * 세션 ID
     */
    private String sessionId;
    
    /**
     * 질문 유형 (follow-up, clarification, complete)
     */
    private String questionType;
    
    /**
     * 현재 질문
     */
    private String question;
    
    /**
     * 도움말 텍스트
     */
    private String helpText;
    
    /**
     * 빠른 선택 옵션들
     */
    private List<FollowUpQuestionDto.QuickOption> quickOptions;
    
    /**
     * 예시 답변들
     */
    private List<String> exampleAnswers;
    
    /**
     * 입력 타입 (text, select, date-range 등)
     */
    private String inputType;
    
    /**
     * 현재 단계
     */
    private String currentStep;
    
    /**
     * 수집된 정보
     */
    private Map<String, Object> collectedInfo;
    
    /**
     * 진행률 (0-100)
     */
    private int progressPercentage;
    
    /**
     * 완료 여부
     */
    private boolean isComplete;
    
    /**
     * 여행계획 생성 가능 여부
     */
    private boolean canGeneratePlan;
    
    /**
     * 추가 메시지
     */
    private String message;
    
    /**
     * 세션 만료 여부
     */
    private boolean isExpired;
}