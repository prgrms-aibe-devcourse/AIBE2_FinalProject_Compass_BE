package com.compass.domain.chat.model.context;

import lombok.Data;
import lombok.Builder;

/**
 * TODO: 구현 필요
 * 담당: Chat2 개발자
 *
 * 대화 컨텍스트 관리
 */
@Data
@Builder
public class TravelContext {
    private String threadId;
    private String userId;
    private String currentPhase;
    private Object collectedInfo;
    private Object travelPlan;
    // TODO: 추가 필드 구현
}
