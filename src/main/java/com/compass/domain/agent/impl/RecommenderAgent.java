package com.compass.domain.agent.impl;

import com.compass.domain.agent.Agent;
import org.springframework.stereotype.Component;

@Component("recommenderAgent")
public class RecommenderAgent implements Agent {

    /**
     * REQ-INTENT-008: 개인화 추천 에이전트
     */
    @Override
    public String execute(String message) {
        // TODO: RAG 기반 개인화 추천 로직 구현
        return "[추천 에이전트] 맞춤 추천을 해드릴게요. 어떤 종류의 추천을 원하시나요?";
    }
}
