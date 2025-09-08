package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.dto.ChatDtos.MessageCreateDto;
import com.compass.domain.chat.dto.ChatDtos.MessageDto;
import com.compass.domain.chat.dto.ChatDtos.ThreadDto;
import com.compass.domain.chat.service.ChatModelService;
import com.compass.domain.chat.service.ChatService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    // In-memory DB Simulation (use ConcurrentHashMap for thread safety)
    private final Map<String, ThreadDto> threadsDb = new ConcurrentHashMap<>();
    private final Map<String, List<MessageDto>> messagesDb = new ConcurrentHashMap<>();

    private final ChatModelService geminiChatService;
    private final ChatModelService openAiChatService;

    public ChatServiceImpl(@Qualifier("geminiChatService") ChatModelService geminiChatService,
                           @Qualifier("openAIChatService") ChatModelService openAiChatService) {
        this.geminiChatService = geminiChatService;
        this.openAiChatService = openAiChatService;
    }


    @Override
    public ThreadDto createThread(String userId) {
        String threadId = UUID.randomUUID().toString();
        ThreadDto newThread = new ThreadDto(threadId, userId, LocalDateTime.now(), "아직 메시지가 없습니다.");

        threadsDb.put(threadId, newThread);
        messagesDb.put(threadId, new java.util.ArrayList<>());
        return newThread;
    }

    @Override
    public List<ThreadDto> getUserThreads(String userId, int skip, int limit) {
        return threadsDb.values().stream()
                .filter(thread -> userId.equals(thread.userId()))
                .sorted(Comparator.comparing(ThreadDto::createdAt).reversed())
                .skip(skip)
                .limit(limit)
                .map(thread -> {
                    List<MessageDto> messages = messagesDb.getOrDefault(thread.id(), List.of());
                    String preview = messages.isEmpty() ? "아직 메시지가 없습니다." : messages.get(messages.size() - 1).content();
                    if (preview.length() > 50) {
                        preview = preview.substring(0, 50) + "...";
                    }
                    return new ThreadDto(thread.id(), thread.userId(), thread.createdAt(), preview);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDto> addMessageToThread(String threadId, String userId, MessageCreateDto messageDto) {
        if (!threadsDb.containsKey(threadId) || !userId.equals(threadsDb.get(threadId).userId())) {
            // In a real project, throw a specific exception here.
            return null;
        }

        MessageDto userMsg = new MessageDto(
                UUID.randomUUID().toString(),
                threadId,
                "user",
                messageDto.content(),
                System.currentTimeMillis()
        );
        messagesDb.get(threadId).add(userMsg);

        String aiResponseContent = "'''" + messageDto.content() + "'''에 대한 AI 응답입니다.";
        MessageDto aiMsg = new MessageDto(
                UUID.randomUUID().toString(),
                threadId,
                "ai",
                aiResponseContent,
                System.currentTimeMillis() + 500
        );
        messagesDb.get(threadId).add(aiMsg);

        return List.of(userMsg, aiMsg);
    }

    @Override
    public List<MessageDto> getMessages(String threadId, String userId, int limit, Long before) {
        if (!threadsDb.containsKey(threadId) || !userId.equals(threadsDb.get(threadId).userId())) {
            return null;
        }

        List<MessageDto> threadMessages = messagesDb.getOrDefault(threadId, List.of());

        var messageStream = before != null ?
                threadMessages.stream().filter(m -> m.timestamp() < before) :
                threadMessages.stream();

        List<MessageDto> sortedMessages = messageStream
                .sorted(Comparator.comparing(MessageDto::timestamp))
                .collect(Collectors.toList());

        int totalSize = sortedMessages.size();
        int startIndex = Math.max(0, totalSize - limit);

        return sortedMessages.subList(startIndex, totalSize);
    }
    
    @Override
    public Map<String, Object> chatWithGemini(String message) {
        String response = geminiChatService.generateResponse(message);
        Map<String, Object> result = new HashMap<>();
        result.put("model", geminiChatService.getModelName());
        result.put("message", message);
        result.put("response", response);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @Override
    public Map<String, Object> chatWithOpenAI(String message) {
        String response = openAiChatService.generateResponse(message);
        Map<String, Object> result = new HashMap<>();
        result.put("model", openAiChatService.getModelName());
        result.put("message", message);
        result.put("response", response);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
