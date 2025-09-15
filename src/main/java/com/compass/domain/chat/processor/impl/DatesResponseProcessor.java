package com.compass.domain.chat.processor.impl;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import com.compass.domain.chat.util.TravelParsingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 날짜 응답 처리기
 */
@Slf4j
@Component
public class DatesResponseProcessor implements ResponseProcessor {
    
    @Override
    public void process(TravelInfoCollectionState state, String response) {
        // 빈 응답이나 스킵 요청 처리
        if (response == null || response.trim().isEmpty() || 
            response.toLowerCase().contains("skip") || 
            response.toLowerCase().contains("나중에") ||
            response.toLowerCase().contains("모르")) {
            log.info("Dates skipped or empty, moving to next step");
            state.setDatesRaw("");
            state.setDatesCollected(true);
            return;
        }
        
        // 원문 그대로 저장
        state.setDatesRaw(response.trim());
        state.setDatesCollected(true);
        log.info("Dates collected (raw): {}", response.trim());
        
        // 선택적으로 날짜 파싱 시도 (실패해도 계속 진행)
        try {
            TravelParsingUtils.DateRange dateRange = TravelParsingUtils.parseDateRange(response);
            
            if (dateRange != null) {
                state.setStartDate(dateRange.startDate());
                state.setEndDate(dateRange.endDate());
                
                // 기간 자동 계산
                state.setDurationNights(dateRange.getNights());
                state.setDurationCollected(true);
                state.setDurationRaw(dateRange.getNights() + "박 " + (dateRange.getNights() + 1) + "일");
                
                log.debug("Dates parsed: {} ~ {}, Duration: {} nights", 
                        dateRange.startDate(), dateRange.endDate(), dateRange.getNights());
            }
        } catch (Exception e) {
            log.debug("Date parsing failed, using raw dates: {}", e.getMessage());
        }
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.DATES;
    }
}