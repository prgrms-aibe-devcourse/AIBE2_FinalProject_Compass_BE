package com.compass.domain.chat.function.general;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

// 날씨, 환율 등 일반 정보를 제공하는 Function
@Component("provideGeneralInfoFunction")
public class ProvideGeneralInfoFunction implements Function<ProvideGeneralInfoFunction.InfoRequest, ProvideGeneralInfoFunction.InfoResponse> {

    // 정보 요청 타입 정의
    public enum InfoType {
        WEATHER, // 날씨
        EXCHANGE_RATE // 환율
    }

    // 일반 정보 요청
    public record InfoRequest(
            @JsonProperty(required = true)
            @JsonPropertyDescription("요청할 정보의 타입 (예: WEATHER, EXCHANGE_RATE)")
            InfoType infoType,

            @JsonProperty
            @JsonPropertyDescription("정보 조회에 필요한 파라미터 (예: 도시, 통화)")
            Map<String, String> parameters
    ) {}

    // 일반 정보 응답
    public record InfoResponse(String requestedInfo, Map<String, Object> data) {}

    @Override
    public InfoResponse apply(InfoRequest infoRequest) {
        // TODO: infoType에 따라 외부 API를 호출하여 실제 정보를 조회하는 로직 구현 필요

        var infoType = infoRequest.infoType();
        var params = infoRequest.parameters();

        // 임시 응답 생성
        var responseMessage = infoType + "에 대한 정보를 조회합니다. 파라미터: " + params;
        var responseData = Map.<String, Object>of("status", "pending", "details", responseMessage);

        return new InfoResponse(responseMessage, responseData);
    }
}
