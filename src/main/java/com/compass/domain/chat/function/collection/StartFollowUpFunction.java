package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.collection.service.FollowUpOrchestrator;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.function.Function;

@Slf4j
@Component("startFollowUp")
@RequiredArgsConstructor
public class StartFollowUpFunction implements Function<TravelFormSubmitRequest, ChatResponse> {

    private final FollowUpOrchestrator followUpOrchestrator;

    @Override
    public ChatResponse apply(TravelFormSubmitRequest incompleteInfo) {
        log.info("Follow-up 질문 생성 시작");

        // 질문 선택 책임을 Orchestrator에 위임
        String firstQuestion = followUpOrchestrator.determineNextQuestion(incompleteInfo)
                .orElse("필요한 정보는 모두 확인된 것 같아요. 이제 여행 계획을 세워볼까요?");

        log.info("첫 Follow-up 질문 생성: {}", firstQuestion);
        return ChatResponse.builder()
                .content(firstQuestion)
                .type("ASSISTANT_MESSAGE")
                .nextAction("AWAIT_USER_INPUT")
                .build();
    }
}