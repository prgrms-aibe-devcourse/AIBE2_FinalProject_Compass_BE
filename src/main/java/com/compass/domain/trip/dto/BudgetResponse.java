package com.compass.domain.trip.dto;

import com.compass.domain.trip.enums.BudgetLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BudgetResponse {
    private Long userId;
    private String budgetLevel;
    private String description;
    private String message;

    public static BudgetResponse from(Long userId, BudgetLevel budgetLevel, String message) {
        return BudgetResponse.builder()
                .userId(userId)
                .budgetLevel(budgetLevel != null ? budgetLevel.name() : null)
                .description(budgetLevel != null ? budgetLevel.getDescription() : "설정된 예산 수준이 없습니다.")
                .message(message)
                .build();
    }
    
    public static BudgetResponse of(Long userId, BudgetLevel budgetLevel) {
        return from(userId, budgetLevel, null);
    }
}
