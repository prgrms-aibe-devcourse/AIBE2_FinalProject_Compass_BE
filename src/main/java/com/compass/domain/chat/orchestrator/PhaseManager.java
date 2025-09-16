package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Phase 관리자
@Slf4j
@Component
@RequiredArgsConstructor
public class PhaseManager {

    // 다음 Phase 결정
    public TravelPhase determineNextPhase(TravelPhase currentPhase, Intent intent,
                                         TravelContext context) {
        log.debug("Phase 전환 결정: current={}, intent={}", currentPhase, intent);

        // Phase 전환 로직
        return switch (currentPhase) {
            case INITIALIZATION -> {
                // 여행 계획 시작 시 정보 수집으로 전환
                if (intent == Intent.TRAVEL_PLANNING || intent == Intent.QUICK_INPUT) {
                    yield TravelPhase.INFORMATION_COLLECTION;
                }
                yield currentPhase;
            }
            case INFORMATION_COLLECTION -> {
                // 충분한 정보가 수집되면 계획 생성으로 전환
                if (hasEnoughInformation(context)) {
                    yield TravelPhase.PLAN_GENERATION;
                }
                yield currentPhase;
            }
            case PLAN_GENERATION -> {
                // 계획 생성 후 피드백 단계로 전환
                if (context.getTravelPlan() != null) {
                    yield TravelPhase.FEEDBACK_REFINEMENT;
                }
                yield currentPhase;
            }
            case FEEDBACK_REFINEMENT -> {
                // 사용자가 만족하면 완료로 전환
                if (isUserSatisfied(intent)) {
                    yield TravelPhase.COMPLETION;
                }
                yield currentPhase;
            }
            case COMPLETION -> currentPhase;
        };
    }

    // 충분한 정보 수집 여부 확인
    private boolean hasEnoughInformation(TravelContext context) {
        // 임시 구현 - 나중에 실제 로직 추가
        return context.getCollectedInfo() != null;
    }

    // 사용자 만족 여부 확인
    private boolean isUserSatisfied(Intent intent) {
        // 임시 구현 - 나중에 실제 로직 추가
        return intent == Intent.GENERAL_QUESTION;
    }
}
