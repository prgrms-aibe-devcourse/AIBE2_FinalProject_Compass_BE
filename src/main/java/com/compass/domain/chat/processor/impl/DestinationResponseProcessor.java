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
        // REQ-FOLLOW-003: LLM 파싱 우선 사용
        String destination = parsingService.parseDestination(response);
        
        if (destination != null && !destination.isEmpty()) {
            state.setDestination(destination);
            state.setDestinationCollected(true);
            log.info("Destination collected via LLM parsing: {}", destination);
        } else {
            // 파싱 실패 시 원본 저장 (REQ-FOLLOW-006에서 재질문 처리)
            state.setParsingFailed(true);
            state.setFailedField("destination");
            log.warn("Failed to parse destination from response: {}", response);
        }
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.DESTINATION;
    }
}