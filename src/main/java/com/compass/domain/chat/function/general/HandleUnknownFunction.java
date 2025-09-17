package com.compass.domain.chat.function.general;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.function.Function;

// Intent로 분류할 수 없는 요청을 처리하는 Function
// 사용자에게 이해하지 못했음을 알리고 도움말을 제공합니다.
@Component("handleUnknownFunction")
public class HandleUnknownFunction implements Function<HandleUnknownFunction.UnknownRequest, HandleUnknownFunction.DefaultResponse> {

    // 분류 불가능한 요청
    public record UnknownRequest(
            @JsonProperty(required = true)
            @JsonPropertyDescription("분류하지 못한 사용자의 원본 메시지")
            String originalMessage
    ) {}

    // 기본 응답
    public record DefaultResponse(String message) {}

    @Override
    public DefaultResponse apply(UnknownRequest unknownRequest) {
        // 사용자의 원본 메시지를 로깅하거나 분석할 수 있습니다.
        var originalMessage = unknownRequest.originalMessage();

        // 사용자에게 반환할 기본 응답 메시지
        var responseMessage = "죄송합니다, 요청하신 내용을 이해하지 못했습니다. '여행 계획 짜줘' 또는 '파리 날씨 알려줘'와 같이 명확하게 말씀해주시겠어요?";

        return new DefaultResponse(responseMessage);
    }
}
