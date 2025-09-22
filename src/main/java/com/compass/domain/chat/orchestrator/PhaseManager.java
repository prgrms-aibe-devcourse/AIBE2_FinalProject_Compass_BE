package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.orchestrator.cache.PhaseCache;
import com.compass.domain.chat.orchestrator.persistence.PhasePersistence;
import com.compass.domain.chat.orchestrator.strategy.PhaseLoadStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

// Phase 관리자 - 5단계 여행 계획 워크플로우 관리
@Slf4j
@Component
@RequiredArgsConstructor
public class PhaseManager {

    private final PhaseCache phaseCache;
    private final PhasePersistence phasePersistence;
    private final ContextManager contextManager;

    // 진행중인 대화용 Phase 조회 (Redis 우선)
    public TravelPhase getCurrentPhase(String threadId) {
        return getPhase(threadId, PhaseLoadStrategy.CACHE_FIRST);
    }

    // 히스토리 조회용 Phase 로드 (DB 우선)
    public TravelPhase loadPhaseFromHistory(String threadId) {
        return getPhase(threadId, PhaseLoadStrategy.DB_FIRST);
    }

    // 전략에 따른 Phase 조회
    private TravelPhase getPhase(String threadId, PhaseLoadStrategy strategy) {
        TravelPhase phase = null;

        if (strategy == PhaseLoadStrategy.CACHE_FIRST) {
            // 진행중인 대화 - Redis 먼저
            phase = phaseCache.get(threadId).orElse(null);
            if (phase == null) {
                phase = loadFromDbAndCache(threadId);
            }
        } else {
            // 히스토리 조회 - DB 먼저
            phase = phasePersistence.findByThreadId(threadId).orElse(null);
            if (phase != null) {
                phaseCache.put(threadId, phase);
            }
        }

        if (phase == null) {
            log.info("Thread {}의 Phase를 INITIALIZATION으로 초기화", threadId);
            phase = TravelPhase.INITIALIZATION;
            savePhase(threadId, phase);
        }

        return phase;
    }

    // DB에서 로드하고 캐시에 저장
    private TravelPhase loadFromDbAndCache(String threadId) {
        return phasePersistence.findByThreadId(threadId)
                .map(phase -> {
                    phaseCache.put(threadId, phase);
                    return phase;
                })
                .orElse(null);
    }

    // Phase 저장 (Redis와 DB 모두 업데이트)
    @Transactional
    public void savePhase(String threadId, TravelPhase phase) {
        // 동시에 양쪽에 저장
        phaseCache.put(threadId, phase);
        phasePersistence.save(threadId, phase);

        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ 💾 Phase 저장 완료");
        log.info("║ Thread ID: {}", threadId);
        log.info("║ Phase: {}", phase);
        log.info("║ Cache 저장: ✅");
        log.info("║ DB 저장: ✅");
        log.info("╚══════════════════════════════════════════════════════════════");
    }

    // Phase 전환 로직 - Intent와 컨텍스트 기반 전환
    public TravelPhase transitionPhase(String threadId, Intent intent, TravelContext context) {
        TravelPhase currentPhase = getCurrentPhase(threadId);

        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ 🔍 Phase 전환 검토 시작");
        log.info("║ Thread ID: {}", threadId);
        log.info("║ 현재 Phase: {}", currentPhase);
        log.info("║ Intent: {}", intent);
        log.info("╚══════════════════════════════════════════════════════════════");

        TravelPhase nextPhase = determineNextPhase(currentPhase, intent, context);

        if (currentPhase != nextPhase) {
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ 🎯 Phase 전환 결정!");
            log.info("║ {} → {}", currentPhase, nextPhase);
            log.info("║ 전환 이유: Intent {} 에 의한 자동 전환", intent);
            log.info("╚══════════════════════════════════════════════════════════════");

            savePhase(threadId, nextPhase);

            // 특별한 Phase 전환에 대한 추가 로깅
            logPhaseTransitionDetails(nextPhase);
        } else {
            log.info("║ ℹ️ Phase 유지: {} (전환 불필요)", currentPhase);
        }

        return nextPhase;
    }

    // 다음 Phase 결정 로직
    private TravelPhase determineNextPhase(TravelPhase currentPhase, Intent intent,
                                          TravelContext context) {
        log.debug("╔══════════════════════════════════════════════════════════════");
        log.debug("║ Phase 전환 로직 실행");
        log.debug("║ 현재 Phase: {}", currentPhase);
        log.debug("║ Intent: {}", intent);
        log.debug("╚══════════════════════════════════════════════════════════════");

        return switch (currentPhase) {
            case INITIALIZATION -> handleInitializationPhase(intent, context, currentPhase);
            case INFORMATION_COLLECTION -> handleInformationCollectionPhase(intent, context, currentPhase);
            case PLAN_GENERATION -> handlePlanGenerationPhase(intent, context, currentPhase);
            case FEEDBACK_REFINEMENT -> handleFeedbackRefinementPhase(intent, context, currentPhase);
            case COMPLETION -> handleCompletionPhase(intent, currentPhase);
        };
    }

    // INITIALIZATION Phase 처리
    private TravelPhase handleInitializationPhase(Intent intent, TravelContext context,
                                                 TravelPhase currentPhase) {
        // 사용자가 명확하게 여행 계획 시작을 확인한 경우에만 전환
        // TRAVEL_PLANNING intent는 사용자의 여행 의도를 나타내지만 바로 전환하지 않음

        // 사용자가 명시적으로 확인한 경우 (예: "네", "좋아", "시작할게" 등)
        // INFORMATION_COLLECTION Intent 자체는 전환 조건이 아님 - 확인 대기 중일 때만 CONFIRMATION으로 전환
        if (intent == Intent.CONFIRMATION && context.isWaitingForTravelConfirmation()) {
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ ✅ 전환 조건 충족: INITIALIZATION → INFORMATION_COLLECTION");
            log.info("║ 사용자가 여행 계획을 시작하기로 확인했습니다");
            log.info("╚══════════════════════════════════════════════════════════════");
            return TravelPhase.INFORMATION_COLLECTION;
        }

        // TRAVEL_PLANNING intent가 감지되었을 때
        if (intent == Intent.TRAVEL_PLANNING) {
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ ✅ TRAVEL_PLANNING Intent 감지 - 바로 정보 수집 단계로 전환");
            log.info("║ 사용자가 명확히 여행 계획을 요청했습니다");
            log.info("╚══════════════════════════════════════════════════════════════");
            context.setWaitingForTravelConfirmation(false);
            contextManager.updateContext(context, context.getUserId());
            return TravelPhase.INFORMATION_COLLECTION;
        }

        // 여행 질문이 반복되면 확인 질문 유도
        if (intent == Intent.GENERAL_QUESTION &&
            context.getConversationCount() >= 2) {
            log.info("여행 관련 대화 지속 - 확인 질문 대기");
            context.setWaitingForTravelConfirmation(true);
            // ContextManager를 통해 변경사항 저장
            contextManager.updateContext(context, context.getUserId());
            return currentPhase; // INITIALIZATION 유지
        }

        return currentPhase;
    }

    // INFORMATION_COLLECTION Phase 처리
    private TravelPhase handleInformationCollectionPhase(Intent intent, TravelContext context,
                                                        TravelPhase currentPhase) {
        // 충분한 정보 수집 완료 체크
        if (isInformationComplete(context)) {
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ ✅ 전환 조건 충족: INFORMATION_COLLECTION → PLAN_GENERATION");
            log.info("║ 필수 정보 수집 완료");
            log.info("╚══════════════════════════════════════════════════════════════");
            return TravelPhase.PLAN_GENERATION;
        }
        // 사용자가 직접 계획 생성 요청
        if (intent == Intent.DESTINATION_SEARCH) {
            log.info("사용자 요청으로 계획 생성 Phase로 전환");
            return TravelPhase.PLAN_GENERATION;
        }
        return currentPhase;
    }

    // PLAN_GENERATION Phase 처리
    private TravelPhase handlePlanGenerationPhase(Intent intent, TravelContext context,
                                                 TravelPhase currentPhase) {
        // 계획 생성 완료시 피드백 단계로 전환
        if (context.getTravelPlan() != null) {
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ ✅ 전환 조건 충족: PLAN_GENERATION → FEEDBACK_REFINEMENT");
            log.info("║ 여행 계획 생성 완료");
            log.info("╚══════════════════════════════════════════════════════════════");
            return TravelPhase.FEEDBACK_REFINEMENT;
        }
        // 계획 수정 요청시 바로 피드백 단계로
        if (intent == Intent.PLAN_MODIFICATION || intent == Intent.FEEDBACK) {
            return TravelPhase.FEEDBACK_REFINEMENT;
        }
        return currentPhase;
    }

    // FEEDBACK_REFINEMENT Phase 처리
    private TravelPhase handleFeedbackRefinementPhase(Intent intent, TravelContext context,
                                                     TravelPhase currentPhase) {
        // 사용자 만족시 완료 단계로 전환
        if (intent == Intent.COMPLETION) {
            // 완료 Intent
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ ✅ 전환 조건 충족: FEEDBACK_REFINEMENT → COMPLETION");
            log.info("║ 사용자가 여행 계획에 만족했습니다");
            log.info("╚══════════════════════════════════════════════════════════════");
            return TravelPhase.COMPLETION;
        }
        // 추가 정보 필요시 정보 수집으로 복귀
        if (intent == Intent.INFORMATION_COLLECTION &&
            needsMoreInfo(context)) {
            log.info("추가 정보 필요, 정보 수집 Phase로 복귀");
            return TravelPhase.INFORMATION_COLLECTION;
        }
        return currentPhase;
    }

    // COMPLETION Phase 처리
    private TravelPhase handleCompletionPhase(Intent intent, TravelPhase currentPhase) {
        // 완료 후 새로운 계획 시작시 초기화
        if (intent == Intent.TRAVEL_PLANNING) {
            log.info("새로운 여행 계획 시작, INITIALIZATION으로 전환");
            return TravelPhase.INITIALIZATION;
        }
        return currentPhase;
    }

    // 정보 수집 완료 여부 확인
    private boolean isInformationComplete(TravelContext context) {
        if (context.getCollectedInfo() == null) {
            return false;
        }

        Map<String, Object> info = (Map<String, Object>) context.getCollectedInfo();
        return hasRequiredTravelInfo(info);
    }

    // 필수 여행 정보 확인
    private boolean hasRequiredTravelInfo(Map<String, Object> info) {
        boolean hasDestination = info.containsKey("destination");
        boolean hasDates = info.containsKey("startDate") && info.containsKey("endDate");
        boolean hasBudget = info.containsKey("budget");

        return hasDestination && hasDates && hasBudget;
    }

    // 추가 정보 필요 여부 확인
    private boolean needsMoreInfo(TravelContext context) {
        // 컨텍스트 분석하여 추가 정보 필요 여부 판단
        if (context.getTravelPlan() == null) {
            return true;
        }

        Map<String, Object> plan = (Map<String, Object>) context.getTravelPlan();
        // 계획이 불완전하거나 사용자가 특정 정보 요청시
        return plan.get("status") != null && "incomplete".equals(plan.get("status"));
    }

    // Phase 초기화
    @Transactional
    public void resetPhase(String threadId) {
        TravelPhase initialPhase = TravelPhase.INITIALIZATION;
        savePhase(threadId, initialPhase);
        log.info("Thread {}의 Phase를 초기화", threadId);
    }

    // Phase 전환 세부 정보 로깅
    private void logPhaseTransitionDetails(TravelPhase phase) {
        switch (phase) {
            case INFORMATION_COLLECTION:
                log.info("║ 📋 정보 수집 단계로 진입합니다");
                log.info("║ 목적지, 날짜, 예산 등의 정보를 수집합니다");
                break;
            case PLAN_GENERATION:
                log.info("║ ✈️ 여행 계획 생성 단계로 진입합니다");
                log.info("║ 수집된 정보를 바탕으로 일정을 생성합니다");
                break;
            case FEEDBACK_REFINEMENT:
                log.info("║ 🔄 피드백 수정 단계로 진입합니다");
                log.info("║ 사용자의 피드백을 반영하여 계획을 수정합니다");
                break;
            case COMPLETION:
                log.info("║ ✅ 완료 단계로 진입합니다");
                log.info("║ 최종 여행 계획이 완성되었습니다");
                break;
            default:
                break;
        }
    }

    // 캐시만 삭제 (필요시)
    public void clearPhaseCache(String threadId) {
        phaseCache.evict(threadId);
        log.debug("Thread {}의 캐시 삭제", threadId);
    }

    // Phase 검증 (현재 Intent가 Phase에 적합한지)
    public boolean isValidIntentForPhase(TravelPhase phase, Intent intent) {
        return switch (phase) {
            case INITIALIZATION -> true; // 모든 Intent 허용
            case INFORMATION_COLLECTION ->
                intent == Intent.INFORMATION_COLLECTION ||
                intent == Intent.GENERAL_QUESTION ||
                intent == Intent.IMAGE_UPLOAD;
            case PLAN_GENERATION ->
                intent == Intent.DESTINATION_SEARCH ||
                intent == Intent.TRAVEL_PLANNING;
            case FEEDBACK_REFINEMENT ->
                intent == Intent.PLAN_MODIFICATION ||
                intent == Intent.FEEDBACK;
            case COMPLETION ->
                intent == Intent.COMPLETION;
        };
    }
}