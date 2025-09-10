package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.dto.ChatDtos.MessageCreateDto;
import com.compass.domain.chat.dto.ChatDtos.MessageDto;
import com.compass.domain.chat.dto.ChatDtos.ThreadDto;
import com.compass.domain.chat.service.ChatModelService;
import com.compass.domain.chat.service.ChatService;
import com.compass.domain.intent.IntentClassification;
import com.compass.domain.intent.service.IntentRouter;
import com.compass.domain.intent.service.IntentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    // 의존성 주입(DI)을 통해 외부에서 관리되는 Bean을 받도록 수정
    @Qualifier("threadsDb")
    private final Map<String, ThreadDto> threadsDb;
    @Qualifier("messagesDb")
    private final Map<String, List<MessageDto>> messagesDb;

    private final IntentService intentService;
    private final IntentRouter intentRouter;

    @Qualifier("geminiChatService")
    private final ChatModelService geminiChatService;
    @Qualifier("openAIChatService")
    private final ChatModelService openAiChatService;

    @Override
    public ThreadDto createThread(String userId) {
        String threadId = UUID.randomUUID().toString();
        ThreadDto newThread = new ThreadDto(threadId, userId, LocalDateTime.now(), "새로운 채팅", "아직 메시지가 없습니다.");
        threadsDb.put(threadId, newThread);
        messagesDb.put(threadId, new java.util.ArrayList<>());
        return newThread;
    }

    @Override
    public List<MessageDto> addMessageToThread(String threadId, String userId, MessageCreateDto messageDto) {
        ThreadDto currentThread = threadsDb.get(threadId);
        if (currentThread == null || !userId.equals(currentThread.userId())) {
            return null; // Or throw exception
        }

        // REQ-CHAT-007: 첫 메시지인 경우 제목 자동 생성
        boolean isFirstMessage = messagesDb.getOrDefault(threadId, List.of()).isEmpty();
        if (isFirstMessage) {
            String newTitle = messageDto.content().length() > 50 ? messageDto.content().substring(0, 50) : messageDto.content();
            currentThread = new ThreadDto(currentThread.id(), currentThread.userId(), currentThread.createdAt(), newTitle, currentThread.preview());
            threadsDb.put(threadId, currentThread);
        }

        MessageDto userMsg = new MessageDto(
                UUID.randomUUID().toString(),
                threadId,
                "user",
                messageDto.content(),
                System.currentTimeMillis()
        );
        messagesDb.get(threadId).add(userMsg);

        // --- CHAT1 워크플로우 실행 ---
        IntentClassification classification = intentService.classifyIntent(messageDto.content());
        String aiResponseContent = intentRouter.route(classification, messageDto.content());
        // ---------------------------

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
                    return new ThreadDto(thread.id(), thread.userId(), thread.createdAt(), thread.title(), preview);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDto> getMessages(String threadId, String userId, int limit, Long before) {
        if (!threadsDb.containsKey(threadId) || !userId.equals(threadsDb.get(threadId).userId())) {
            return null;
        }
        return messagesDb.getOrDefault(threadId, List.of()).stream()
                .sorted(Comparator.comparing(MessageDto::timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteThread(String threadId, String userId) {
        if (threadsDb.containsKey(threadId) && threadsDb.get(threadId).userId().equals(userId)) {
            threadsDb.remove(threadId);
            messagesDb.remove(threadId);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateChatThreadTitle(String threadId, String userId, String newTitle) {
        if (threadsDb.containsKey(threadId) && threadsDb.get(threadId).userId().equals(userId)) {
            ThreadDto thread = threadsDb.get(threadId);
            if (newTitle == null || newTitle.isBlank() || newTitle.length() > 50) {
                return false;
            }
            ThreadDto updatedThread = new ThreadDto(thread.id(), thread.userId(), thread.createdAt(), newTitle, thread.preview());
            threadsDb.put(threadId, updatedThread);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, Object> chatWithGemini(String message) {
        return Map.of("response", geminiChatService.generateResponse(message));
    }

    @Override
    public Map<String, Object> chatWithOpenAI(String message) {
        return Map.of("response", openAiChatService.generateResponse(message));
    }
}
