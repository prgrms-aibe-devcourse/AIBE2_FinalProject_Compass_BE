package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.collection.service.TravelInfoCollectionService;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.response.FollowUpResponse;
import com.compass.domain.chat.orchestrator.PhaseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.function.Function;

@Slf4j
@Component("continueFollowUp")
@RequiredArgsConstructor
public class ContinueFollowUpFunction implements Function<FollowUpResponse, ChatResponse> {

    private final TravelInfoCollectionService collectionService; // 정보 수집 파이프라인
    private final PhaseManager phaseManager;                     // Phase 전환 관리

    @Override
    public ChatResponse apply(FollowUpResponse response) {
        // userId가 아닌 threadId로 로그를 남기고 처리합니다.
        log.info("Follow-up 응답 처리 시작: threadId={}", response.threadId());

        // 정보 수집의 모든 책임을 Service에 위임
        var result = collectionService.collectInfo(
                response.threadId(), // 1. 올바른 식별자인 threadId 사용
                response.userInput(),
                "conversationBasedCollector" // 2. 정확한 Bean 이름 명시
        );

        // 서비스 처리 결과를 바탕으로 응답 생성
        return result.nextQuestion()
                .map(question -> {
                    log.info("다음 Follow-up 질문 생성: {}", question);
                    return ChatResponse.builder()
                            .content(question)
                            .type("ASSISTANT_MESSAGE")
                            .phase(TravelPhase.INFORMATION_COLLECTION.name()) // 3. 현재 Phase 정보 추가
                            .nextAction("AWAIT_USER_INPUT")
                            .build();
                })
                .orElseGet(() -> {
                    log.info("모든 필수 정보 수집 완료. DB 저장 후 여행 계획 생성 단계로 전환합니다.");

                    phaseManager.savePhase(response.threadId(), TravelPhase.PLAN_GENERATION);

                    return ChatResponse.builder()
                            .content("감사합니다! 필요한 정보가 모두 모였습니다. 이제 멋진 여행 계획을 세워드릴게요. 잠시만 기다려주세요.")
                            .type("ASSISTANT_MESSAGE")
                            .phase(TravelPhase.PLAN_GENERATION.name()) // 3. 전환된 Phase 정보 추가
                            .nextAction("TRIGGER_PLAN_GENERATION")
                            .build();
                });
    }
}