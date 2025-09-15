package com.compass.domain.chat.strategy.impl;

import com.compass.domain.chat.constant.FollowUpConstants;
import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.FollowUpResponseDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.strategy.ResponseBuildStrategy;
import org.springframework.stereotype.Component;

/**
 * 완료 응답 생성 전략
 * 정보 수집이 완료된 경우의 응답 생성
 */
@Component
public class CompletionResponseStrategy implements ResponseBuildStrategy {
    
    @Override
    public boolean canApply(TravelInfoCollectionState state, FollowUpQuestionDto question) {
        return state.isCompleted() && question == null;
    }
    
    @Override
    public FollowUpResponseDto buildResponse(
            String sessionId, 
            TravelInfoCollectionState state, 
            FollowUpQuestionDto question,
            int progressPercentage,
            boolean canGeneratePlan) {
        
        return FollowUpResponseDto.builder()
                .sessionId(sessionId)
                .isComplete(true)
                .progressPercentage(100) // 완료 시 항상 100%
                .collectedInfo(state.toInfoMap())
                .canGeneratePlan(true) // 완료되면 항상 계획 생성 가능
                .questionType(FollowUpConstants.QUESTION_TYPE_COMPLETE)
                .question(FollowUpConstants.COMPLETION_MESSAGE)
                .message("이제 여행 계획을 생성할 수 있습니다.")
                .build();
    }
}