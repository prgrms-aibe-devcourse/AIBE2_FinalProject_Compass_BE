package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.orchestrator.cache.PhaseCache;
import com.compass.domain.chat.orchestrator.persistence.PhasePersistence;
import com.compass.domain.chat.orchestrator.strategy.PhaseLoadStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
            MDC.put("threadId", threadId);
            try {
                log.info("Phase 초기화 - INITIALIZATION");
                phase = TravelPhase.INITIALIZATION;
                savePhase(threadId, phase);
            } finally {
                MDC.remove("threadId");
            }
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

        MDC.put("threadId", threadId);
        MDC.put("phase", phase.name());
        try {
            log.info("Phase 저장 완료 - Cache: ✅, DB: ✅");
        } finally {
            MDC.remove("threadId");
            MDC.remove("phase");
        }
    }

    // Phase 전환 로직 - Intent와 컨텍스트 기반 전환
    public TravelPhase transitionPhase(String threadId, Intent intent, TravelContext context) {
        TravelPhase currentPhase = getCurrentPhase(threadId);

        MDC.put("threadId", threadId);
        MDC.put("phase", currentPhase.name());
        MDC.put("intent", intent.name());

        try {
            log.info("Phase 전환 검토 시작");

        TravelPhase nextPhase = determineNextPhase(currentPhase, intent, context);

        if (currentPhase != nextPhase) {
            MDC.put("nextPhase", nextPhase.name());
            log.info("Phase 전환: {} → {}", currentPhase, nextPhase);
            MDC.remove("nextPhase");

            savePhase(threadId, nextPhase);

            // 특별한 Phase 전환에 대한 추가 로깅
            logPhaseTransitionDetails(nextPhase);
        } else {
            log.debug("Phase 유지 - 전환 불필요");
        }

        return nextPhase;
        } finally {
            MDC.remove("threadId");
            MDC.remove("phase");
            MDC.remove("intent");
        }
    }

    // 다음 Phase 결정 로직
    private TravelPhase determineNextPhase(TravelPhase currentPhase, Intent intent,
                                          TravelContext context) {
        log.debug("Phase 전환 로직 실행");

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
            log.info("INITIALIZATION → INFORMATION_COLLECTION 전환");
            return TravelPhase.INFORMATION_COLLECTION;
        }

        // TRAVEL_PLANNING intent가 감지되었을 때
        if (intent == Intent.TRAVEL_PLANNING) {
            log.info("TRAVEL_PLANNING Intent로 INFORMATION_COLLECTION 전환");
            context.setWaitingForTravelConfirmation(false);
            contextManager.updateContext(context, context.getUserId());
            return TravelPhase.INFORMATION_COLLECTION;
        }

        // 여행 질문이 반복되면 확인 질문 유도
        if (intent == Intent.GENERAL_QUESTION &&
            context.getConversationCount() >= 2) {
            log.debug("여행 관련 대화 지속 - 확인 대기");
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
            log.info("INFORMATION_COLLECTION → PLAN_GENERATION 전환");
            return TravelPhase.PLAN_GENERATION;
        }
        // 사용자가 직접 계획 생성 요청
        if (intent == Intent.DESTINATION_SEARCH) {
            log.info("사용자 요청으로 PLAN_GENERATION 전환");
            return TravelPhase.PLAN_GENERATION;
        }
        return currentPhase;
    }

    // PLAN_GENERATION Phase 처리
    private TravelPhase handlePlanGenerationPhase(Intent intent, TravelContext context,
                                                 TravelPhase currentPhase) {
        // 계획 생성 완료시 피드백 단계로 전환
        if (context.getTravelPlan() != null) {
            log.info("PLAN_GENERATION → FEEDBACK_REFINEMENT 전환");
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
            log.info("FEEDBACK_REFINEMENT → COMPLETION 전환");
            return TravelPhase.COMPLETION;
        }
        // 추가 정보 필요시 정보 수집으로 복귀
        if (intent == Intent.INFORMATION_COLLECTION &&
            needsMoreInfo(context)) {
            log.info("FEEDBACK_REFINEMENT → INFORMATION_COLLECTION 복귀");
            return TravelPhase.INFORMATION_COLLECTION;
        }
        return currentPhase;
    }

    // COMPLETION Phase 처리
    private TravelPhase handleCompletionPhase(Intent intent, TravelPhase currentPhase) {
        // 완료 후 새로운 계획 시작시 초기화
        if (intent == Intent.TRAVEL_PLANNING) {
            log.info("COMPLETION → INITIALIZATION 전환");
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
        MDC.put("threadId", threadId);
        try {
            log.info("Phase 초기화 완료");
        } finally {
            MDC.remove("threadId");
        }
    }

    // Phase 전환 세부 정보 로깅
    private void logPhaseTransitionDetails(TravelPhase phase) {
        switch (phase) {
            case INFORMATION_COLLECTION:
                log.debug("정보 수집 단계 진입 - 목적지, 날짜, 예산 수집");
                break;
            case PLAN_GENERATION:
                log.debug("계획 생성 단계 진입 - 수집된 정보로 일정 생성");
                break;
            case FEEDBACK_REFINEMENT:
                log.debug("피드백 수정 단계 진입 - 사용자 피드백 반영");
                break;
            case COMPLETION:
                log.debug("완료 단계 진입 - 최종 계획 완성");
                break;
            default:
                break;
        }
    }

    // 캐시만 삭제 (필요시)
    public void clearPhaseCache(String threadId) {
        phaseCache.evict(threadId);
        MDC.put("threadId", threadId);
        try {
            log.debug("캐시 삭제 완료");
        } finally {
            MDC.remove("threadId");
        }
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

    // OCR로 확인된 일정을 Phase2 Context에 추가
    @Transactional
    public void updatePhase2WithOcrSchedule(String threadId, ConfirmedSchedule schedule) {
        MDC.put("threadId", threadId);
        try {
            log.info("OCR 확정 일정 추가 - type: {}, title: {}",
                    schedule.documentType(), schedule.title());

            // Context 가져오기
            TravelContext context = contextManager.getContext(threadId).orElse(null);
            if (context == null) {
                log.warn("Context를 찾을 수 없음 - threadId: {}", threadId);
                return;
            }

            // OCR 일정 추가
            context.addOcrSchedule(schedule);

            // Phase 확인 및 업데이트
            TravelPhase currentPhase = getCurrentPhase(threadId);
            if (currentPhase == TravelPhase.INITIALIZATION) {
                // OCR 데이터가 있으면 자동으로 INFORMATION_COLLECTION으로 전환
                log.info("OCR 데이터로 인한 Phase 전환: INITIALIZATION → INFORMATION_COLLECTION");
                savePhase(threadId, TravelPhase.INFORMATION_COLLECTION);
            }

            // Context 저장
            contextManager.updateContext(context, context.getUserId());

            log.info("OCR 일정 저장 완료 - 총 {}개의 확정 일정",
                    context.getOcrConfirmedSchedules().size());
        } finally {
            MDC.remove("threadId");
        }
    }

    // OCR 원본 텍스트만 저장 (파싱 실패 시)
    @Transactional
    public void updatePhase2WithOcrText(String threadId, String text, DocumentType type) {
        MDC.put("threadId", threadId);
        MDC.put("documentType", type.name());
        try {
            log.info("OCR 원본 텍스트 저장 - textLength: {}", text.length());

            TravelContext context = contextManager.getContext(threadId).orElse(null);
            if (context == null) {
                log.warn("Context를 찾을 수 없음 - threadId: {}", threadId);
                return;
            }

            // OCR 원본 텍스트 저장
            context.addOcrRawText(type, text);
            contextManager.updateContext(context, context.getUserId());

            log.info("OCR 원본 텍스트 저장 완료");
        } finally {
            MDC.remove("threadId");
            MDC.remove("documentType");
        }
    }

    // OCR 처리 에러 알림
    public void notifyOcrError(String threadId, String errorMessage) {
        MDC.put("threadId", threadId);
        try {
            log.error("OCR 처리 실패 알림 - error: {}", errorMessage);

            // TODO: 사용자에게 에러 알림 (WebSocket/SSE)
            // 임시로 로그만 남김
        } finally {
            MDC.remove("threadId");
        }
    }
}
