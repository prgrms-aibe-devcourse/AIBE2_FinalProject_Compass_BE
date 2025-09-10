package com.compass.domain.chat.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 여행 정보 검증 결과 DTO
 * REQ-FOLLOW-005: 필수 필드 입력 완성도 검증 결과
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    
    /**
     * 전체 검증 성공 여부
     */
    private boolean valid;
    
    /**
     * 필드별 오류 메시지
     * key: 필드명, value: 오류 메시지
     */
    @Builder.Default
    private Map<String, String> fieldErrors = new HashMap<>();
    
    /**
     * 사용자에게 제공할 개선 제안
     */
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();
    
    /**
     * 완성도 백분율 (0-100)
     */
    private int completionPercentage;
    
    /**
     * 미완성 필드 목록
     */
    @Builder.Default
    private List<String> incompleteFields = new ArrayList<>();
    
    /**
     * 검증 수준
     */
    private ValidationLevel validationLevel;
    
    /**
     * 검증 수준 열거형
     */
    public enum ValidationLevel {
        BASIC,      // 기본 검증 (null, 빈 값)
        STANDARD,   // 표준 검증 (형식, 범위)
        STRICT      // 엄격한 검증 (비즈니스 규칙)
    }
    
    /**
     * 오류 추가 헬퍼 메서드
     */
    public void addFieldError(String field, String error) {
        if (fieldErrors == null) {
            fieldErrors = new HashMap<>();
        }
        fieldErrors.put(field, error);
    }
    
    /**
     * 제안 추가 헬퍼 메서드
     */
    public void addSuggestion(String suggestion) {
        if (suggestions == null) {
            suggestions = new ArrayList<>();
        }
        suggestions.add(suggestion);
    }
    
    /**
     * 미완성 필드 추가 헬퍼 메서드
     */
    public void addIncompleteField(String field) {
        if (incompleteFields == null) {
            incompleteFields = new ArrayList<>();
        }
        incompleteFields.add(field);
    }
    
    /**
     * 특정 필드에 오류가 있는지 확인
     */
    public boolean hasFieldError(String field) {
        return fieldErrors != null && fieldErrors.containsKey(field);
    }
    
    /**
     * 오류가 있는지 확인
     */
    public boolean hasErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }
    
    /**
     * 사용자 친화적인 메시지 생성
     */
    public String getUserFriendlyMessage() {
        if (valid) {
            return "모든 필수 정보가 올바르게 입력되었습니다.";
        }
        
        StringBuilder message = new StringBuilder();
        message.append("다음 정보를 확인해 주세요:\n");
        
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            fieldErrors.forEach((field, error) -> {
                message.append("• ").append(getFieldDisplayName(field))
                       .append(": ").append(error).append("\n");
            });
        }
        
        if (suggestions != null && !suggestions.isEmpty()) {
            message.append("\n💡 제안사항:\n");
            suggestions.forEach(suggestion -> 
                message.append("• ").append(suggestion).append("\n")
            );
        }
        
        return message.toString();
    }
    
    /**
     * 필드명을 사용자 친화적인 이름으로 변환
     */
    private String getFieldDisplayName(String field) {
        return switch (field) {
            case "origin" -> "출발지";
            case "destination" -> "목적지";
            case "startDate" -> "출발일";
            case "endDate" -> "도착일";
            case "duration" -> "여행 기간";
            case "companions" -> "동행자";
            case "budget" -> "예산";
            default -> field;
        };
    }
}