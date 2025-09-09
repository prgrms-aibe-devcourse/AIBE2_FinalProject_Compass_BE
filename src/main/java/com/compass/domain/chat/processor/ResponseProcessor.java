package com.compass.domain.chat.processor;

import com.compass.domain.chat.entity.TravelInfoCollectionState;

/**
 * 응답 처리를 위한 Strategy 인터페이스
 * OCP(Open-Closed Principle)를 준수하기 위한 설계
 */
public interface ResponseProcessor {
    
    /**
     * 사용자 응답을 처리하여 상태를 업데이트
     * 
     * @param state 현재 수집 상태
     * @param response 사용자 응답
     */
    void process(TravelInfoCollectionState state, String response);
    
    /**
     * 이 프로세서가 처리하는 수집 단계
     * 
     * @return 수집 단계
     */
    TravelInfoCollectionState.CollectionStep getStep();
    
    /**
     * 이 단계를 건너뛸 수 있는지 여부
     * 
     * @param state 현재 수집 상태
     * @return 건너뛸 수 있으면 true
     */
    default boolean canSkip(TravelInfoCollectionState state) {
        return false;
    }
}