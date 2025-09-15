package com.compass.domain.chat2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * StartFollowUpRequest - Follow-up 시작 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartFollowUpRequest {

    /**
     * 대화 스레드 ID
     */
    private String threadId;

    /**
     * 사용자 ID
     */
    private String userId;

    /**
     * 현재까지 수집된 정보
     */
    private Map<String, Object> collectedInfo;

    /**
     * Follow-up 타입 (선택)
     * MISSING_INFO: 누락 정보 수집
     * CLARIFICATION: 명확화 필요
     * PREFERENCE: 선호도 확인
     */
    private String followUpType;
}