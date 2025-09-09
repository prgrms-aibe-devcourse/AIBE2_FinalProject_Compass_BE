package com.compass.domain.chat.engine;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.ValidationResult;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import com.compass.domain.chat.service.FollowUpQuestionGenerator;
import com.compass.domain.chat.service.ClarificationQuestionGenerator;
import com.compass.domain.chat.util.TravelInfoValidator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 리팩토링된 질문 플로우 엔진
 * Strategy Pattern을 활용하여 OCP를 준수하는 설계
 * 
 * @Primary 어노테이션으로 기존 TravelQuestionFlowEngine 대신 사용
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RefactoredQuestionFlowEngine implements QuestionFlowEngine {
    
    private final List<ResponseProcessor> processors;
    private final FollowUpQuestionGenerator questionGenerator;
    private final ClarificationQuestionGenerator clarificationGenerator;
    private final Map<TravelInfoCollectionState.CollectionStep, ResponseProcessor> processorMap = new HashMap<>();
    
    @Autowired(required = false)
    private TravelInfoValidator validator;
    
    @PostConstruct
    public void init() {
        // 각 프로세서를 단계별로 매핑
        processors.forEach(processor -> 
            processorMap.put(processor.getStep(), processor)
        );
        log.info("Initialized RefactoredQuestionFlowEngine with {} processors", processors.size());
    }
    
    @Override
    public FollowUpQuestionDto generateNextQuestion(TravelInfoCollectionState state) {
        log.info("Generating next question for session: {}, current step: {}", 
                state.getSessionId(), state.getCurrentStep());
        
        // REQ-FOLLOW-006: 파싱 실패 시 재질문 생성
        if (state.isParsingFailed()) {
            log.info("Parsing failed for field: {}, generating clarification question", state.getFailedField());
            return clarificationGenerator.generateClarificationQuestion(
                state, 
                state.getFailedField(),
                state.getLastQuestionAsked(),
                state.getRetryCount()
            );
        }
        
        return questionGenerator.generateNextQuestion(state);
    }
    
    @Override
    public TravelInfoCollectionState processResponse(TravelInfoCollectionState state, String userResponse) {
        log.info("Processing response for step: {}, response: {}", state.getCurrentStep(), userResponse);
        
        TravelInfoCollectionState.CollectionStep currentStep = state.getCurrentStep();
        if (currentStep == null) {
            currentStep = state.getNextRequiredStep();
        }
        
        // 이전 파싱 실패 상태 초기화
        state.setParsingFailed(false);
        state.setFailedField(null);
        
        // Strategy Pattern 적용: 적절한 프로세서 선택
        ResponseProcessor processor = processorMap.get(currentStep);
        if (processor != null) {
            processor.process(state, userResponse);
            
            // REQ-FOLLOW-006: 파싱 실패 체크
            if (state.isParsingFailed()) {
                // 파싱 실패 시 재시도 횟수 증가
                state.setRetryCount(state.getRetryCount() + 1);
                state.setLastQuestionAsked(userResponse);
                log.info("Parsing failed, retry count: {}", state.getRetryCount());
                // 다음 단계로 진행하지 않음
                return state;
            } else {
                // 파싱 성공 시 재시도 횟수 초기화
                state.setRetryCount(0);
            }
        } else {
            log.warn("No processor found for step: {}", currentStep);
        }
        
        // 파싱 성공 시에만 다음 단계로 이동
        if (!state.isParsingFailed()) {
            state.setCurrentStep(state.getNextRequiredStep());
        }
        
        return state;
    }
    
    @Override
    public boolean isFlowComplete(TravelInfoCollectionState state) {
        // 필수 정보 수집 완료 여부 확인
        boolean coreInfoCollected = state.isDestinationCollected() &&
                                    state.isDatesCollected() &&
                                    state.isDurationCollected() &&
                                    state.isCompanionsCollected() &&
                                    state.isBudgetCollected();
        
        log.info("Flow completion check - Core info collected: {}", coreInfoCollected);
        return coreInfoCollected;
    }
    
    @Override
    public boolean canSkipCurrentStep(TravelInfoCollectionState state) {
        TravelInfoCollectionState.CollectionStep currentStep = state.getCurrentStep();
        
        // 해당 프로세서에게 건너뛸 수 있는지 확인
        ResponseProcessor processor = processorMap.get(currentStep);
        if (processor != null) {
            return processor.canSkip(state);
        }
        
        // 기본 로직: 날짜가 설정되어 있으면 기간을 건너뛸 수 있음
        if (currentStep == TravelInfoCollectionState.CollectionStep.DURATION &&
            state.getStartDate() != null && state.getEndDate() != null) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean validateFlow(TravelInfoCollectionState state) {
        // REQ-FOLLOW-005: 향상된 검증 로직
        if (validator != null) {
            ValidationResult validationResult = validator.validate(state, ValidationResult.ValidationLevel.STANDARD);
            
            if (!validationResult.isValid()) {
                log.warn("Validation failed: {}", validationResult.getUserFriendlyMessage());
                
                // 검증 실패 시 상세 로그
                validationResult.getFieldErrors().forEach((field, error) -> 
                    log.debug("Field validation error - {}: {}", field, error)
                );
            }
            
            return validationResult.isValid();
        }
        
        // Fallback: 기본 검증 (validator가 없는 경우)
        log.debug("Using basic validation as TravelInfoValidator is not available");
        
        if (!state.isDestinationCollected()) {
            log.warn("Validation failed: Destination not collected");
            return false;
        }
        
        if (!state.isDatesCollected() && !state.isDurationCollected()) {
            log.warn("Validation failed: Neither dates nor duration collected");
            return false;
        }
        
        if (!state.isCompanionsCollected()) {
            log.warn("Validation failed: Companions not collected");
            return false;
        }
        
        // 데이터 유효성 체크
        if (!state.hasValidData()) {
            log.warn("Validation failed: Invalid data detected");
            return false;
        }
        
        return true;
    }
    
    @Override
    public int getRequiredQuestionCount() {
        // 기본 5개 (목적지, 날짜, 기간, 동행자, 예산)
        // 출발지 포함 시 6개
        return 5;
    }
    
    @Override
    public int getCurrentStepNumber(TravelInfoCollectionState state) {
        int completedSteps = 0;
        
        if (state.isOriginCollected()) completedSteps++;
        if (state.isDestinationCollected()) completedSteps++;
        if (state.isDatesCollected()) completedSteps++;
        if (state.isDurationCollected()) completedSteps++;
        if (state.isCompanionsCollected()) completedSteps++;
        if (state.isBudgetCollected()) completedSteps++;
        
        return completedSteps + 1; // 다음 단계 번호
    }
}