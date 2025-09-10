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
        TravelParsingUtils.DateRange dateRange = TravelParsingUtils.parseDateRange(response);
        
        if (dateRange != null) {
            state.setStartDate(dateRange.startDate());
            state.setEndDate(dateRange.endDate());
            state.setDatesCollected(true);
            
            // 기간 자동 계산
            state.setDurationNights(dateRange.getNights());
            state.setDurationCollected(true);
            
            log.info("Dates collected: {} ~ {}, Duration: {} nights", 
                    dateRange.startDate(), dateRange.endDate(), dateRange.getNights());
        } else {
            log.warn("Failed to parse date range from: {}", response);
        }
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.DATES;
    }
}