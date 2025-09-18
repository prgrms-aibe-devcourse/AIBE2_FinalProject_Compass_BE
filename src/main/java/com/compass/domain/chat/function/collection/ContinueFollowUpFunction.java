package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.function.followup.FollowUpQuestionSelector;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

// Follow-up 대화를 계속 진행할지, 아니면 종료할지를 결정하는 Function
@Slf4j
@Component("continueFollowUp")
@RequiredArgsConstructor
public class ContinueFollowUpFunction implements Function<TravelFormSubmitRequest, ChatResponse> {

    private final FollowUpQuestionSelector questionSelector;

    @Override
    public ChatResponse apply(TravelFormSubmitRequest updatedInfo) {
        log.info("Follow-up 대화 지속 여부를 결정합니다. userId: {}", updatedInfo.userId());

        // 질문 선택자에게 다음 질문을 찾아달라고 요청
        return questionSelector.findNextQuestion(updatedInfo)
                .map(this::createQuestionResponse) // 질문이 있으면, 다음 질문 응답 생성
                .orElseGet(() -> { // 질문할 것이 없으면, 정보 수집 완료
                    log.info("모든 필수 정보가 수집되었습니다. 여행 계획 생성 단계로 전환합니다. userId: {}", updatedInfo.userId());
                    return ChatResponse.builder()
                            .content("감사합니다! 필요한 정보가 모두 모였습니다. 이제 멋진 여행 계획을 세워드릴게요. 잠시만 기다려주세요.")
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
                .nextAction("AWAIT_USER_INPUT") // 사용자의 다음 입력을 기다림
                .build();
    }
}