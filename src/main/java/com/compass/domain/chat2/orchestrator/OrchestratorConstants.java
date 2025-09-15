package com.compass.domain.chat2.orchestrator;

/**
 * OrchestratorConstants - 오케스트레이터 상수 정의
 *
 * 오케스트레이션 과정에서 사용되는 모든 상수를 중앙 관리
 */
public final class OrchestratorConstants {

    private OrchestratorConstants() {
        // 유틸리티 클래스 생성 방지
    }

    // 에러 메시지
    public static final String ERROR_API_LIMIT_MESSAGE = "현재 API 사용량이 제한에 도달했습니다. 잠시 후 다시 시도해주세요.";
    public static final String ERROR_GENERAL_MESSAGE = "요청을 처리하는 중 오류가 발생했습니다. 다시 시도해주세요.";
    public static final String ERROR_NULL_RESPONSE = "죄송합니다. 요청을 처리하는 중 오류가 발생했습니다.";

    // 로그 메시지
    public static final String LOG_ORCHESTRATION_START = "🎯 MainLLMOrchestrator 시작 - ThreadId: {}, UserId: {}";
    public static final String LOG_INTENT_CLASSIFIED = "📊 Intent 분류 완료: {}";
    public static final String LOG_FUNCTIONS_SELECTED = "🔧 선택된 Functions: {}";
    public static final String LOG_ORCHESTRATION_COMPLETE = "✅ MainLLMOrchestrator 완료 - ThreadId: {}";
    public static final String LOG_ORCHESTRATION_ERROR = "❌ MainLLMOrchestrator 오류 - ThreadId: {}";
    public static final String LOG_FUNCTION_STATUS = "📦 등록된 Functions:";
    public static final String LOG_FUNCTION_ITEM = "  - {}: {}";

    // 시스템 프롬프트
    public static final String SYSTEM_PROMPT = """
        당신은 Compass 여행 계획 도우미입니다.
        사용자의 요청을 이해하고 적절한 기능을 선택하여 최고의 여행 계획을 제공합니다.

        현재 사용 가능한 기능:
        - analyzeUserInput: 사용자 입력에서 여행 정보 추출
        - startFollowUp: 누락된 정보 수집을 위한 Follow-up 시작
        - generateTravelPlan: 여행 계획 생성
        - searchWithPerplexity: 트렌디한 장소 검색
        - uploadToS3AndOCR: 이미지 OCR 처리
        - extractFlightInfo: 항공권 정보 추출
        - trackApiUsage: API 사용량 추적
        - processQuickInput: 빠른 입력 폼 처리

        사용자 요청을 처리하고 자연스러운 대화로 응답하세요.
        """;

    // 프롬프트 템플릿
    public static final String PROMPT_TEMPLATE = """
        {systemPrompt}

        대화 ID: {threadId}
        사용자 ID: {userId}

        사용자 요청: {userInput}

        위 요청을 처리하고 친절하게 응답해주세요.
        """;
}