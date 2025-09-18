package com.compass.domain.chat.function.collection;

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


    @Override
    public ChatResponse apply(TravelFormSubmitRequest request) {
        log.info("Follow-up 질문 생성을 시작합니다. userId: {}", request.userId());

        // 1. 목적지 확인 (가장 중요한 정보)
        if (CollectionUtils.isEmpty(request.destinations()) || request.destinations().stream().anyMatch("목적지 미정"::equalsIgnoreCase)) {
            return createQuestionResponse("어디로 여행을 떠나고 싶으신가요? 도시 이름을 알려주세요.");
        }

        // 2. 여행 날짜 확인
        if (request.travelDates() == null || request.travelDates().startDate() == null || request.travelDates().endDate() == null) {
            return createQuestionResponse("여행은 언제부터 언제까지 계획하고 계신가요?");
        }

        // 3. 출발지 확인
        if (!StringUtils.hasText(request.departureLocation())) {
            return createQuestionResponse("어디서 출발하시는지 알려주시겠어요?");
        }

        // 모든 필수 정보가 있는 경우 (이론적으로는 호출되지 않아야 함)
        log.warn("모든 필수 정보가 이미 존재하지만 Follow-up 질문 생성기가 호출되었습니다. userId: {}", request.userId());
        return ChatResponse.builder()
                .content("필요한 정보는 모두 확인된 것 같아요. 이제 여행 계획을 세워볼까요?")
                .type("ASSISTANT_MESSAGE")
                .nextAction("TRIGGER_PLAN_GENERATION")
                .build();
    }

    private ChatResponse createQuestionResponse(String question) {
        return ChatResponse.builder()
                .content(question)
                .type("ASSISTANT_MESSAGE")
                .nextAction("AWAIT_USER_INPUT") // 사용자의 다음 입력을 기다림
                .build();
    }
}