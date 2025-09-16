package com.compass.domain.chat.function.general;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;
import java.util.function.Function;

// 일반적인 비여행 관련 질문을 처리하는 Function
// 날씨, 환율 등 정보성 질문에 답변합니다.
@Component("handleGeneralQuestionFunction")
public class HandleGeneralQuestionFunction implements Function<HandleGeneralQuestionFunction.GeneralQuestion, HandleGeneralQuestionFunction.GeneralResponse> {

    // 일반 질문 요청
    public record GeneralQuestion(
            @JsonProperty(required = true)
            @JsonPropertyDescription("사용자의 일반 질문")
            String question
    ) {}

    // 일반 질문에 대한 응답
    public record GeneralResponse(String answer) {}

    @Override
    public GeneralResponse apply(GeneralQuestion generalQuestion) {
        // TODO: 일반 질문을 처리하는 로직 구현 필요.
        // 외부 API 또는 다른 서비스를 호출하여 날씨, 환율 등의 정보를 가져올 수 있습니다.
        // 현재는 임시 응답을 반환합니다.

        var userQuestion = generalQuestion.question();
        var responseMessage = "일반 질문입니다: \"" + userQuestion + "\". 답변은 곧 구현될 예정입니다.";

        return new GeneralResponse(responseMessage);
    }
}
