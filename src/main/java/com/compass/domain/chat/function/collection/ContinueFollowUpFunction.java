package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.function.followup.FollowUpQuestionSelector;
import com.compass.domain.chat.model.request.AnalyzeUserInputRequest;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.model.response.FollowUpResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

// Follow-up 대화를 계속 진행할지, 아니면 종료할지를 결정하는 Function
@Slf4j
@Component("continueFollowUp")
@RequiredArgsConstructor
public class ContinueFollowUpFunction implements Function<FollowUpResponse, ChatResponse> {

    private final AnalyzeUserInputFunction analyzer; // 사용자 입력 분석기
    private final FollowUpQuestionSelector questionSelector;

    @Override
    public ChatResponse apply(FollowUpResponse response) {
        log.info("Follow-up 응답을 처리하고 대화를 계속합니다. userId: {}", response.currentInfo().userId());

        // 1. 사용자 답변을 분석하여 TravelInfo를 업데이트
        TravelFormSubmitRequest updatedInfo = analyzer.apply(
            new AnalyzeUserInputRequest(response.currentInfo().userId(), response.userInput(), response.currentInfo())
        );

        // 2. 업데이트된 정보로 다음 질문을 찾음 (완성도 재평가)
        return questionSelector.findNextQuestion(updatedInfo)
                .map(nextQuestion -> {
                    // 3-1. 다음 질문이 있으면, 질문을 생성
                    log.info("다음 Follow-up 질문을 생성합니다: {}", nextQuestion);
                    return ChatResponse.builder()
                            .content(nextQuestion)
                            .type("ASSISTANT_MESSAGE")
                            .nextAction("AWAIT_USER_INPUT")
                            .build();
                })
                .orElseGet(() -> {
                    // 3-2. 모든 정보가 수집되었으면, Phase 전환
                    log.info("모든 필수 정보가 수집되었습니다. 여행 계획 생성 단계로 전환합니다. userId: {}", updatedInfo.userId());
                    return ChatResponse.builder()
                            .content("감사합니다! 필요한 정보가 모두 모였습니다. 이제 멋진 여행 계획을 세워드릴게요. 잠시만 기다려주세요.")
                            .type("ASSISTANT_MESSAGE")
                            .nextAction("TRIGGER_PLAN_GENERATION")
                            .build();
                });
    }
}