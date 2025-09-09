package com.compass.domain.agent.impl;

import com.compass.domain.agent.Agent;
import org.springframework.stereotype.Component;

@Component("plannerAgent")
public class PlannerAgent implements Agent {

    /**
     * REQ-INTENT-006: 여행 계획 전문 에이전트
     */
    @Override
    public String execute(String message) {
        // TODO: 여행 계획 생성 로직 구현 (일정 최적화, 예산 관리 등)
        return "[플래너 에이전트] 여행 계획을 생성해 드릴게요. 어떤 여행을 원하시나요?";
    }
}
