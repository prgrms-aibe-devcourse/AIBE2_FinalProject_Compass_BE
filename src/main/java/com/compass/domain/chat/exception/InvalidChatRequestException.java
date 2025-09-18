package com.compass.domain.chat.exception;

// 잘못된 채팅 요청 예외
public class InvalidChatRequestException extends RuntimeException {

    public InvalidChatRequestException(String message) {
        super(message);
    }

    public InvalidChatRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}