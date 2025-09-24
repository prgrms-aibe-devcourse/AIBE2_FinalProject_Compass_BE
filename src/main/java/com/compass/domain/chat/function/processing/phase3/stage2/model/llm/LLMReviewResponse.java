package com.compass.domain.chat.function.processing.phase3.stage2.model.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// LLM 검수 응답 데이터
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMReviewResponse {
    private boolean needsAdjustment;
    private String reason;
    private List<LLMSuggestion> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LLMSuggestion {
        private int day;
        private String type;        // MOVE, REMOVE, SWAP, ADD_BREAK
        private String place;
        private String action;
        private Integer targetDay;  // MOVE인 경우
        private String swapWith;    // SWAP인 경우
    }
}