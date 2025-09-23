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

        // '목적지 미정' 케이스를 더 유연하게 확인합니다.
        // 프론트엔드에서 '목적지 미정'을 빈 배열([])로 보내거나, 리스트에 "목적지 미정" 문자열을 포함할 수 있습니다.
        boolean isDestinationEffectivelyUndecided =
                request.destinations() == null ||
                request.destinations().isEmpty() ||
                request.destinations().stream().anyMatch(d -> d == null || d.isBlank() || "목적지 미정".equalsIgnoreCase(d));

        if (isDestinationEffectivelyUndecided) {
            log.info("'목적지 미정' 또는 빈 목적지 확인됨. 목적지 추천 단계로 전환합니다.");
            return ChatResponse.builder()
                    .content("어디로 갈지 고민이시군요! 제가 몇 군데 추천해 드릴게요.")
                    .type("ASSISTANT_MESSAGE")
                    .nextAction("RECOMMEND_DESTINATIONS")
                    .build();
        }

        //목적지 미정이 아닌 경우, 기존 검증 로직을 수행
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
