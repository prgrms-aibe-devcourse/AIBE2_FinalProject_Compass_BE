package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.collection.service.FollowUpOrchestrator;
import com.compass.domain.chat.function.collection.ShowQuickInputFormFunction;
import com.compass.domain.chat.collection.service.validator.TravelInfoValidator;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Component("startFollowUp")
@RequiredArgsConstructor
public class StartFollowUpFunction implements Function<TravelFormSubmitRequest, ChatResponse> {

    private final FollowUpOrchestrator followUpOrchestrator;
    private final TravelInfoValidator validator; // 검증기를 주입받습니다.
    private final ShowQuickInputFormFunction showQuickInputFormFunction; // 폼 생성기를 주입받습니다.

    @Override
    public ChatResponse apply(TravelFormSubmitRequest incompleteInfo) {
        log.info("Follow-up 질문 생성 시작");

        // 1. 먼저 제출된 정보에 대한 유효성 검사를 다시 실행
        validator.validate(incompleteInfo);
        List<String> errors = validator.getErrors();

        String firstQuestion;

        // 2. 만약 유효성 검사 오류가 있다면, 그 오류 메시지를 첫 질문으로 사용
        if (!errors.isEmpty()) {
            String errorMessage = errors.get(0);
            log.warn("입력 데이터에 유효성 검사 오류가 발견되어 폼을 다시 표시합니다: {}", errorMessage);

            // 폼 구조를 가져옵니다.
            var formDto = showQuickInputFormFunction.apply(new ShowQuickInputFormFunction.Request());

            // 오류 메시지와 함께 폼을 다시 보여주는 응답을 생성
            return ChatResponse.builder()
                    .content(errorMessage) // 오류 메시지를 사용자에게 안내
                    .type("QUICK_FORM") // 프론트엔드가 폼을 렌더링하도록 타입 지정
                    .data(formDto) // 폼 구조 데이터 전달
                    .nextAction("AWAIT_FORM_SUBMISSION") // 다음 액션을 폼 제출 대기로 명시
                    .build();
        } else {
            // 3. 유효성 오류가 없다면, 누락된 정보를 찾기 위해 기존 로직을 실행
            firstQuestion = followUpOrchestrator.determineNextQuestion(incompleteInfo)
                    .orElse("필요한 정보는 모두 확인된 것 같아요. 이제 여행 계획을 세워볼까요?");

            log.info("첫 Follow-up 질문 생성: {}", firstQuestion);
            return ChatResponse.builder()
                    .content(firstQuestion)
                    .type("ASSISTANT_MESSAGE")
                    .nextAction("AWAIT_USER_INPUT")
                    .build();
        }
    }
}