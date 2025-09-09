package com.compass.domain.chat.engine;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.service.FollowUpQuestionGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 여행 정보 수집을 위한 질문 플로우 엔진 구현체
 * REQ-FOLLOW-001: 5개 필수 질문 순차 진행 (출발지 제외 시)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelQuestionFlowEngine implements QuestionFlowEngine {
    
    private final FollowUpQuestionGenerator questionGenerator;
    
    // 필수 질문 5개 (출발지 제외 옵션 고려)
    private static final int REQUIRED_QUESTIONS_WITHOUT_ORIGIN = 5;
    private static final int REQUIRED_QUESTIONS_WITH_ORIGIN = 6;
    
    @Override
    public FollowUpQuestionDto generateNextQuestion(TravelInfoCollectionState state) {
        log.info("Generating next question for session: {}, current step: {}", 
                state.getSessionId(), state.getCurrentStep());
        return questionGenerator.generateNextQuestion(state);
    }
    
    @Override
    public TravelInfoCollectionState processResponse(TravelInfoCollectionState state, String userResponse) {
        log.info("Processing response for step: {}, response: {}", state.getCurrentStep(), userResponse);
        
        TravelInfoCollectionState.CollectionStep currentStep = state.getCurrentStep();
        if (currentStep == null) {
            currentStep = state.getNextRequiredStep();
        }
        
        switch (currentStep) {
            case ORIGIN -> processOriginResponse(state, userResponse);
            case DESTINATION -> processDestinationResponse(state, userResponse);
            case DATES -> processDateResponse(state, userResponse);
            case DURATION -> processDurationResponse(state, userResponse);
            case COMPANIONS -> processCompanionResponse(state, userResponse);
            case BUDGET -> processBudgetResponse(state, userResponse);
            default -> log.warn("Unknown step: {}", currentStep);
        }
        
        // 다음 단계로 이동
        state.setCurrentStep(state.getNextRequiredStep());
        
        return state;
    }
    
    @Override
    public boolean isFlowComplete(TravelInfoCollectionState state) {
        // 출발지는 선택사항으로 처리 가능
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
        
        // 예산은 건너뛸 수 있음
        if (currentStep == TravelInfoCollectionState.CollectionStep.BUDGET) {
            return true;
        }
        
        // 날짜가 설정되어 있으면 기간을 건너뛸 수 있음
        if (currentStep == TravelInfoCollectionState.CollectionStep.DURATION &&
            state.getStartDate() != null && state.getEndDate() != null) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean validateFlow(TravelInfoCollectionState state) {
        // 최소 필수 정보 검증
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
        
        return true;
    }
    
    @Override
    public int getRequiredQuestionCount() {
        // 기본 5개 (목적지, 날짜, 기간, 동행자, 예산)
        // 출발지 포함 시 6개
        return REQUIRED_QUESTIONS_WITHOUT_ORIGIN;
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
    
    // === Private Helper Methods ===
    
    private void processOriginResponse(TravelInfoCollectionState state, String response) {
        state.setOrigin(response.trim());
        state.setOriginCollected(true);
        log.info("Origin collected: {}", response);
    }
    
    private void processDestinationResponse(TravelInfoCollectionState state, String response) {
        state.setDestination(response.trim());
        state.setDestinationCollected(true);
        log.info("Destination collected: {}", response);
    }
    
    private void processDateResponse(TravelInfoCollectionState state, String response) {
        // 날짜 파싱 로직
        try {
            // "2024-03-15 ~ 2024-03-17" 형식 파싱
            Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*~\\s*(\\d{4}-\\d{2}-\\d{2})");
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                LocalDate startDate = LocalDate.parse(matcher.group(1));
                LocalDate endDate = LocalDate.parse(matcher.group(2));
                
                state.setStartDate(startDate);
                state.setEndDate(endDate);
                state.setDatesCollected(true);
                
                // 기간 자동 계산
                int nights = (int) (endDate.toEpochDay() - startDate.toEpochDay());
                state.setDurationNights(nights);
                state.setDurationCollected(true);
                
                log.info("Dates collected: {} ~ {}, Duration: {} nights", startDate, endDate, nights);
            }
        } catch (DateTimeParseException e) {
            log.error("Failed to parse date response: {}", response, e);
        }
    }
    
    private void processDurationResponse(TravelInfoCollectionState state, String response) {
        // "2박 3일", "3 nights", "당일치기" 등 파싱
        try {
            if (response.contains("당일") || response.toLowerCase().contains("day trip")) {
                state.setDurationNights(0);
            } else {
                // 숫자 추출
                Pattern pattern = Pattern.compile("(\\d+)");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    state.setDurationNights(Integer.parseInt(matcher.group(1)));
                }
            }
            state.setDurationCollected(true);
            log.info("Duration collected: {} nights", state.getDurationNights());
        } catch (NumberFormatException e) {
            log.error("Failed to parse duration response: {}", response, e);
        }
    }
    
    private void processCompanionResponse(TravelInfoCollectionState state, String response) {
        String lowerResponse = response.toLowerCase();
        
        // 동행자 타입 파싱
        if (lowerResponse.contains("혼자") || lowerResponse.contains("alone") || lowerResponse.contains("solo")) {
            state.setCompanionType("solo");
            state.setNumberOfTravelers(1);
        } else if (lowerResponse.contains("연인") || lowerResponse.contains("couple")) {
            state.setCompanionType("couple");
            state.setNumberOfTravelers(2);
        } else if (lowerResponse.contains("가족") || lowerResponse.contains("family")) {
            state.setCompanionType("family");
            // 숫자가 있으면 파싱, 없으면 기본 4명
            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                state.setNumberOfTravelers(Integer.parseInt(matcher.group(1)));
            } else {
                state.setNumberOfTravelers(4);
            }
        } else if (lowerResponse.contains("친구") || lowerResponse.contains("friend")) {
            state.setCompanionType("friends");
            // 숫자 파싱
            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                state.setNumberOfTravelers(Integer.parseInt(matcher.group(1)));
            } else {
                state.setNumberOfTravelers(2);
            }
        } else if (lowerResponse.contains("비즈니스") || lowerResponse.contains("business")) {
            state.setCompanionType("business");
            state.setNumberOfTravelers(1);
        }
        
        state.setCompanionsCollected(true);
        log.info("Companions collected: type={}, count={}", state.getCompanionType(), state.getNumberOfTravelers());
    }
    
    private void processBudgetResponse(TravelInfoCollectionState state, String response) {
        String lowerResponse = response.toLowerCase();
        
        // 예산 레벨 파싱
        if (lowerResponse.contains("저렴") || lowerResponse.contains("budget") || lowerResponse.contains("cheap")) {
            state.setBudgetLevel("budget");
        } else if (lowerResponse.contains("보통") || lowerResponse.contains("moderate") || lowerResponse.contains("medium")) {
            state.setBudgetLevel("moderate");
        } else if (lowerResponse.contains("럭셔리") || lowerResponse.contains("luxury") || lowerResponse.contains("premium")) {
            state.setBudgetLevel("luxury");
        }
        
        // 구체적인 금액이 있으면 파싱
        Pattern pattern = Pattern.compile("(\\d+)(만원|천원|원|,000|000)");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            if (unit.contains("만원")) {
                amount *= 10000;
            } else if (unit.contains("천원") || unit.contains(",000")) {
                amount *= 1000;
            }
            
            state.setBudgetPerPerson(amount);
            state.setBudgetCurrency("KRW");
        }
        
        state.setBudgetCollected(true);
        log.info("Budget collected: level={}, amount={}", state.getBudgetLevel(), state.getBudgetPerPerson());
    }
}