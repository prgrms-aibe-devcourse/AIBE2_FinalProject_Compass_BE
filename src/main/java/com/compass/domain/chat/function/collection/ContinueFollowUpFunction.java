package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.collection.service.TravelInfoCollectionService;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.model.response.FollowUpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.function.Function;

@Slf4j
@Component("continueFollowUp")
@RequiredArgsConstructor
public class ContinueFollowUpFunction implements Function<FollowUpResponse, ChatResponse> {

    private final TravelInfoCollectionService collectionService;

    @Override
    public ChatResponse apply(FollowUpResponse response) {
        log.info("Follow-up 응답 처리 시작: userId={}", response.currentInfo().userId());

        // 정보 수집의 모든 책임을 Service에 위임
        var result = collectionService.collectInfo(
                response.currentInfo().userId(), // 실제로는 threadId를 사용해야 할 수 있음
                response.userInput(),
                "conversation" // 대화 기반 수집임을 명시
        );

        // 서비스 처리 결과를 바탕으로 응답 생성
        return result.nextQuestion()
                .map(question -> {
                    log.info("다음 Follow-up 질문 생성: {}", question);
                    return ChatResponse.builder()
                            .content(question)
                            .type("ASSISTANT_MESSAGE")
                            .nextAction("AWAIT_USER_INPUT")
                            .build();
                })
                .orElseGet(() -> {
                    log.info("모든 필수 정보 수집 완료. 여행 계획 생성 단계로 전환합니다.");
                    return ChatResponse.builder()
                            .content("감사합니다! 필요한 정보가 모두 모였습니다. 이제 멋진 여행 계획을 세워드릴게요. 잠시만 기다려주세요.")
                            .type("ASSISTANT_MESSAGE")
                            .nextAction("TRIGGER_PLAN_GENERATION")
                            .build();
                });
    }
}