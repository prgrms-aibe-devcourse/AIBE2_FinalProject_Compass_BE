package com.compass.domain.chat2.orchestrator;

import com.compass.domain.chat2.model.Intent;
import com.compass.domain.chat2.model.TravelPhase;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.ai.model.function.FunctionCallingOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OrchestrationContext - 오케스트레이션 실행 컨텍스트
 *
 * 오케스트레이션 과정에서 필요한 모든 정보를 담는 불변 컨텍스트 객체
 */
@Getter
@Builder
@ToString(exclude = {"functionOptions", "prompt"})
public class OrchestrationContext {

    // 요청 정보
    private final String userInput;
    private final String threadId;
    private final String userId;

    // 분류 결과
    private final Intent intent;

    // Phase 관리
    private final TravelPhase currentPhase;
    private final Map<String, Object> phaseData;

    // Function 정보
    private final List<String> selectedFunctions;
    private final FunctionCallingOptions functionOptions;

    // 프롬프트
    private final Prompt prompt;

    // 메타데이터
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 컨텍스트 유효성 검증
     */
    public boolean isValid() {
        return userInput != null && !userInput.isBlank()
            && threadId != null && !threadId.isBlank()
            && userId != null && !userId.isBlank()
            && intent != null
            && prompt != null;
    }

    /**
     * Function이 선택되었는지 확인
     */
    public boolean hasFunctions() {
        return selectedFunctions != null && !selectedFunctions.isEmpty();
    }

    /**
     * 현재 Phase 확인
     */
    public boolean isInPhase(TravelPhase phase) {
        return currentPhase == phase;
    }

    /**
     * Phase 전환 가능 여부 확인
     */
    public boolean canTransitionTo(TravelPhase nextPhase) {
        return currentPhase != null && currentPhase.canTransitionTo(nextPhase);
    }

    /**
     * Phase 데이터 조회
     */
    public Object getPhaseData(String key) {
        return phaseData != null ? phaseData.get(key) : null;
    }
}