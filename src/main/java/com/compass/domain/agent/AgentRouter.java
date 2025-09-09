package com.compass.domain.agent;

import com.compass.domain.intent.Intent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * REQ-INTENT-002: 라우팅 처리
 * <p>
 * 분류된 의도에 따라 적절한 에이전트를 선택하고 작업을 위임하는 라우터입니다.
 */
@Component
@RequiredArgsConstructor
public class AgentRouter {

    @Qualifier("plannerAgent")
    private final Agent plannerAgent;

    @Qualifier("recommenderAgent")
    private final Agent recommenderAgent;

    @Qualifier("informationAgent")
    private final Agent informationAgent;

    @Qualifier("unknownAgent")
    private final Agent unknownAgent;

    /**
     * 의도에 맞는 에이전트를 반환합니다.
     *
     * @param intent 분류된 의도
     * @return 해당 의도를 처리할 에이전트
     */
    public Agent getAgent(Intent intent) {
        return switch (intent) {
            case TRAVEL -> plannerAgent;
            case RECOMMENDATION -> recommenderAgent;
            case GENERAL -> informationAgent;
            default -> unknownAgent;
        };
    }
}
