package com.compass.domain.chat2.config;

import com.compass.domain.chat.service.TravelInfoCollectionService;
import com.compass.domain.chat.service.TravelQuestionFlowEngine;
import com.compass.domain.chat2.dto.AnalyzeUserInputRequest;
import com.compass.domain.chat2.dto.AnalyzeUserInputResponse;
import com.compass.domain.chat2.dto.StartFollowUpRequest;
import com.compass.domain.chat2.dto.StartFollowUpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

import static com.compass.domain.chat2.config.Chat2FunctionConstants.*;

/**
 * Chat2FunctionConfiguration - CHAT2 도메인 전용 Function 설정
 *
 * REQ-CHAT2-006: Function Calling 구현
 * REQ-CHAT2-008: 도메인별 Function 제공
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Chat2FunctionConfiguration {

    private final TravelInfoCollectionService travelInfoCollectionService;
    private final TravelQuestionFlowEngine travelQuestionFlowEngine;

    /**
     * REQ-CHAT2-007: analyzeUserInput - 사용자 입력 분석
     * 사용자 입력에서 여행 정보를 추출하는 Function
     */
    @Bean("analyzeUserInput")
    @Description("사용자 입력에서 여행 정보(목적지, 날짜, 예산 등)를 추출합니다")
    public Function<AnalyzeUserInputRequest, AnalyzeUserInputResponse> analyzeUserInput() {
        return request -> {
            log.info("🔍 사용자 입력 분석 시작 - ThreadId: {}", request.getThreadId());

            try {
                // 사용자 입력 분석 및 정보 추출
                var travelInfo = travelInfoCollectionService.extractTravelInfo(
                    request.getUserInput(),
                    request.getThreadId()
                );

                // 추출된 정보 검증
                boolean isComplete = travelInfoCollectionService.isCollectionComplete(request.getThreadId());

                return AnalyzeUserInputResponse.builder()
                    .status("SUCCESS")
                    .extractedInfo(travelInfo)
                    .isComplete(isComplete)
                    .missingFields(travelInfoCollectionService.getMissingFields(request.getThreadId()))
                    .confidence(0.85)
                    .build();

            } catch (Exception e) {
                log.error("사용자 입력 분석 실패", e);
                return AnalyzeUserInputResponse.builder()
                    .status("ERROR")
                    .errorCode("CHAT2_007")
                    .message("입력 분석 중 오류가 발생했습니다")
                    .build();
            }
        };
    }

    /**
     * REQ-CHAT2-009: startFollowUp - Follow-up 질문 시작
     * 누락된 정보 수집을 위한 Follow-up 프로세스 시작
     */
    @Bean("startFollowUp")
    @Description("누락된 여행 정보 수집을 위한 Follow-up 질문을 시작합니다")
    public Function<StartFollowUpRequest, StartFollowUpResponse> startFollowUp() {
        return request -> {
            log.info("🔄 Follow-up 프로세스 시작 - ThreadId: {}", request.getThreadId());

            try {
                // 현재 수집 상태 확인
                var collectionState = travelInfoCollectionService.getCollectionState(request.getThreadId());

                if (collectionState == null) {
                    // 새로운 Follow-up 세션 시작
                    travelInfoCollectionService.initializeCollection(request.getThreadId());
                }

                // 다음 질문 생성
                String nextQuestion = travelQuestionFlowEngine.generateNextQuestion(
                    request.getThreadId(),
                    request.getUserId()
                );

                // 진행률 계산
                double progress = travelInfoCollectionService.calculateProgress(request.getThreadId());

                return StartFollowUpResponse.builder()
                    .status("SUCCESS")
                    .question(nextQuestion)
                    .progress(progress)
                    .threadId(request.getThreadId())
                    .requiresMoreInfo(progress < 1.0)
                    .build();

            } catch (Exception e) {
                log.error("Follow-up 시작 실패", e);
                return StartFollowUpResponse.builder()
                    .status("ERROR")
                    .errorCode("CHAT2_009")
                    .message("Follow-up 질문 생성 중 오류가 발생했습니다")
                    .build();
            }
        };
    }

    /**
     * REQ-CHAT2-003: classifyIntent Function
     * Intent 분류를 위한 Function (IntentClassificationService에서 처리)
     */
    @Bean("classifyIntent")
    @Description("사용자 입력의 의도(Intent)를 분류합니다")
    public Function<String, String> classifyIntent() {
        return userInput -> {
            log.info("📊 Intent 분류 Function 호출");
            // IntentClassificationService는 MainLLMOrchestrator에서 직접 호출
            // 이 Function은 외부 도메인에서 필요시 사용
            return "INTENT_CLASSIFICATION";
        };
    }

    /**
     * REQ-CHAT2-010: generateResponse - 최종 응답 생성
     * 수집된 정보와 처리 결과를 바탕으로 최종 응답 생성
     */
    @Bean("generateFinalResponse")
    @Description("처리 결과를 바탕으로 사용자에게 전달할 최종 응답을 생성합니다")
    public Function<Object, String> generateFinalResponse() {
        return result -> {
            log.info("📝 최종 응답 생성");

            // 결과 타입에 따른 응답 생성
            if (result == null) {
                return "요청을 처리했습니다.";
            }

            // 여행 계획인 경우
            if (result.toString().contains("itinerary")) {
                return String.format(
                    "🎉 여행 계획이 완성되었습니다!\n\n%s\n\n즐거운 여행 되세요!",
                    result.toString()
                );
            }

            // Follow-up이 필요한 경우
            if (result.toString().contains("followUp")) {
                return String.format(
                    "여행 계획을 완성하기 위해 추가 정보가 필요합니다.\n\n%s",
                    result.toString()
                );
            }

            // 기본 응답
            return result.toString();
        };
    }

    /**
     * Function 등록 상태 로깅
     */
    @Bean
    public FunctionCallback chat2FunctionLogger() {
        log.info("✅ CHAT2 Functions 등록 완료:");
        log.info("  - analyzeUserInput: 사용자 입력 분석");
        log.info("  - startFollowUp: Follow-up 질문 시작");
        log.info("  - classifyIntent: Intent 분류");
        log.info("  - generateFinalResponse: 최종 응답 생성");

        // Dummy callback for logging
        return FunctionCallback.builder()
            .function("chat2Logger", (Object input) -> "logged")
            .description("CHAT2 Function 로깅")
            .build();
    }
}