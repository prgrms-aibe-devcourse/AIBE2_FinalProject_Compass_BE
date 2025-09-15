package com.compass.domain.chat.engine;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.service.FollowUpQuestionGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
            state.setCurrentStep(currentStep);
        }
        
        log.debug("Before processing - Origin collected: {}, Destination collected: {}", 
                 state.isOriginCollected(), state.isDestinationCollected());
        
        switch (currentStep) {
            case ORIGIN -> processOriginResponse(state, userResponse);
            case DESTINATION -> processDestinationResponse(state, userResponse);
            case DATES -> processDateResponse(state, userResponse);
            case DURATION -> processDurationResponse(state, userResponse);
            case COMPANIONS -> processCompanionResponse(state, userResponse);
            case BUDGET -> processBudgetResponse(state, userResponse);
            case TRAVEL_STYLE -> processTravelStyleResponse(state, userResponse);
            default -> log.warn("Unknown step: {}", currentStep);
        }
        
        log.debug("After processing - Origin collected: {}, Destination collected: {}", 
                 state.isOriginCollected(), state.isDestinationCollected());
        
        // 다음 단계로 이동
        TravelInfoCollectionState.CollectionStep nextStep = state.getNextRequiredStep();
        state.setCurrentStep(nextStep);
        log.info("Moving to next step: {}", nextStep);
        
        return state;
    }
    
    @Override
    public boolean isFlowComplete(TravelInfoCollectionState state) {
        // 7개 필드 모두 체크 (출발지는 선택사항)
        boolean coreInfoCollected = state.isDestinationCollected() &&
                                    (state.isDatesCollected() || state.isDurationCollected()) && // 날짜나 기간 중 하나
                                    state.isCompanionsCollected() &&
                                    state.isBudgetCollected() &&
                                    (state.getTravelStyle() != null && !state.getTravelStyle().trim().isEmpty());
        
        log.info("Flow completion check - Destination: {}, Dates: {}, Duration: {}, Companions: {}, Budget: {}, TravelStyle: {}", 
                state.isDestinationCollected(),
                state.isDatesCollected(),
                state.isDurationCollected(),
                state.isCompanionsCollected(),
                state.isBudgetCollected(),
                state.getTravelStyle() != null && !state.getTravelStyle().trim().isEmpty());
        
        log.info("Flow completion result: {}", coreInfoCollected);
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
        // 원문 그대로 저장
        state.setOriginRaw(response.trim());
        state.setOrigin(response.trim()); // 호환성을 위해 기존 필드도 유지
        state.setOriginCollected(true);
        log.info("Origin collected (raw): {}", response.trim());
    }
    
    private void processDestinationResponse(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장
        state.setDestinationRaw(response.trim());
        state.setDestination(response.trim()); // 호환성을 위해 기존 필드도 유지
        state.setDestinationCollected(true);
        log.info("Destination collected (raw): {}", response.trim());
    }
    
    private void processDateResponse(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장
        state.setDatesRaw(response.trim());
        state.setDatesCollected(true);
        log.info("Dates collected (raw): {}", response.trim());
        
        // 간단한 날짜 파싱 시도 (선택적)
        try {
            Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*~\\s*(\\d{4}-\\d{2}-\\d{2})");
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                LocalDate startDate = LocalDate.parse(matcher.group(1));
                LocalDate endDate = LocalDate.parse(matcher.group(2));
                
                state.setStartDate(startDate);
                state.setEndDate(endDate);
                
                // 기간 자동 계산 - 날짜가 파싱되면 기간도 자동으로 수집된 것으로 처리
                int nights = (int) (endDate.toEpochDay() - startDate.toEpochDay());
                state.setDurationNights(nights);
                state.setDurationCollected(true); // 기간도 자동으로 수집됨
                state.setDurationRaw(nights + "박 " + (nights + 1) + "일");
                log.info("Duration automatically calculated from dates: {} nights", nights);
            }
        } catch (Exception e) {
            // 파싱 실패해도 계속 진행 (원문은 저장됨)
            log.debug("Date parsing failed, but raw data saved: {}", e.getMessage());
        }
    }
    
    private void processDurationResponse(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장
        state.setDurationRaw(response.trim());
        state.setDurationCollected(true);
        log.info("Duration collected (raw): {}", response.trim());
        
        // 간단한 기간 파싱 시도 (선택적)
        try {
            if (response.contains("당일") || response.toLowerCase().contains("day trip")) {
                state.setDurationNights(0);
            } else {
                Pattern pattern = Pattern.compile("(\\d+)");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    state.setDurationNights(Integer.parseInt(matcher.group(1)));
                }
            }
        } catch (Exception e) {
            // 파싱 실패해도 계속 진행 (원문은 저장됨)
            log.debug("Duration parsing failed, but raw data saved: {}", e.getMessage());
        }
    }
    
    private void processCompanionResponse(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장
        state.setCompanionsRaw(response.trim());
        state.setCompanionsCollected(true);
        log.info("Companions collected (raw): {}", response.trim());
        
        // 간단한 동행자 파싱 시도 (선택적)
        try {
            String lowerResponse = response.toLowerCase();
            
            if (lowerResponse.contains("혼자") || lowerResponse.contains("alone") || lowerResponse.contains("solo")) {
                state.setCompanionType("solo");
                state.setNumberOfTravelers(1);
            } else if (lowerResponse.contains("연인") || lowerResponse.contains("couple")) {
                state.setCompanionType("couple");
                state.setNumberOfTravelers(2);
            } else if (lowerResponse.contains("가족") || lowerResponse.contains("family")) {
                state.setCompanionType("family");
                Pattern pattern = Pattern.compile("(\\d+)");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    state.setNumberOfTravelers(Integer.parseInt(matcher.group(1)));
                } else {
                    state.setNumberOfTravelers(4);
                }
            } else if (lowerResponse.contains("친구") || lowerResponse.contains("friend")) {
                state.setCompanionType("friends");
                Pattern pattern = Pattern.compile("(\\d+)");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    state.setNumberOfTravelers(Integer.parseInt(matcher.group(1)));
                } else {
                    state.setNumberOfTravelers(2);
                }
            }
        } catch (Exception e) {
            // 파싱 실패해도 계속 진행 (원문은 저장됨)
            log.debug("Companion parsing failed, but raw data saved: {}", e.getMessage());
        }
    }
    
    private void processBudgetResponse(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장
        state.setBudgetRaw(response.trim());
        state.setBudgetCollected(true);
        log.info("Budget collected (raw): {}", response.trim());
        
        // 간단한 예산 파싱 시도 (선택적)
        try {
            String lowerResponse = response.toLowerCase();
            
            if (lowerResponse.contains("저렴") || lowerResponse.contains("budget") || lowerResponse.contains("cheap")) {
                state.setBudgetLevel("budget");
            } else if (lowerResponse.contains("보통") || lowerResponse.contains("moderate") || lowerResponse.contains("medium")) {
                state.setBudgetLevel("moderate");
            } else if (lowerResponse.contains("럭셔리") || lowerResponse.contains("luxury") || lowerResponse.contains("premium")) {
                state.setBudgetLevel("luxury");
            }
            
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
        } catch (Exception e) {
            // 파싱 실패해도 계속 진행 (원문은 저장됨)
            log.debug("Budget parsing failed, but raw data saved: {}", e.getMessage());
        }
    }
    
    private void processTravelStyleResponse(TravelInfoCollectionState state, String response) {
        // 원문 그대로 저장
        state.setTravelStyle(response.trim());
        log.info("Travel style collected: {}", response.trim());
        
        // multi-select 형태로 들어온 경우 처리
        // 프론트엔드에서 배열이나 쉼표로 구분된 문자열로 전달될 수 있음
        if (response.contains(",") || response.contains("&") || response.contains("and") || 
            response.contains("[") || response.contains("|")) {
            // 여러 스타일이 선택된 경우 그대로 저장
            state.setMainInterests(response.trim());
            log.info("Multiple travel styles selected: {}", response.trim());
        } else {
            // 단일 스타일도 mainInterests에 저장
            state.setMainInterests(response.trim());
        }
    }
}