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
        // 원문 그대로 저장
        state.setCompanionsRaw(response.trim());
        state.setCompanionsCollected(true);
        log.info("Companions collected (raw): {}", response.trim());
        
        // 선택적으로 동행자 파싱 시도 (실패해도 계속 진행)
        try {
            String companionType = TravelParsingUtils.parseCompanionType(response);
            int travelerCount = TravelParsingUtils.parseTravelerCount(response, companionType);
            
            state.setCompanionType(companionType);
            state.setNumberOfTravelers(travelerCount);
            
            log.debug("Companions parsed: type={}, count={}", companionType, travelerCount);
        } catch (Exception e) {
            log.debug("Companion parsing failed, using raw companions: {}", e.getMessage());
        }
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.COMPANIONS;
    }
}