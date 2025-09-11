package com.compass.domain.chat.processor.impl;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import com.compass.domain.chat.util.TravelParsingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 예산 정보 응답 처리기
 */
@Slf4j
@Component
public class BudgetResponseProcessor implements ResponseProcessor {
    
    @Override
    public void process(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장 - 이것만으로도 충분함
        state.setBudgetRaw(response.trim());
        state.setBudgetCollected(true);
        log.info("Budget collected (raw): {}", response.trim());
        
        // 파싱은 선택사항 - 실패해도 문제없음
        try {
            // 예산 레벨 파싱 시도
            String budgetLevel = TravelParsingUtils.parseBudgetLevel(response);
            if (budgetLevel != null) {
                state.setBudgetLevel(budgetLevel);
            }
            
            // 구체적인 금액 파싱 시도
            try {
                Integer amount = TravelParsingUtils.parseMoneyAmount(response);
                if (amount != null && amount > 0) {
                    state.setBudgetPerPerson(amount);
                    state.setBudgetCurrency("KRW");
                }
            } catch (Exception amountEx) {
                // 금액 파싱 실패는 무시
                log.debug("Amount parsing skipped: {}", amountEx.getMessage());
            }
            
            log.debug("Budget processing complete: level={}, raw={}", 
                     state.getBudgetLevel(), state.getBudgetRaw());
        } catch (Exception e) {
            // 파싱 실패는 완전히 무시 - raw 값이 있으면 충분
            log.debug("Budget parsing skipped, using raw value only");
        }
        
        // 파싱 실패 플래그는 설정하지 않음
        state.setParsingFailed(false);
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.BUDGET;
    }
    
    @Override
    public boolean canSkip(TravelInfoCollectionState state) {
        // 예산은 선택 사항으로 건너뛸 수 있음
        return true;
    }
}