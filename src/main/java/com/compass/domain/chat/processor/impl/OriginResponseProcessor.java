package com.compass.domain.chat.processor.impl;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 출발지 응답 처리기
 * REQ-FOLLOW-003: 원문 그대로 저장
 */
@Slf4j
@Component
public class OriginResponseProcessor implements ResponseProcessor {
    
    @Override
    public void process(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장
        state.setOriginRaw(response.trim());
        state.setOrigin(response.trim());
        state.setOriginCollected(true);
        log.info("Origin collected (raw): {}", response.trim());
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.ORIGIN;
    }
}