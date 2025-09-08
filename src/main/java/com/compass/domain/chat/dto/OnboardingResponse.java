package com.compass.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 온보딩 응답 DTO
 * REQ-PERS-007: 신규 사용자 온보딩 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResponse {
    
    /**
     * 환영 메시지
     */
    private String welcomeMessage;
    
    /**
     * 선호도 수집을 위한 질문 목록
     */
    private List<String> preferenceQuestions;
    
    /**
     * 사용자가 바로 시작할 수 있는 예시 질문들
     */
    private List<String> exampleQuestions;
    
    /**
     * 신규 사용자 여부
     */
    private boolean isNewUser;
    
    /**
     * 온보딩 단계 (향후 확장용)
     * - WELCOME: 환영 단계
     * - PREFERENCE: 선호도 수집 단계
     * - READY: 준비 완료
     */
    @Builder.Default
    private String onboardingStep = "WELCOME";
}