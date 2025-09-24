package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.collection.service.validator.DefaultTravelInfoValidator;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.function.Function;

@Slf4j
@Component("submitTravelForm")
@RequiredArgsConstructor
public class SubmitTravelFormFunction implements Function<TravelFormSubmitRequest, ChatResponse> {

    private final DefaultTravelInfoValidator validator;

    @Override
    public ChatResponse apply(TravelFormSubmitRequest request) {
        log.info("폼 데이터 제출 처리 시작: userId={}", request.userId());

        // 검증 책임을 Validator에게 위임
        validator.validate(request);

        // 검증 결과를 바탕으로 다음 액션 결정
        if (validator.getErrors().isEmpty()) {
            log.info("필수 정보가 모두 입력되었습니다. 여행 계획 생성 단계로 전환합니다.");
            return ChatResponse.builder()
                    .content("감사합니다! 입력해주신 정보를 바탕으로 멋진 여행 계획을 세워드릴게요. 잠시만 기다려주세요.")
                    .type("ASSISTANT_MESSAGE")
                    .nextAction("TRIGGER_PLAN_GENERATION")
                    .build();
        } else {
            log.info("필수 정보가 누락되었습니다. Follow-up 질문 단계로 전환합니다.");
            return ChatResponse.builder()
                    .content("입력해주셔서 감사합니다. 몇 가지만 더 여쭤봐도 될까요?")
                    .type("ASSISTANT_MESSAGE")
                    .nextAction("START_FOLLOW_UP")
                    .build();
        }
    }
}