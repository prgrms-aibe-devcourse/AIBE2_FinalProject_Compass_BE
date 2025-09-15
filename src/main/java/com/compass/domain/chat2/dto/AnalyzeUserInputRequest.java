package com.compass.domain.chat2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AnalyzeUserInputRequest - 사용자 입력 분석 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeUserInputRequest {

    /**
     * 사용자 입력 텍스트
     */
    private String userInput;

    /**
     * 대화 스레드 ID
     */
    private String threadId;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 이전 컨텍스트 (선택)
     */
    private String previousContext;
}