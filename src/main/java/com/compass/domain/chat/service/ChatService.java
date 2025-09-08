package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.ChatDtos.MessageCreateDto;
import com.compass.domain.chat.dto.ChatDtos.MessageDto;
import com.compass.domain.chat.dto.ChatDtos.ThreadDto;

import java.util.List;
import java.util.Map;

public interface ChatService {
    /**
     * REQ-CHAT-001: 채팅방 생성 로직
     */
    ThreadDto createThread(String userId);

    /**
     * REQ-CHAT-002: 채팅 목록 조회 로직
     */
    List<ThreadDto> getUserThreads(String userId, int skip, int limit);

    /**
     * REQ-CHAT-003: 메시지 전송 로직
     */
    List<MessageDto> addMessageToThread(String threadId, String userId, MessageCreateDto messageDto);

    /**
     * REQ-CHAT-004: 대화 조회 로직
     */
    List<MessageDto> getMessages(String threadId, String userId, int limit, Long before);

    Map<String, Object> chatWithGemini(String message);

    Map<String, Object> chatWithOpenAI(String message);
}
