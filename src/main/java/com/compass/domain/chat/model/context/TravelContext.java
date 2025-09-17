package com.compass.domain.chat.model.context;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// 대화 컨텍스트 관리 - 여행 계획 유도를 위한 추적
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelContext {
    private String threadId;
    private String userId;
    private String currentPhase;
    private Object collectedInfo;
    private Object travelPlan;

    // 대화 진행 추적
    @Builder.Default
    private int conversationCount = 0;  // 대화 횟수 카운트

    // 대화 횟수 증가
    public void incrementConversation() {
        this.conversationCount++;
    }
}