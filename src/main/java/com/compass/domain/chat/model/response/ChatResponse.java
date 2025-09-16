package com.compass.domain.chat.model.response;

import lombok.Data;
import lombok.Builder;

/**
 * TODO: 구현 필요
 * 담당: Trip 개발자
 */
@Data
@Builder
public class ChatResponse {
    private String content;
    private String type;
    private Object data;
    private String nextAction;
}
