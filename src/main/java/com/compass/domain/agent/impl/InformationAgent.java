package com.compass.domain.agent.impl;

import com.compass.domain.agent.Agent;
import org.springframework.stereotype.Component;

@Component("informationAgent")
public class InformationAgent implements Agent {

    /**
     * REQ-INTENT-010: 정보 알리미 에이전트
     */
    @Override
    public String execute(String message) {
        // TODO: Lambda MCP 등을 이용한 실시간 정보 조회 로직 구현
        return "[정보 에이전트] 날씨, 환율 등 필요한 정보를 알려드릴게요.";
    }
}
