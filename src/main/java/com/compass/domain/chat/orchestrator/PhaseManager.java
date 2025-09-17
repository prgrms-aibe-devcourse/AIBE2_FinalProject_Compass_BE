package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Phase 관리자 - 모든 대화를 여행 계획으로 자연스럽게 유도
@Slf4j
@Component
@RequiredArgsConstructor
public class PhaseManager {

    // 다음 Phase 결정 - 모든 대화를 여행 계획으로 유도
    public TravelPhase determineNextPhase(TravelPhase currentPhase, Intent intent,
                                         TravelContext context) {
        log.debug("Phase 전환 결정: current={}, intent={}", currentPhase, intent);

        // Phase 전환 로직 - 여행 계획으로 자연스럽게 유도
        return switch (currentPhase) {
            case INITIALIZATION -> {
                // 여행 계획 시작 시 정보 수집으로 전환
                if (intent == Intent.TRAVEL_INFO_COLLECTION) {
                    log.info("사용자가 여행 계획을 요청했습니다. 정보 수집 Phase로 전환합니다.");
                    yield TravelPhase.INFORMATION_COLLECTION;
                }
                // TRAVEL_QUESTION도 자연스럽게 정보 수집으로 이어질 수 있음
                if (intent == Intent.TRAVEL_QUESTION && shouldPromoteToCollection(context)) {
                    log.info("여행 관련 질문에서 계획 수립으로 자연스럽게 전환합니다.");
                    yield TravelPhase.INFORMATION_COLLECTION;
                }
                // GENERAL_CHAT도 대화를 통해 자연스럽게 유도
                log.debug("대화를 계속하며 여행 계획으로 유도 중...");
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
        // 일반 대화로 돌아가면 만족한 것으로 간주
        return intent == Intent.GENERAL_CHAT;
    }

    // 여행 계획으로 전환할 타이밍인지 확인
    private boolean shouldPromoteToCollection(TravelContext context) {
        // 여행 질문이 반복되면 계획 수립으로 유도
        // 나중에 대화 이력 분석 로직 추가 가능
        return context.getConversationCount() > 2;
    }
}