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
        // 원문 그대로 저장
        state.setBudgetRaw(response.trim());
        state.setBudgetCollected(true);
        log.info("Budget collected (raw): {}", response.trim());
        
        // 선택적으로 예산 파싱 시도 (실패해도 계속 진행)
        try {
            // 예산 레벨 파싱
            String budgetLevel = TravelParsingUtils.parseBudgetLevel(response);
            state.setBudgetLevel(budgetLevel);
            
            // 구체적인 금액 파싱
            Integer amount = TravelParsingUtils.parseMoneyAmount(response);
            if (amount != null) {
                state.setBudgetPerPerson(amount);
                state.setBudgetCurrency("KRW");
            }
            
            log.debug("Budget parsed: level={}, amount={}", budgetLevel, amount);
        } catch (Exception e) {
            log.debug("Budget parsing failed, using raw budget: {}", e.getMessage());
        }
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