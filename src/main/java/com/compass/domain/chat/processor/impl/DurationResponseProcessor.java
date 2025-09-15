package com.compass.domain.chat.processor.impl;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.processor.ResponseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기간 응답 처리기
 * REQ-FOLLOW-003: 원문 그대로 저장
 */
@Slf4j
@Component
public class DurationResponseProcessor implements ResponseProcessor {
    
    @Override
    public void process(TravelInfoCollectionState state, String response) {
        // 빈 응답이나 스킵 요청 처리
        if (response == null || response.trim().isEmpty() || 
            response.toLowerCase().contains("skip") || 
            response.toLowerCase().contains("나중에") ||
            response.toLowerCase().contains("모르")) {
            log.info("Duration skipped or empty, moving to next step");
            state.setDurationRaw("");
            state.setDurationCollected(true);
            return;
        }
        
        // 원문 그대로 저장
        state.setDurationRaw(response.trim());
        state.setDurationCollected(true);
        log.info("Duration collected (raw): {}", response.trim());
        
        // 선택적으로 기간 파싱 시도 (실패해도 계속 진행)
        try {
            if (response.contains("당일") || response.toLowerCase().contains("day trip")) {
                state.setDurationNights(0);
            } else {
                // "2박3일", "3박4일" 등의 패턴 파싱
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)박");
                java.util.regex.Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    state.setDurationNights(Integer.parseInt(matcher.group(1)));
                } else {
                    // 숫자만 있는 경우 (예: "3일")
                    pattern = java.util.regex.Pattern.compile("(\\d+)");
                    matcher = pattern.matcher(response);
                    if (matcher.find()) {
                        int days = Integer.parseInt(matcher.group(1));
                        state.setDurationNights(Math.max(0, days - 1)); // 일수 -> 박수 변환
                    }
                }
            }
            log.debug("Duration parsed: {} nights", state.getDurationNights());
        } catch (Exception e) {
            log.debug("Duration parsing failed, using raw duration: {}", e.getMessage());
        }
    }
    
    @Override
    public TravelInfoCollectionState.CollectionStep getStep() {
        return TravelInfoCollectionState.CollectionStep.DURATION;
    }
    
    @Override
    public boolean canSkip(TravelInfoCollectionState state) {
        // 날짜가 설정되어 있으면 기간을 건너뛸 수 있음
        return state.getStartDate() != null && state.getEndDate() != null;
    }
}