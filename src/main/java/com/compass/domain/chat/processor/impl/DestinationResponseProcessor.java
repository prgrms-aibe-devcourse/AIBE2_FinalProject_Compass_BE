package com.compass.domain.chat.processor.impl;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import com.compass.domain.chat.service.NaturalLanguageParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 목적지 응답 처리기
 * REQ-FOLLOW-003: LLM 파싱 통합
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationResponseProcessor implements ResponseProcessor {
    
    private final NaturalLanguageParsingService parsingService;
    
    @Override
    public void process(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장
        state.setDestinationRaw(response.trim());
        state.setDestination(response.trim());
        state.setDestinationCollected(true);
        log.info("Destination collected (raw): {}", response.trim());
        
        // 선택적으로 LLM 파싱 시도 (실패해도 계속 진행)
        try {
            String parsedDestination = parsingService.parseDestination(response);
            if (parsedDestination != null && !parsedDestination.isEmpty()) {
                state.setDestination(parsedDestination);
                log.debug("Destination parsed by LLM: {}", parsedDestination);
            }
        } catch (Exception e) {
            log.debug("LLM parsing failed, using raw destination: {}", e.getMessage());
        }
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.DESTINATION;
    }
}