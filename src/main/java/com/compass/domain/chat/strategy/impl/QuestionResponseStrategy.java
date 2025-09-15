package com.compass.domain.chat.strategy.impl;

import com.compass.domain.chat.constant.FollowUpConstants;
import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.FollowUpResponseDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.strategy.ResponseBuildStrategy;
import org.springframework.stereotype.Component;

/**
 * 질문 응답 생성 전략
 * 꼬리질문 또는 명확화 질문에 대한 응답 생성
 */
@Component
public class QuestionResponseStrategy implements ResponseBuildStrategy {
    
    @Override
    public boolean canApply(TravelInfoCollectionState state, FollowUpQuestionDto question) {
        return question != null && !state.isCompleted();
    }
    
    @Override
    public FollowUpResponseDto buildResponse(
            String sessionId, 
            TravelInfoCollectionState state, 
            FollowUpQuestionDto question,
            int progressPercentage,
            boolean canGeneratePlan) {
        
        String questionType = question.isClarification() ? 
                FollowUpConstants.QUESTION_TYPE_CLARIFICATION : 
                FollowUpConstants.QUESTION_TYPE_FOLLOW_UP;
        
        return FollowUpResponseDto.builder()
                .sessionId(sessionId)
                .isComplete(false)
                .progressPercentage(progressPercentage)
                .collectedInfo(state.toInfoMap())
                .canGeneratePlan(canGeneratePlan)
                .questionType(questionType)
                .question(question.getPrimaryQuestion())
                .helpText(question.getHelpText())
                .quickOptions(question.getQuickOptions())
                .exampleAnswers(question.getExampleAnswers())
                .inputType(question.getInputType())
                .currentStep(question.getCurrentStep() != null ? 
                        question.getCurrentStep().name() : null)
                .build();
    }
}