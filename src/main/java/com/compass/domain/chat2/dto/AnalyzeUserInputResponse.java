package com.compass.domain.chat2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AnalyzeUserInputResponse - 사용자 입력 분석 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeUserInputResponse {

    /**
     * 처리 상태
     */
    private String status;

    /**
     * 추출된 여행 정보
     */
    private Map<String, Object> extractedInfo;

    /**
     * 정보 수집 완료 여부
     */
    private boolean isComplete;

    /**
     * 누락된 필드 목록
     */
    private List<String> missingFields;

    /**
     * 추출 신뢰도 (0.0 ~ 1.0)
     */
    private double confidence;

    /**
     * 에러 코드 (에러 시)
     */
    private String errorCode;

    /**
     * 메시지
     */
    private String message;
}