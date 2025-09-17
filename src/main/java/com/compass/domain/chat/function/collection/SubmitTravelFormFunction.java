package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.function.Function;

// 빠른 입력 폼 제출을 처리하는 Function
// 제출된 데이터의 완성도를 검증하고, 다음 단계(Phase)를 결정합니다.
@Slf4j
@Component("submitTravelForm")
@RequiredArgsConstructor
public class SubmitTravelFormFunction implements Function<TravelFormSubmitRequest, ChatResponse> {

    @Override
    public ChatResponse apply(TravelFormSubmitRequest request) {
        log.info("폼 데이터 제출을 처리합니다. userId: {}", request.userId());
        log.debug("제출된 데이터: {}", request);

        // 필수 정보 완성도 검증
        if (isFormComplete(request)) {
            // 정보가 완성된 경우
            log.info("필수 정보가 모두 입력되었습니다. 여행 계획 생성 단계로 전환합니다.");

            // 다음 단계 안내 응답 생성
            return ChatResponse.builder()
                    .content("감사합니다! 입력해주신 정보를 바탕으로 멋진 여행 계획을 세워드릴게요. 잠시만 기다려주세요.")
                    .type("ASSISTANT_MESSAGE")
                    .nextAction("TRIGGER_PLAN_GENERATION") // 오케스트레이터가 이 액션을 보고 계획 생성 Function을 호출
                    .build();
        } else {
            // 정보가 불완전한 경우
            log.info("필수 정보가 누락되었습니다. Follow-up 질문 단계로 전환합니다.");

            // Follow-up 시작 안내 응답 생성
            return ChatResponse.builder()
                    .content("입력해주셔서 감사합니다. 몇 가지만 더 여쭤봐도 될까요?")
                    .type("ASSISTANT_MESSAGE")
                    .nextAction("START_FOLLOW_UP") // 오케스트레이터가 이 액션을 보고 Follow-up Function을 호출
                    .build();
        }
    }

    // 폼 데이터 완성도 검증 로직
    // 필수 필드: 목적지, 출발지, 여행 날짜
    private boolean isFormComplete(TravelFormSubmitRequest request) {
        // 1. 목적지 검증 (비어있지 않고, "목적지 미정"이 아닌 경우)
        boolean isDestinationValid = !CollectionUtils.isEmpty(request.destinations()) &&
                request.destinations().stream().noneMatch("목적지 미정"::equalsIgnoreCase);

        // 2. 출발지 검증
        boolean isDepartureValid = StringUtils.hasText(request.departureLocation());

        // 3. 여행 날짜 검증
        boolean isDatesValid = request.travelDates() != null &&
                StringUtils.hasText(request.travelDates().get("startDate")) &&
                StringUtils.hasText(request.travelDates().get("endDate"));

        return isDestinationValid && isDepartureValid && isDatesValid;
    }
}