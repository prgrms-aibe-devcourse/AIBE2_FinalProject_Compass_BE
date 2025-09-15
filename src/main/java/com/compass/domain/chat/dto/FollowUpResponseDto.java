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
     * ChatThread ID
     */
    private String threadId;
    
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
     * 입력 타입 (text, select, date-range, multi-select 등)
     */
    private String inputType;
    
    /**
     * UI 타입 힌트 (calendar, checkbox-group 등)
     */
    private String uiType;
    
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
    
    /**
     * 세션 만료 여부 (alias for isExpired)
     */
    private boolean isSessionExpired;
    
    /**
     * FollowUpQuestionDto로부터 FollowUpResponseDto 생성
     * REQ-FOLLOW: Frontend 응답을 위한 변환 메서드
     */
    public static FollowUpResponseDto from(FollowUpQuestionDto questionDto) {
        if (questionDto == null) {
            return null;
        }
        
        return FollowUpResponseDto.builder()
            .sessionId(questionDto.getSessionId())
            .threadId(questionDto.getSessionId()) // sessionId를 threadId로도 사용
            .questionType("follow-up") // 기본값
            .question(questionDto.getPrimaryQuestion())
            .helpText(questionDto.getHelpText())
            .quickOptions(questionDto.getQuickOptions())
            .exampleAnswers(questionDto.getExampleAnswers())
            .inputType(questionDto.getInputType())
            .currentStep(questionDto.getCurrentStep() != null ? questionDto.getCurrentStep().toString() : null)
            .collectedInfo(questionDto.getCollectedInfo())
            .progressPercentage(questionDto.getProgressPercentage())
            .isComplete(false) // 질문 중이므로 false
            .canGeneratePlan(questionDto.isCanSkip()) // skip 가능하면 계획 생성 가능
            .message(null)
            .isExpired(false)
            .isSessionExpired(false)
            .build();
    }
}