package com.compass.domain.chat.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 메모리 기반 여행 정보 수집 템플릿
 * DB 접근 최소화를 위한 임시 저장소
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelInfoTemplate {
    
    // 세션 정보
    private String sessionId;
    private Long userId;
    private String chatThreadId;
    
    // 필수 정보 (6개 필드)
    private String origin;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer numberOfTravelers;
    private String companionType; // solo, couple, family, friends, business
    private Integer budgetPerPerson;
    private String budgetCurrency;
    private String budgetLevel; // budget, moderate, luxury
    
    // 추가 정보
    @Builder.Default
    private Map<String, Object> additionalInfo = new HashMap<>();
    
    // 선호사항
    private List<String> interests;
    private String travelStyle; // relaxed, packed, balanced
    private String accommodation; // hotel, airbnb, hostel, resort
    private List<String> mustVisit;
    private List<String> avoidPlaces;
    private String dietaryRestrictions;
    private String mobilityRequirements;
    
    // 메타데이터
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String currentStep;
    private String lastUserResponse;
    
    /**
     * 템플릿 완성도 체크
     */
    @JsonIgnore
    public boolean isComplete() {
        return origin != null && 
               destination != null && 
               startDate != null && 
               endDate != null && 
               numberOfTravelers != null && 
               companionType != null;
        // 예산은 선택사항
    }
    
    /**
     * 필수 필드 완성도 체크 (예산 제외)
     */
    @JsonIgnore
    public boolean hasRequiredFields() {
        return origin != null && 
               destination != null && 
               startDate != null && 
               endDate != null && 
               numberOfTravelers != null && 
               companionType != null;
    }
    
    /**
     * 완성도 퍼센테이지 계산
     */
    @JsonIgnore
    public int getCompletionPercentage() {
        int completedFields = 0;
        int totalFields = 6; // 6개 필수 필드
        
        if (origin != null) completedFields++;
        if (destination != null) completedFields++;
        if (startDate != null) completedFields++;
        if (endDate != null) completedFields++;
        if (numberOfTravelers != null) completedFields++;
        if (companionType != null) completedFields++;
        
        return (completedFields * 100) / totalFields;
    }
    
    /**
     * 누락된 필드 목록
     */
    @JsonIgnore
    public List<String> getMissingFields() {
        List<String> missing = new ArrayList<>();
        
        if (origin == null) missing.add("origin");
        if (destination == null) missing.add("destination");
        if (startDate == null) missing.add("startDate");
        if (endDate == null) missing.add("endDate");
        if (numberOfTravelers == null) missing.add("numberOfTravelers");
        if (companionType == null) missing.add("companionType");
        
        return missing;
    }
    
    /**
     * 다음 필요한 필드 결정
     */
    @JsonIgnore
    public String getNextRequiredField() {
        if (origin == null) return "origin";
        if (destination == null) return "destination";
        if (startDate == null || endDate == null) return "dates";
        if (numberOfTravelers == null || companionType == null) return "companions";
        if (budgetPerPerson == null && budgetLevel == null) return "budget";
        return null;
    }
    
    /**
     * 여행 기간 계산 (박)
     */
    @JsonIgnore
    public Integer getDurationNights() {
        if (startDate != null && endDate != null) {
            return (int) ChronoUnit.DAYS.between(startDate, endDate);
        }
        return null;
    }
    
    /**
     * 여행 기간 계산 (일)
     */
    @JsonIgnore
    public Integer getDurationDays() {
        Integer nights = getDurationNights();
        return nights != null ? nights + 1 : null;
    }
    
    /**
     * 계획 생성 가능 여부
     */
    @JsonIgnore
    public boolean canGeneratePlan() {
        // 필수 필드가 모두 있으면 계획 생성 가능
        return hasRequiredFields();
    }
    
    /**
     * 템플릿 검증
     */
    @JsonIgnore
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // 날짜 검증
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                errors.add("종료일이 시작일보다 이전입니다");
            }
            if (startDate.isBefore(LocalDate.now())) {
                errors.add("시작일이 과거입니다");
            }
        }
        
        // 인원 검증
        if (numberOfTravelers != null && numberOfTravelers < 1) {
            errors.add("여행자 수는 1명 이상이어야 합니다");
        }
        
        // 예산 검증
        if (budgetPerPerson != null && budgetPerPerson < 0) {
            errors.add("예산은 0원 이상이어야 합니다");
        }
        
        return errors;
    }
    
    /**
     * 템플릿을 Map으로 변환 (프롬프트 생성용)
     */
    public Map<String, Object> toPromptVariables() {
        Map<String, Object> variables = new HashMap<>();
        
        variables.put("origin", origin != null ? origin : "미정");
        variables.put("destination", destination != null ? destination : "미정");
        variables.put("startDate", startDate != null ? startDate.toString() : "미정");
        variables.put("endDate", endDate != null ? endDate.toString() : "미정");
        variables.put("duration", getDurationDays() != null ? 
            getDurationNights() + "박 " + getDurationDays() + "일" : "미정");
        variables.put("numberOfTravelers", numberOfTravelers != null ? numberOfTravelers : 1);
        variables.put("companionType", companionType != null ? companionType : "solo");
        variables.put("budgetPerPerson", budgetPerPerson != null ? budgetPerPerson : "미정");
        variables.put("budgetLevel", budgetLevel != null ? budgetLevel : "moderate");
        variables.put("travelStyle", travelStyle != null ? travelStyle : "balanced");
        
        // 추가 정보
        if (interests != null && !interests.isEmpty()) {
            variables.put("interests", String.join(", ", interests));
        }
        if (mustVisit != null && !mustVisit.isEmpty()) {
            variables.put("mustVisit", String.join(", ", mustVisit));
        }
        if (dietaryRestrictions != null) {
            variables.put("dietaryRestrictions", dietaryRestrictions);
        }
        
        return variables;
    }
    
    /**
     * 간단한 요약 텍스트 생성
     */
    @JsonIgnore
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        
        if (origin != null && destination != null) {
            sb.append(origin).append(" → ").append(destination);
        }
        
        if (startDate != null && endDate != null) {
            sb.append(" | ").append(startDate).append(" ~ ").append(endDate);
            sb.append(" (").append(getDurationNights()).append("박").append(getDurationDays()).append("일)");
        }
        
        if (numberOfTravelers != null) {
            sb.append(" | ").append(numberOfTravelers).append("명");
            if (companionType != null) {
                sb.append(" (").append(getCompanionTypeKorean()).append(")");
            }
        }
        
        if (budgetPerPerson != null) {
            sb.append(" | 1인 ").append(String.format("%,d", budgetPerPerson)).append("원");
        } else if (budgetLevel != null) {
            sb.append(" | ").append(getBudgetLevelKorean());
        }
        
        return sb.toString();
    }
    
    /**
     * 동행자 유형 한글 변환
     */
    private String getCompanionTypeKorean() {
        if (companionType == null) return "";
        
        return switch (companionType) {
            case "solo" -> "혼자";
            case "couple" -> "커플";
            case "family" -> "가족";
            case "friends" -> "친구";
            case "business" -> "비즈니스";
            default -> companionType;
        };
    }
    
    /**
     * 예산 레벨 한글 변환
     */
    private String getBudgetLevelKorean() {
        if (budgetLevel == null) return "";
        
        return switch (budgetLevel) {
            case "budget" -> "저예산";
            case "moderate" -> "중간";
            case "luxury" -> "럭셔리";
            default -> budgetLevel;
        };
    }
}