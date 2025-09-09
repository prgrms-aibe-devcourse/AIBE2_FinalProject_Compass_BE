package com.compass.domain.chat.processor.impl;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import com.compass.domain.chat.util.TravelParsingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 동행자 정보 응답 처리기
 */
@Slf4j
@Component
public class CompanionsResponseProcessor implements ResponseProcessor {
    
    @Override
    public void process(TravelInfoCollectionState state, String response) {
        String companionType = TravelParsingUtils.parseCompanionType(response);
        int travelerCount = TravelParsingUtils.parseTravelerCount(response, companionType);
        
        state.setCompanionType(companionType);
        state.setNumberOfTravelers(travelerCount);
        state.setCompanionsCollected(true);
        
        log.info("Companions collected: type={}, count={}", companionType, travelerCount);
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.COMPANIONS;
    }
}