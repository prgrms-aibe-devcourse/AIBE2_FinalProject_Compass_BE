package com.compass.domain.chat.dto;

import com.compass.domain.chat.model.TravelInfoTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 템플릿 상태 응답 DTO
 * 클라이언트에게 현재 수집 상태와 다음 질문을 전달
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateStatusDto {
    
    // 세션 정보
    private String sessionId;
    
    // 템플릿 데이터
    private TravelInfoTemplate template;
    
    // 다음 질문
    private String nextQuestion;
    private String helpText;
    private List<String> exampleAnswers;
    
    // 빠른 선택 옵션
    private List<QuickOption> quickOptions;
    
    // 상태 정보
    private boolean canGeneratePlan;
    private int completionPercentage;
    private List<String> missingFields;
    private List<String> validationErrors;
    
    // 요약
    private String summary;
    
    // UI 힌트
    private String inputType; // text, date, number, select
    private boolean canSkip;
    private boolean showGenerateButton;
    
    /**
     * 빠른 선택 옵션
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuickOption {
        private String value;
        private String label;
        private String description;
        private String icon;
    }
    
    /**
     * 성공 응답 생성
     */
    public static TemplateStatusDto success(TravelInfoTemplate template, String nextQuestion) {
        return TemplateStatusDto.builder()
                .sessionId(template.getSessionId())
                .template(template)
                .nextQuestion(nextQuestion)
                .canGeneratePlan(template.canGeneratePlan())
                .completionPercentage(template.getCompletionPercentage())
                .missingFields(template.getMissingFields())
                .summary(template.getSummary())
                .showGenerateButton(template.canGeneratePlan())
                .build();
    }
    
    /**
     * 완료 응답 생성
     */
    public static TemplateStatusDto complete(TravelInfoTemplate template) {
        return TemplateStatusDto.builder()
                .sessionId(template.getSessionId())
                .template(template)
                .canGeneratePlan(true)
                .completionPercentage(100)
                .summary(template.getSummary())
                .showGenerateButton(true)
                .build();
    }
}