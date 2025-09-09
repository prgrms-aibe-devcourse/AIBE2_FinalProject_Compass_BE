package com.compass.domain.chat.processor.impl;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import com.compass.domain.chat.util.TravelParsingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 목적지 응답 처리기
 */
@Slf4j
@Component
public class DestinationResponseProcessor implements ResponseProcessor {
    
    @Override
    public void process(TravelInfoCollectionState state, String response) {
        String destination = TravelParsingUtils.parseDestination(response);
        state.setDestination(destination);
        state.setDestinationCollected(true);
        log.info("Destination collected: {}", destination);
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.DESTINATION;
    }
}