package com.compass.domain.chat.strategy;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.FollowUpResponseDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;

/**
 * 응답 생성 전략 인터페이스
 * Strategy Pattern 적용 - 응답 유형별 생성 로직 분리
 */
public interface ResponseBuildStrategy {
    
    /**
     * 해당 전략이 적용 가능한지 확인
     */
    boolean canApply(TravelInfoCollectionState state, FollowUpQuestionDto question);
    
    /**
     * 응답 DTO 생성
     */
    FollowUpResponseDto buildResponse(
        String sessionId, 
        TravelInfoCollectionState state, 
        FollowUpQuestionDto question,
        int progressPercentage,
        boolean canGeneratePlan
    );
}