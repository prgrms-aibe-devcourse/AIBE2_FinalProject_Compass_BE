package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.followup.service.FollowUpQuestionSelector;
import com.compass.domain.chat.model.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

// Follow-up 질문을 시작하는 Function
// SubmitTravelFormFunction에서 정보가 불완전하다고 판단했을 때 호출.
@Slf4j
@Component("startFollowUp") // nextAction "START_FOLLOW_UP"과 매핑
@RequiredArgsConstructor
public class StartFollowUpFunction implements Function<com.compass.domain.chat.model.request.TravelFormSubmitRequest, ChatResponse> {

    private final FollowUpQuestionSelector questionSelector;

    @Override
    public ChatResponse apply(com.compass.domain.chat.model.request.TravelFormSubmitRequest incompleteInfo) {
        log.info("Follow-up 질문 생성을 시작합니다.");

        // 질문 선택자에게 다음 질문을 찾아달라고 요청
        String firstQuestion = questionSelector.findNextQuestion(incompleteInfo)
                .orElse("필요한 정보는 모두 확인된 것 같아요. 이제 여행 계획을 세워볼까요?"); // 방어 코드
        
        log.info("첫 Follow-up 질문을 생성합니다: {}", firstQuestion);
        return ChatResponse.builder()
                .content(firstQuestion)
                .type("ASSISTANT_MESSAGE")
                .nextAction("AWAIT_USER_INPUT") // 사용자의 다음 입력을 기다림
                .build();
    }
}