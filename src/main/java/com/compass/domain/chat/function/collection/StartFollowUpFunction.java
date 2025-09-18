package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.function.followup.FollowUpQuestionSelector;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.function.Function;

// Follow-up 질문을 시작하는 Function
// SubmitTravelFormFunction에서 정보가 불완전하다고 판단했을 때 호출.
@Slf4j
@Component("startFollowUp") // nextAction "START_FOLLOW_UP"과 매핑
@RequiredArgsConstructor
public class StartFollowUpFunction implements Function<TravelFormSubmitRequest, ChatResponse> {

    private final FollowUpQuestionSelector questionSelector;

    @Override
    public ChatResponse apply(TravelFormSubmitRequest request) {
        log.info("Follow-up 질문 생성을 시작합니다. userId: {}", request.userId());

        // 질문 선택자에게 다음 질문을 찾아달라고 요청
        return questionSelector.findNextQuestion(request)
                .map(this::createQuestionResponse) // 질문이 있으면, 질문 응답 생성
                .orElseGet(() -> { // 질문할 것이 없으면 (이론적으로는 호출되지 않아야 함)
                    log.warn("모든 필수 정보가 이미 존재하지만 StartFollowUpFunction이 호출되었습니다. userId: {}", request.userId());
                    return ChatResponse.builder()
                            .content("필요한 정보는 모두 확인된 것 같아요. 이제 여행 계획을 세워볼까요?")
                            .type("ASSISTANT_MESSAGE")
                            .nextAction("TRIGGER_PLAN_GENERATION")
                            .build();
                });
    }

    private ChatResponse createQuestionResponse(String question) {
        log.info("다음 Follow-up 질문을 생성합니다: {}", question);
        return ChatResponse.builder()
                .content(question)
                .type("ASSISTANT_MESSAGE")
                .nextAction("TRIGGER_PLAN_GENERATION")
                .build();
    }


}