package com.compass.domain.chat.processor.impl;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 출발지 응답 처리기
 */
@Slf4j
@Component
public class OriginResponseProcessor implements ResponseProcessor {
    
    @Override
    public void process(TravelInfoCollectionState state, String response) {
        String origin = response.trim();
        state.setOrigin(origin);
        state.setOriginCollected(true);
        log.info("Origin collected: {}", origin);
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.ORIGIN;
    }
}