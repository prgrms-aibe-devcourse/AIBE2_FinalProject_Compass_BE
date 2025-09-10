package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.ChatDtos.MessageCreateDto;
import com.compass.domain.chat.dto.ChatDtos.MessageDto;
import com.compass.domain.chat.dto.ChatDtos.ThreadDto;

import java.util.List;
import java.util.Map;

public interface ChatService {

    ThreadDto createThread(String userId);

    List<ThreadDto> getUserThreads(String userId, int skip, int limit);

    List<MessageDto> addMessageToThread(String threadId, String userId, MessageCreateDto messageDto);

    List<MessageDto> getMessages(String threadId, String userId, int limit, Long before);

    boolean deleteThread(String threadId, String userId);

    boolean updateChatThreadTitle(String threadId, String userId, String newTitle);

    Map<String, Object> chatWithGemini(String message);

    Map<String, Object> chatWithOpenAI(String message);
}
