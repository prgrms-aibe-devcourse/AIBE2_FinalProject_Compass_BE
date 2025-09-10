package com.compass.domain.chat.util;

import com.compass.domain.chat.constant.TravelConstants;
import com.compass.domain.chat.dto.ValidationResult;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 여행 정보 검증 유틸리티
 * REQ-FOLLOW-005: 필수 필드 입력 완성도 검증
 */
@Slf4j
@Component
public class TravelInfoValidator {
    
    // 검증 상수들은 TravelConstants에서 가져옴
    
    /**
     * 전체 여행 정보 검증
     */
    public ValidationResult validate(TravelInfoCollectionState state) {
        return validate(state, ValidationResult.ValidationLevel.STANDARD);
    }
    
    /**
     * 지정된 수준으로 여행 정보 검증
     */
    public ValidationResult validate(TravelInfoCollectionState state, ValidationResult.ValidationLevel level) {
        ValidationResult result = ValidationResult.builder()
                .valid(true)
                .validationLevel(level)
                .build();
        
        // 각 필드 검증
        validateOrigin(state, result, level);
        validateDestination(state, result, level);
        validateDates(state, result, level);
        validateDuration(state, result, level);
        validateCompanions(state, result, level);
        validateBudget(state, result, level);
        
        // 완성도 계산
        result.setCompletionPercentage(state.getCompletionPercentage());
        
        // 전체 검증 결과 설정
        result.setValid(result.getFieldErrors().isEmpty());
        
        // 개선 제안 추가
        if (!result.isValid()) {
            addSuggestions(result, state);
        }
        
        log.info("Validation result - Valid: {}, Completion: {}%, Errors: {}", 
                result.isValid(), result.getCompletionPercentage(), result.getFieldErrors().size());
        
        return result;
    }
    
    /**
     * 출발지 검증
     */
    private void validateOrigin(TravelInfoCollectionState state, ValidationResult result, 
                                ValidationResult.ValidationLevel level) {
        if (!state.isOriginCollected()) {
            result.addIncompleteField("origin");
            result.addFieldError("origin", "출발지를 입력해주세요");
            return;
        }
        
        String origin = state.getOrigin();
        if (origin == null || origin.trim().isEmpty()) {
            result.addFieldError("origin", "출발지를 입력해주세요");
            return;
        }
        
        if (level != ValidationResult.ValidationLevel.BASIC) {
            if (origin.trim().length() < TravelConstants.MIN_STRING_LENGTH) {
                result.addFieldError("origin", "출발지는 최소 2자 이상 입력해주세요");
            }
        }
    }
    
    /**
     * 목적지 검증
     */
    private void validateDestination(TravelInfoCollectionState state, ValidationResult result,
                                     ValidationResult.ValidationLevel level) {
        if (!state.isDestinationCollected()) {
            result.addIncompleteField("destination");
            result.addFieldError("destination", "목적지를 입력해주세요");
            return;
        }
        
        String destination = state.getDestination();
        if (destination == null || destination.trim().isEmpty()) {
            result.addFieldError("destination", "목적지를 입력해주세요");
            return;
        }
        
        if (level != ValidationResult.ValidationLevel.BASIC) {
            if (destination.trim().length() < TravelConstants.MIN_STRING_LENGTH) {
                result.addFieldError("destination", "목적지는 최소 2자 이상 입력해주세요");
            }
            
            // STRICT 수준에서 출발지와 목적지 동일 체크
            if (level == ValidationResult.ValidationLevel.STRICT) {
                if (state.getOrigin() != null && 
                    state.getOrigin().equalsIgnoreCase(destination)) {
                    result.addFieldError("destination", "출발지와 목적지가 동일합니다");
                }
            }
        }
    }
    
    /**
     * 날짜 검증
     */
    private void validateDates(TravelInfoCollectionState state, ValidationResult result,
                               ValidationResult.ValidationLevel level) {
        if (!state.isDatesCollected()) {
            result.addIncompleteField("dates");
            result.addFieldError("dates", "여행 날짜를 입력해주세요");
            return;
        }
        
        LocalDate startDate = state.getStartDate();
        LocalDate endDate = state.getEndDate();
        
        if (startDate == null) {
            result.addFieldError("startDate", "출발일을 입력해주세요");
            return;
        }
        
        if (endDate == null) {
            result.addFieldError("endDate", "도착일을 입력해주세요");
            return;
        }
        
        if (level != ValidationResult.ValidationLevel.BASIC) {
            LocalDate today = LocalDate.now();
            
            // 과거 날짜 체크
            if (startDate.isBefore(today)) {
                result.addFieldError("startDate", "출발일은 오늘 이후여야 합니다");
            }
            
            // 날짜 순서 체크
            if (endDate.isBefore(startDate)) {
                result.addFieldError("dates", "도착일이 출발일보다 빠릅니다");
            }
            
            // STRICT 수준에서 추가 검증
            if (level == ValidationResult.ValidationLevel.STRICT) {
                // 너무 먼 미래 체크
                long daysUntilStart = ChronoUnit.DAYS.between(today, startDate);
                if (daysUntilStart > TravelConstants.MAX_ADVANCE_BOOKING_DAYS) {
                    result.addFieldError("startDate", 
                        String.format("출발일은 최대 %d일 이내여야 합니다", TravelConstants.MAX_ADVANCE_BOOKING_DAYS));
                }
            }
        }
    }
    
    /**
     * 기간 검증
     */
    private void validateDuration(TravelInfoCollectionState state, ValidationResult result,
                                  ValidationResult.ValidationLevel level) {
        if (!state.isDurationCollected()) {
            result.addIncompleteField("duration");
            result.addFieldError("duration", "여행 기간을 입력해주세요");
            return;
        }
        
        Integer durationNights = state.getDurationNights();
        if (durationNights == null) {
            result.addFieldError("duration", "여행 기간을 입력해주세요");
            return;
        }
        
        if (level != ValidationResult.ValidationLevel.BASIC) {
            // 먼저 범위 검증
            if (durationNights < TravelConstants.MIN_DURATION_DAYS) {
                result.addFieldError("duration", 
                    String.format("여행 기간은 최소 %d일 이상이어야 합니다", TravelConstants.MIN_DURATION_DAYS));
                return; // 더 이상 검증하지 않음
            }
            
            if (durationNights > TravelConstants.MAX_DURATION_DAYS) {
                result.addFieldError("duration", 
                    String.format("여행 기간은 최대 %d일까지 가능합니다", TravelConstants.MAX_DURATION_DAYS));
                return; // 더 이상 검증하지 않음
            }
            
            // 날짜와 기간 일치 검증 (범위가 유효한 경우에만)
            if (state.getStartDate() != null && state.getEndDate() != null) {
                long calculatedNights = ChronoUnit.DAYS.between(state.getStartDate(), state.getEndDate());
                if (calculatedNights != durationNights) {
                    result.addFieldError("duration", 
                        String.format("날짜 기준 기간(%d일)과 입력된 기간(%d일)이 일치하지 않습니다", 
                            calculatedNights, durationNights));
                }
            }
        }
    }
    
    /**
     * 동행자 검증
     */
    private void validateCompanions(TravelInfoCollectionState state, ValidationResult result,
                                    ValidationResult.ValidationLevel level) {
        if (!state.isCompanionsCollected()) {
            result.addIncompleteField("companions");
            result.addFieldError("companions", "동행자 정보를 입력해주세요");
            return;
        }
        
        Integer numberOfTravelers = state.getNumberOfTravelers();
        if (numberOfTravelers == null) {
            result.addFieldError("companions", "여행 인원을 입력해주세요");
            return;
        }
        
        if (level != ValidationResult.ValidationLevel.BASIC) {
            if (numberOfTravelers < TravelConstants.MIN_TRAVELERS) {
                result.addFieldError("companions", 
                    String.format("여행 인원은 최소 %d명 이상이어야 합니다", TravelConstants.MIN_TRAVELERS));
            }
            
            if (numberOfTravelers > TravelConstants.MAX_TRAVELERS) {
                result.addFieldError("companions", 
                    String.format("여행 인원은 최대 %d명까지 가능합니다", TravelConstants.MAX_TRAVELERS));
            }
            
            // 동행자 타입 검증
            String companionType = state.getCompanionType();
            if (companionType == null || companionType.trim().isEmpty()) {
                result.addFieldError("companions", "동행자 유형을 선택해주세요");
            } else if (level == ValidationResult.ValidationLevel.STRICT) {
                // 동행자 타입과 인원 수 일치 검증
                validateCompanionTypeConsistency(companionType, numberOfTravelers, result);
            }
        }
    }
    
    /**
     * 예산 검증
     */
    private void validateBudget(TravelInfoCollectionState state, ValidationResult result,
                                ValidationResult.ValidationLevel level) {
        if (!state.isBudgetCollected()) {
            result.addIncompleteField("budget");
            result.addFieldError("budget", "예산 정보를 입력해주세요");
            return;
        }
        
        // 예산 레벨 또는 금액 중 하나는 있어야 함
        String budgetLevel = state.getBudgetLevel();
        Integer budgetPerPerson = state.getBudgetPerPerson();
        
        if ((budgetLevel == null || budgetLevel.trim().isEmpty()) && budgetPerPerson == null) {
            result.addFieldError("budget", "예산 수준 또는 금액을 입력해주세요");
            return;
        }
        
        if (level != ValidationResult.ValidationLevel.BASIC && budgetPerPerson != null) {
            if (budgetPerPerson < TravelConstants.MIN_BUDGET) {
                result.addFieldError("budget", 
                    String.format("예산은 최소 %,d원 이상이어야 합니다", TravelConstants.MIN_BUDGET));
            }
            
            if (budgetPerPerson > TravelConstants.MAX_BUDGET) {
                result.addFieldError("budget", 
                    String.format("예산은 최대 %,d원까지 가능합니다", TravelConstants.MAX_BUDGET));
            }
            
            // STRICT 수준에서 예산과 기간 일치성 검증
            if (level == ValidationResult.ValidationLevel.STRICT && state.getDurationNights() != null) {
                validateBudgetReasonableness(budgetPerPerson, state.getDurationNights(), 
                                            budgetLevel, result);
            }
        }
    }
    
    /**
     * 동행자 타입과 인원 수 일치성 검증
     */
    private void validateCompanionTypeConsistency(String companionType, int numberOfTravelers,
                                                  ValidationResult result) {
        switch (companionType.toLowerCase()) {
            case "solo" -> {
                if (numberOfTravelers != 1) {
                    result.addFieldError("companions", 
                        "혼자 여행시 인원은 1명이어야 합니다");
                }
            }
            case "couple" -> {
                if (numberOfTravelers != 2) {
                    result.addFieldError("companions", 
                        "커플 여행시 인원은 2명이어야 합니다");
                }
            }
            case "family" -> {
                if (numberOfTravelers < 2) {
                    result.addFieldError("companions", 
                        "가족 여행시 인원은 2명 이상이어야 합니다");
                }
            }
            case "friends" -> {
                if (numberOfTravelers < 2) {
                    result.addFieldError("companions", 
                        "친구와 여행시 인원은 2명 이상이어야 합니다");
                }
            }
        }
    }
    
    /**
     * 예산 합리성 검증
     */
    private void validateBudgetReasonableness(int budgetPerPerson, int durationNights,
                                             String budgetLevel, ValidationResult result) {
        int dailyBudget = budgetPerPerson / (durationNights + 1);
        
        // 예산 레벨과 실제 금액 일치성 검증
        if (budgetLevel != null) {
            switch (budgetLevel.toLowerCase()) {
                case "budget" -> {
                    if (dailyBudget > 200000) {
                        result.addSuggestion("일일 예산이 20만원을 초과합니다. '적당한' 또는 '럭셔리' 수준을 고려해보세요");
                    }
                }
                case "moderate" -> {
                    if (dailyBudget < 50000) {
                        result.addSuggestion("일일 예산이 5만원 미만입니다. '알뜰한' 수준을 고려해보세요");
                    } else if (dailyBudget > 500000) {
                        result.addSuggestion("일일 예산이 50만원을 초과합니다. '럭셔리' 수준을 고려해보세요");
                    }
                }
                case "luxury" -> {
                    if (dailyBudget < 200000) {
                        result.addSuggestion("일일 예산이 20만원 미만입니다. '적당한' 수준을 고려해보세요");
                    }
                }
            }
        }
        
        // 극단적인 예산 경고
        if (dailyBudget < 30000) {
            result.addSuggestion("일일 예산이 매우 적습니다. 숙박과 식사 비용을 고려해 예산을 재검토해주세요");
        }
    }
    
    /**
     * 개선 제안 추가
     */
    private void addSuggestions(ValidationResult result, TravelInfoCollectionState state) {
        // 날짜 관련 제안
        if (state.getStartDate() != null && state.getEndDate() == null && 
            state.getDurationNights() != null) {
            result.addSuggestion(String.format("출발일 기준으로 %d박 %d일 여행시 도착일은 %s입니다",
                state.getDurationNights(), state.getDurationNights() + 1,
                state.getStartDate().plusDays(state.getDurationNights())));
        }
        
        // 예산 관련 제안
        if (state.getBudgetPerPerson() == null && state.getBudgetLevel() != null) {
            String suggestedRange = switch (state.getBudgetLevel().toLowerCase()) {
                case "budget" -> "10만원~30만원";
                case "moderate" -> "30만원~100만원";
                case "luxury" -> "100만원 이상";
                default -> "예산 범위";
            };
            result.addSuggestion(String.format("%s 수준의 일반적인 예산은 %s입니다",
                state.getBudgetLevel(), suggestedRange));
        }
        
        // 완성도 제안
        int completionPercentage = state.getCompletionPercentage();
        if (completionPercentage < 50) {
            result.addSuggestion("여행 계획을 위해 더 많은 정보가 필요합니다");
        } else if (completionPercentage < 100) {
            result.addSuggestion(String.format("현재 %d%%가 완성되었습니다. 조금만 더 입력해주세요!", 
                completionPercentage));
        }
    }
    
    /**
     * 단일 필드 검증
     */
    public boolean validateField(String fieldName, Object value) {
        if (value == null) {
            return false;
        }
        
        return switch (fieldName.toLowerCase()) {
            case "origin", "destination" -> 
                value instanceof String && ((String) value).trim().length() >= TravelConstants.MIN_STRING_LENGTH;
            case "startdate", "enddate" -> 
                value instanceof LocalDate && !((LocalDate) value).isBefore(LocalDate.now());
            case "duration" -> 
                value instanceof Integer && ((Integer) value) >= TravelConstants.MIN_DURATION_DAYS && 
                ((Integer) value) <= TravelConstants.MAX_DURATION_DAYS;
            case "companions", "numberoftravelers" -> 
                value instanceof Integer && ((Integer) value) >= TravelConstants.MIN_TRAVELERS && 
                ((Integer) value) <= TravelConstants.MAX_TRAVELERS;
            case "budget", "budgetperperson" -> 
                value instanceof Integer && ((Integer) value) >= TravelConstants.MIN_BUDGET && 
                ((Integer) value) <= TravelConstants.MAX_BUDGET;
            default -> true;
        };
    }
}