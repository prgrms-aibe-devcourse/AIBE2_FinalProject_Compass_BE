package com.compass.domain.chat.model.response;

import lombok.Data;
import lombok.Builder;

/**
 * 통합 채팅 응답 모델
 * 담당: Trip 개발자
 */
@Data
@Builder
public class ChatResponse {
    private String content;
    private String type;
    private String phase;  // 현재 Phase 상태
    private String threadId;  // Thread ID 추가
    private String currentPhase;  // 현재 Phase 추가
    private Object data;
    private String nextAction;
    private boolean requiresConfirmation;  // Phase 진행 확인 필요 여부
}
