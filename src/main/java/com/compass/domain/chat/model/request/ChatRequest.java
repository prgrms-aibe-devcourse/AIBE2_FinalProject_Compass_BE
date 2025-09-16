package com.compass.domain.chat.model.request;

import lombok.Data;

/**
 * TODO: 구현 필요
 * 담당: Trip 개발자
 */
@Data
public class ChatRequest {
    private String message;
    private String threadId;
    private String userId;
    private Object metadata;
}
