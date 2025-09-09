package com.compass.domain.agent.impl;

import com.compass.domain.agent.Agent;
import org.springframework.stereotype.Component;

@Component("unknownAgent")
public class UnknownAgent implements Agent {

    /**
     * REQ-INTENT-005: 라우터 꼬리질문 (의도 명확화)
     */
    @Override
    public String execute(String message) {
        return "어떤 것을 도와드릴까요? (예: 여행 계획, 맛집 추천, 파리 날씨 정보)";
    }
}
