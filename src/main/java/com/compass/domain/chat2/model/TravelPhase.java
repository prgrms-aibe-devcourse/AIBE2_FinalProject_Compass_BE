package com.compass.domain.chat2.model;

/**
 * TravelPhase - 여행 계획 프로세스의 단계 정의
 *
 * MainLLMOrchestrator가 현재 사용자가 어느 단계에 있는지 추적하고
 * 단계별로 적절한 Function을 선택할 수 있도록 지원
 */
public enum TravelPhase {

    /**
     * Phase 1: 초기 단계 - Intent 분류 및 프로세스 시작
     */
    INITIALIZATION("초기화", "사용자 의도 파악 및 프로세스 시작"),

    /**
     * Phase 2: 정보 수집 단계 - 여행 정보 수집
     * - 빠른 입력 폼 (우선순위 1)
     * - OCR 자동 추출 (우선순위 2)
     * - Follow-up 질문 (우선순위 3)
     */
    INFORMATION_COLLECTION("정보 수집", "여행 필수 정보 수집 중"),

    /**
     * Phase 3: 계획 생성 단계 - 수집된 정보로 여행 일정 생성
     * - DB 장소 검색
     * - Perplexity 실시간 검색
     * - 날씨 정보 통합
     * - LLM 일정 생성
     */
    PLAN_GENERATION("계획 생성", "맞춤형 여행 계획 생성 중"),

    /**
     * Phase 4: 피드백 단계 - 생성된 계획 수정 및 최적화
     * - 사용자 피드백 수렴
     * - 일정 수정
     * - 최종 확정
     */
    FEEDBACK_REFINEMENT("피드백 처리", "여행 계획 수정 및 최적화"),

    /**
     * Phase 5: 완료 단계 - 최종 계획 전달 및 저장
     */
    COMPLETION("완료", "여행 계획 완성 및 저장");

    private final String koreanName;
    private final String description;

    TravelPhase(String koreanName, String description) {
        this.koreanName = koreanName;
        this.description = description;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 다음 단계로 전환 가능한지 확인
     */
    public boolean canTransitionTo(TravelPhase nextPhase) {
        return switch (this) {
            case INITIALIZATION -> nextPhase == INFORMATION_COLLECTION;
            case INFORMATION_COLLECTION -> nextPhase == PLAN_GENERATION || nextPhase == INFORMATION_COLLECTION;
            case PLAN_GENERATION -> nextPhase == FEEDBACK_REFINEMENT || nextPhase == COMPLETION;
            case FEEDBACK_REFINEMENT -> nextPhase == PLAN_GENERATION || nextPhase == COMPLETION;
            case COMPLETION -> false; // 완료 후에는 전환 불가
        };
    }

    /**
     * 현재 Phase에서 사용 가능한 Function 카테고리
     */
    public String[] getAvailableFunctionCategories() {
        return switch (this) {
            case INITIALIZATION -> new String[]{"intent_analysis", "session_init"};
            case INFORMATION_COLLECTION -> new String[]{"quick_form", "ocr", "followup"};
            case PLAN_GENERATION -> new String[]{"search", "weather", "generate"};
            case FEEDBACK_REFINEMENT -> new String[]{"modify", "optimize", "validate"};
            case COMPLETION -> new String[]{"save", "notify", "summary"};
        };
    }
}