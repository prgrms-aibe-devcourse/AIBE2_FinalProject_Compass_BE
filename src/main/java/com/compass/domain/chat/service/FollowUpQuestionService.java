package com.compass.domain.chat.service;

import com.compass.domain.chat.constant.FollowUpConstants;
import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.FollowUpResponseDto;
import com.compass.domain.chat.engine.RefactoredQuestionFlowEngine;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.util.ProgressCalculator;
import com.compass.domain.chat.util.TravelInfoValidator;
import com.compass.domain.chat.dto.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 꼬리질문 플로우 통합 서비스
 * REQ-FOLLOW-001 ~ REQ-FOLLOW-006 통합 구현
 * 
 * 프론트엔드와 연동하여 실제 채팅에서 꼬리질문 플로우를 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpQuestionService {
    
    private final RefactoredQuestionFlowEngine flowEngine;
    private final NaturalLanguageParsingService parsingService;
    private final TravelInfoValidator validator;
    private final SessionManagementService sessionService;
    
    /**
     * 새로운 꼬리질문 세션 시작
     * REQ-FOLLOW-001: 질문 플로우 엔진 시작
     */
    public FollowUpResponseDto startSession(String userId, String initialMessage) {
        // 세션 ID 생성
        String sessionId = sessionService.generateSessionId();
        log.info("Starting follow-up question session: {} for user: {}", sessionId, userId);
        
        // 초기 상태 생성
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.DESTINATION)
                .isCompleted(false)
                .build();
        
        // 초기 메시지 파싱 시도
        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            // REQ-FOLLOW-003: LLM을 활용한 자연어 파싱
            Map<String, Object> parsedData = parsingService.parseNaturalLanguageRequest(initialMessage);
            applyParsedDataToState(state, parsedData);
        }
        
        // REQ-FOLLOW-004: Redis에 세션 저장 (30분 TTL)
        sessionService.saveSession(sessionId, state);
        
        // 첫 번째 질문 생성
        FollowUpQuestionDto question = flowEngine.generateNextQuestion(state);
        
        return buildResponse(sessionId, state, question);
    }
    
    /**
     * 사용자 응답 처리 및 다음 질문 생성
     * REQ-FOLLOW-002: 정보 수집 및 파싱
     */
    public FollowUpResponseDto processUserResponse(String sessionId, String userResponse) {
        log.info("Processing user response for session: {}", sessionId);
        
        // 세션에서 상태 로드
        TravelInfoCollectionState state = sessionService.loadSession(sessionId);
        if (state == null) {
            log.warn("Session not found or expired: {}", sessionId);
            // 세션이 만료된 경우 새로 시작
            return startSession(null, userResponse);
        }
        
        // 사용자 응답 처리
        state = flowEngine.processResponse(state, userResponse);
        
        // 완료 여부 확인
        boolean isComplete = flowEngine.isFlowComplete(state);
        state.setCompleted(isComplete);
        
        // 세션 업데이트
        sessionService.saveSession(sessionId, state);
        
        // 다음 질문 생성 또는 완료 처리
        FollowUpQuestionDto question = null;
        if (!isComplete) {
            question = flowEngine.generateNextQuestion(state);
        }
        
        return buildResponse(sessionId, state, question);
    }
    
    /**
     * 현재 세션 상태 조회
     */
    public FollowUpResponseDto getSessionStatus(String sessionId) {
        TravelInfoCollectionState state = sessionService.loadSession(sessionId);
        if (state == null) {
            return FollowUpResponseDto.builder()
                    .sessionId(sessionId)
                    .isExpired(true)
                    .message(FollowUpConstants.SESSION_EXPIRED_MESSAGE)
                    .build();
        }
        
        FollowUpQuestionDto question = null;
        if (!state.isCompleted()) {
            question = flowEngine.generateNextQuestion(state);
        }
        
        return buildResponse(sessionId, state, question);
    }
    
    /**
     * 세션 정보 검증
     * REQ-FOLLOW-005: 완성도 검증
     */
    public ValidationResult validateSession(String sessionId) {
        TravelInfoCollectionState state = sessionService.loadSession(sessionId);
        if (state == null) {
            return ValidationResult.builder()
                    .valid(false)
                    .validationLevel(ValidationResult.ValidationLevel.STANDARD)
                    .userFriendlyMessage(FollowUpConstants.SESSION_EXPIRED_MESSAGE)
                    .build();
        }
        
        return validator.validate(state, ValidationResult.ValidationLevel.STANDARD);
    }
    
    /**
     * 응답 DTO 생성
     */
    private FollowUpResponseDto buildResponse(String sessionId, TravelInfoCollectionState state, FollowUpQuestionDto question) {
        // 진행률 계산
        int progress = ProgressCalculator.calculateProgress(state);
        
        // 여행계획 생성 가능 여부 확인
        boolean canGeneratePlan = ProgressCalculator.canGenerateTravelPlan(state);
        
        FollowUpResponseDto.FollowUpResponseDtoBuilder builder = FollowUpResponseDto.builder()
                .sessionId(sessionId)
                .isComplete(state.isCompleted())
                .progressPercentage(progress)
                .collectedInfo(state.toInfoMap())
                .canGeneratePlan(canGeneratePlan);
        
        if (question != null) {
            builder.questionType(question.isClarification() ? 
                    FollowUpConstants.QUESTION_TYPE_CLARIFICATION : 
                    FollowUpConstants.QUESTION_TYPE_FOLLOW_UP)
                   .question(question.getPrimaryQuestion())
                   .helpText(question.getHelpText())
                   .quickOptions(question.getQuickOptions())
                   .exampleAnswers(question.getExampleAnswers())
                   .inputType(question.getInputType())
                   .currentStep(question.getCurrentStep() != null ? question.getCurrentStep().name() : null);
        } else if (state.isCompleted()) {
            builder.questionType(FollowUpConstants.QUESTION_TYPE_COMPLETE)
                   .question(FollowUpConstants.COMPLETION_MESSAGE)
                   .message("이제 여행 계획을 생성할 수 있습니다.");
        }
        
        return builder.build();
    }
    
    /**
     * 파싱된 데이터를 상태에 적용
     */
    private void applyParsedDataToState(TravelInfoCollectionState state, Map<String, Object> parsedData) {
        if (parsedData.containsKey("destination")) {
            String destination = (String) parsedData.get("destination");
            if (destination != null && !destination.isEmpty()) {
                state.setDestination(destination);
                state.setDestinationCollected(true);
            }
        }
        
        if (parsedData.containsKey("nights")) {
            Integer nights = (Integer) parsedData.get("nights");
            if (nights != null && nights > 0) {
                state.setDurationNights(nights);
                state.setDurationCollected(true);
            }
        }
        
        if (parsedData.containsKey("groupType")) {
            String groupType = (String) parsedData.get("groupType");
            if (groupType != null) {
                state.setCompanionType(groupType);
                state.setCompanionsCollected(true);
            }
        }
        
        if (parsedData.containsKey("budget")) {
            String budget = (String) parsedData.get("budget");
            if (budget != null) {
                state.setBudgetLevel(budget);
                state.setBudgetCollected(true);
            }
        }
    }
}