package com.compass.domain.chat2.service;

import com.compass.domain.chat2.model.Intent;
import com.compass.domain.chat2.model.TravelPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TravelPhaseManager - 여행 계획 Phase 관리 서비스
 *
 * 사용자별 현재 Phase를 추적하고 적절한 Function을 추천
 * MainLLMOrchestrator와 협력하여 Phase 기반 오케스트레이션 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPhaseManager {

    // Thread별 현재 Phase 저장 (세션 관리)
    private final Map<String, PhaseContext> phaseContextMap = new ConcurrentHashMap<>();

    /**
     * Intent 기반으로 초기 Phase 결정
     */
    public TravelPhase determineInitialPhase(Intent intent) {
        return switch (intent) {
            case TRAVEL_PLANNING, INFORMATION_COLLECTION -> TravelPhase.INFORMATION_COLLECTION;
            case IMAGE_UPLOAD -> TravelPhase.INFORMATION_COLLECTION; // OCR은 정보 수집
            case QUICK_INPUT -> TravelPhase.INFORMATION_COLLECTION;   // 빠른 입력도 정보 수집
            case DESTINATION_SEARCH -> TravelPhase.PLAN_GENERATION;   // 검색은 계획 생성
            default -> TravelPhase.INITIALIZATION;
        };
    }

    /**
     * 현재 Phase에서 사용 가능한 Function 목록 반환
     */
    public List<String> getPhaseAppropriateFunctions(TravelPhase phase) {
        return switch (phase) {
            case INITIALIZATION -> List.of(
                "classifyIntent",
                "initializeSession"
            );

            case INFORMATION_COLLECTION -> List.of(
                "showQuickInputForm",      // 우선순위 1: 빠른 입력 폼
                "processOCR",              // 우선순위 2: OCR 자동 추출
                "analyzeUserInput",        // 우선순위 3: 사용자 입력 분석
                "startFollowUp",           // 우선순위 4: Follow-up 시작
                "continueFollowUp"         // 우선순위 5: Follow-up 계속
            );

            case PLAN_GENERATION -> List.of(
                "generateTravelPlan",      // 메인: 여행 계획 생성
                "searchWithPerplexity",    // 실시간 정보 검색
                "searchTourAPI",           // 관광공사 API 검색
                "getWeatherInfo",          // 날씨 정보 조회
                "searchRegions",           // 지역 검색
                "searchAttractions"        // 관광지 검색
            );

            case FEEDBACK_REFINEMENT -> List.of(
                "modifyTravelPlan",        // 일정 수정
                "optimizeItinerary",       // 일정 최적화
                "validatePlan",            // 계획 검증
                "adjustBudget",            // 예산 조정
                "addAlternatives"          // 대안 추가
            );

            case COMPLETION -> List.of(
                "saveTravelPlan",          // 계획 저장
                "generateSummary",         // 요약 생성
                "sendNotification"         // 알림 전송
            );
        };
    }

    /**
     * Phase 전환 시도
     */
    public boolean transitionPhase(String threadId, TravelPhase newPhase) {
        PhaseContext context = phaseContextMap.get(threadId);

        if (context == null) {
            // 새로운 컨텍스트 생성
            context = new PhaseContext(threadId, TravelPhase.INITIALIZATION);
            phaseContextMap.put(threadId, context);
        }

        TravelPhase currentPhase = context.getCurrentPhase();

        if (currentPhase.canTransitionTo(newPhase)) {
            context.setCurrentPhase(newPhase);
            log.info("Phase 전환: {} → {} (threadId: {})",
                currentPhase.getKoreanName(),
                newPhase.getKoreanName(),
                threadId);
            return true;
        } else {
            log.warn("Phase 전환 실패: {} → {} 는 허용되지 않음 (threadId: {})",
                currentPhase.getKoreanName(),
                newPhase.getKoreanName(),
                threadId);
            return false;
        }
    }

    /**
     * 현재 Phase 조회
     */
    public TravelPhase getCurrentPhase(String threadId) {
        PhaseContext context = phaseContextMap.get(threadId);
        return context != null ? context.getCurrentPhase() : TravelPhase.INITIALIZATION;
    }

    /**
     * Phase별 진행률 계산
     */
    public int calculatePhaseProgress(String threadId) {
        PhaseContext context = phaseContextMap.get(threadId);
        if (context == null) {
            return 0;
        }

        return switch (context.getCurrentPhase()) {
            case INITIALIZATION -> 10;
            case INFORMATION_COLLECTION -> calculateInfoCollectionProgress(context);
            case PLAN_GENERATION -> 70;
            case FEEDBACK_REFINEMENT -> 90;
            case COMPLETION -> 100;
        };
    }

    /**
     * 정보 수집 단계 진행률 계산
     */
    private int calculateInfoCollectionProgress(PhaseContext context) {
        Map<String, Object> collectedInfo = context.getPhaseData();
        int totalRequired = 7; // 필수 정보 개수
        int collected = 0;

        // 필수 정보 체크
        if (collectedInfo.get("destination") != null) collected++;
        if (collectedInfo.get("origin") != null) collected++;
        if (collectedInfo.get("dates") != null) collected++;
        if (collectedInfo.get("duration") != null) collected++;
        if (collectedInfo.get("companions") != null) collected++;
        if (collectedInfo.get("budget") != null) collected++;
        if (collectedInfo.get("travelStyle") != null) collected++;

        // 10% ~ 60% 사이에서 진행률 계산
        return 10 + (50 * collected / totalRequired);
    }

    /**
     * Phase 컨텍스트 업데이트
     */
    public void updatePhaseData(String threadId, String key, Object value) {
        PhaseContext context = phaseContextMap.computeIfAbsent(
            threadId,
            k -> new PhaseContext(threadId, TravelPhase.INITIALIZATION)
        );
        context.addPhaseData(key, value);
    }

    /**
     * Phase 완료 조건 확인
     */
    public boolean isPhaseComplete(String threadId) {
        PhaseContext context = phaseContextMap.get(threadId);
        if (context == null) {
            return false;
        }

        return switch (context.getCurrentPhase()) {
            case INITIALIZATION -> true; // 항상 다음 단계로
            case INFORMATION_COLLECTION -> hasAllRequiredInfo(context);
            case PLAN_GENERATION -> context.getPhaseData().containsKey("generatedPlan");
            case FEEDBACK_REFINEMENT -> context.getPhaseData().containsKey("userApproval");
            case COMPLETION -> true;
        };
    }

    /**
     * 필수 정보 모두 수집되었는지 확인
     */
    private boolean hasAllRequiredInfo(PhaseContext context) {
        Map<String, Object> data = context.getPhaseData();
        // 최소 필수 정보: 목적지, 날짜, 인원
        return data.containsKey("destination") &&
               data.containsKey("dates") &&
               data.containsKey("companions");
    }

    /**
     * Phase 컨텍스트 초기화
     */
    public void clearPhaseContext(String threadId) {
        phaseContextMap.remove(threadId);
        log.info("Phase 컨텍스트 초기화 완료 (threadId: {})", threadId);
    }

    /**
     * 내부 클래스: Phase 컨텍스트
     */
    private static class PhaseContext {
        private final String threadId;
        private TravelPhase currentPhase;
        private final Map<String, Object> phaseData;
        private long phaseStartTime;

        public PhaseContext(String threadId, TravelPhase initialPhase) {
            this.threadId = threadId;
            this.currentPhase = initialPhase;
            this.phaseData = new HashMap<>();
            this.phaseStartTime = System.currentTimeMillis();
        }

        public TravelPhase getCurrentPhase() {
            return currentPhase;
        }

        public void setCurrentPhase(TravelPhase phase) {
            this.currentPhase = phase;
            this.phaseStartTime = System.currentTimeMillis();
        }

        public Map<String, Object> getPhaseData() {
            return phaseData;
        }

        public void addPhaseData(String key, Object value) {
            phaseData.put(key, value);
        }

        public long getPhaseElapsedTime() {
            return System.currentTimeMillis() - phaseStartTime;
        }
    }
}