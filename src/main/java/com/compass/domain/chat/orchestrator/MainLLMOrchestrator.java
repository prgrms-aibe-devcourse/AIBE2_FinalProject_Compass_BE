package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.service.ChatThreadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// 메인 오케스트레이터 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class MainLLMOrchestrator {

    private final IntentClassifier intentClassifier;
    private final PhaseManager phaseManager;
    private final ChatThreadService chatThreadService;
    private final ContextManager contextManager;
    private final PromptBuilder promptBuilder;
    private final ResponseGenerator responseGenerator;


    // 채팅 요청 처리
    public ChatResponse processChat(ChatRequest request) {
        log.debug("채팅 요청 처리 시작: threadId={}, userId={}",
                request.getThreadId(), request.getUserId());

        // 컨텍스트 조회 또는 생성
        var context = contextManager.getOrCreateContext(request);

        // 대화 횟수 증가
        context.incrementConversation();

        // 이전 응답이 확인을 요구했는지 체크
        var message = request.getMessage().trim();
        if (isConfirmationResponse(message)) {
            return handleConfirmationResponse(message, context, request);
        }

        // Intent 분류
        var intent = intentClassifier.classify(message);
        log.debug("분류된 Intent: {}", intent);

        // 현재 Phase 확인
        var currentPhase = TravelPhase.valueOf(context.getCurrentPhase());
        log.debug("현재 Phase: {}", currentPhase);

        // Phase 전환 처리
        var nextPhase = handlePhaseTransition(currentPhase, intent, context);

        // 응답 생성 - ResponseGenerator 사용
        return responseGenerator.generateResponse(request, intent, nextPhase, context);
    }

    // Phase 전환 처리
    private TravelPhase handlePhaseTransition(TravelPhase currentPhase, Intent intent,
                                              TravelContext context) {
        // PhaseManager의 transitionPhase 메서드 사용
        var nextPhase = phaseManager.transitionPhase(context.getThreadId(), intent, context);

        if (nextPhase != currentPhase) {
            log.info("Phase 전환: {} -> {}", currentPhase, nextPhase);
            context.setCurrentPhase(nextPhase.name());
            contextManager.updateContext(context, context.getUserId());
        }

        return nextPhase;
    }

    // 컨텍스트 초기화
    public void resetContext(String threadId, String userId) {
        contextManager.resetContext(threadId, userId);
    }

    // 진행 의사 확인 응답인지 판별
    private boolean isConfirmationResponse(String message) {
        var lowerMessage = message.toLowerCase().trim();

        // 부정적 응답 패턴 (먼저 확인)
        var negativePatterns = List.of(
            "아니", "아뇨", "안", "싫어", "싫습니다",
            "no", "n", "그만", "중단", "멈춰", "취소",
            "다시", "나중에", "보류", "필요없", "괜찮"
        );

        // 긍정적 응답 패턴
        var positivePatterns = List.of(
            "네", "예", "응", "좋아", "좋습니다", "알겠습니다",
            "그래", "오케이", "ok", "okay", "yes", "y",
            "진행", "시작", "계속", "다음", "할게요", "할래요",
            "부탁", "원해", "원합니다", "해줘", "해주세요"
        );

        // 부정 패턴 먼저 확인 (우선순위 높음)
        for (var pattern : negativePatterns) {
            if (lowerMessage.contains(pattern)) return true;
        }

        // 긍정 패턴 확인
        for (var pattern : positivePatterns) {
            if (lowerMessage.contains(pattern)) return true;
        }

        return false;
    }

    // 자연어 진행 의사 확인 처리
    private ChatResponse handleConfirmationResponse(String message, TravelContext context, ChatRequest request) {
        var lowerMessage = message.toLowerCase().trim();
        var isPositive = checkPositiveIntent(lowerMessage);

        var currentPhase = TravelPhase.valueOf(context.getCurrentPhase());

        if (isPositive) {
            log.info("사용자가 Phase 진행에 동의: currentPhase={}, message={}", currentPhase, message);

            // 다음 Phase로 전환
            var nextPhase = determineNextPhase(currentPhase);
            if (nextPhase != currentPhase) {
                context.setCurrentPhase(nextPhase.name());
                contextManager.updateContext(context, context.getUserId());
                log.info("Phase 전환: {} -> {}", currentPhase, nextPhase);
            }

            // 다음 Phase에 맞는 응답 생성
            return responseGenerator.generateResponse(request, Intent.INFORMATION_COLLECTION, nextPhase, context);
        } else {
            log.info("사용자가 Phase 진행 거부: currentPhase={}, message={}", currentPhase, message);

            // 현재 Phase 유지하며 대안 제시
            return generateAlternativeResponse(currentPhase);
        }
    }

    // 긍정적 의도 확인
    private boolean checkPositiveIntent(String message) {
        // 부정적 응답 패턴 (먼저 확인하여 제외)
        var negativePatterns = List.of(
            "아니", "아뇨", "안", "싫어", "싫습니다",
            "no", "n", "그만", "중단", "멈춰", "취소",
            "다시", "나중에", "보류", "필요없", "괜찮"
        );

        // 부정 패턴이 있으면 false 반환
        for (var pattern : negativePatterns) {
            if (message.contains(pattern)) {
                return false;
            }
        }

        // 긍정적 응답 패턴
        var positivePatterns = List.of(
            "네", "예", "응", "좋아", "좋습니다", "알겠습니다",
            "그래", "오케이", "ok", "okay", "yes", "y",
            "진행", "시작", "계속", "다음", "할게", "할래",
            "부탁", "원해", "원합니다", "해줘", "해주세요"
        );

        for (var pattern : positivePatterns) {
            if (message.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    // 다음 Phase 결정
    private TravelPhase determineNextPhase(TravelPhase currentPhase) {
        return switch (currentPhase) {
            case INITIALIZATION -> TravelPhase.INFORMATION_COLLECTION;
            case INFORMATION_COLLECTION -> TravelPhase.PLAN_GENERATION;
            case PLAN_GENERATION -> TravelPhase.FEEDBACK_REFINEMENT;
            case FEEDBACK_REFINEMENT -> TravelPhase.COMPLETION;
            case COMPLETION -> TravelPhase.COMPLETION;  // 이미 완료
        };
    }

    // 대안 응답 생성 (N 선택시)
    private ChatResponse generateAlternativeResponse(TravelPhase phase) {
        var content = switch (phase) {
            case INITIALIZATION -> "알겠습니다. 다른 도움이 필요하시면 언제든지 말씀해주세요!";
            case INFORMATION_COLLECTION -> "더 많은 정보가 필요하신가요? 천천히 알려주세요.";
            case PLAN_GENERATION -> "계획을 다시 검토해보시겠어요? 수정하고 싶은 부분이 있으신가요?";
            case FEEDBACK_REFINEMENT -> "어떤 부분이 마음에 들지 않으신가요? 구체적으로 알려주시면 수정해드릴게요.";
            case COMPLETION -> "저장하지 않고 계속 수정하시겠어요?";
        };

        return ChatResponse.builder()
            .content(content)
            .type("TEXT")
            .nextAction("WAIT_FOR_INPUT")
            .requiresConfirmation(false)
            .build();
    }
}