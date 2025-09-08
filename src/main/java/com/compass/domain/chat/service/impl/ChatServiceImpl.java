package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.dto.ChatDtos.MessageCreateDto;
import com.compass.domain.chat.dto.ChatDtos.MessageDto;
import com.compass.domain.chat.dto.ChatDtos.ThreadDto;
import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.exception.ChatThreadNotFoundException;
import com.compass.domain.chat.exception.UnauthorizedThreadAccessException;
import com.compass.domain.chat.repository.ChatMessageRepository;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.service.ChatModelService;
import com.compass.domain.chat.service.ChatService;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatModelService geminiChatService;
    private final ChatModelService openAiChatService;

    public ChatServiceImpl(
            ChatThreadRepository chatThreadRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            @Qualifier("geminiChatService") ChatModelService geminiChatService,
            @Qualifier("openAIChatService") ChatModelService openAiChatService) {
        this.chatThreadRepository = chatThreadRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.geminiChatService = geminiChatService;
        this.openAiChatService = openAiChatService;
    }


    @Override
    @Transactional
    public ThreadDto createThread(String userId) {
        log.debug("Creating new chat thread for user: {}", userId);
        
        // Parse userId to Long and find user
        Long userIdLong = Long.parseLong(userId);
        User user = userRepository.findById(userIdLong)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Create new thread with empty title (will be set on first message)
        ChatThread thread = ChatThread.builder()
            .user(user)
            .title("새 대화") // Default title until first message
            .build();
        
        thread = chatThreadRepository.save(thread);
        log.info("Created new chat thread with ID: {} for user: {}", thread.getId(), userId);
        
        return new ThreadDto(
            thread.getId(),
            userId,
            thread.getCreatedAt(),
            thread.getLatestMessagePreview()
        );
    }

    @Override
    public List<ThreadDto> getUserThreads(String userId, int skip, int limit) {
        log.debug("Getting threads for user: {}, skip: {}, limit: {}", userId, skip, limit);
        
        Long userIdLong = Long.parseLong(userId);
        
        // Use pagination
        PageRequest pageable = PageRequest.of(
            skip / limit, // page number
            limit,
            Sort.by(Sort.Direction.DESC, "lastMessageAt", "createdAt")
        );
        
        List<ChatThread> threads = chatThreadRepository.findByUserId(userIdLong, pageable).getContent();
        
        return threads.stream()
            .map(thread -> {
                // Log for debugging
                log.debug("Thread ID: {}, Title: {}, Preview: {}", 
                         thread.getId(), thread.getTitle(), thread.getLatestMessagePreview());
                return new ThreadDto(
                    thread.getId(),
                    userId,
                    thread.getCreatedAt(),
                    thread.getTitle() != null && !thread.getTitle().equals("새 대화") 
                        ? thread.getTitle() 
                        : thread.getLatestMessagePreview()
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MessageDto> addMessageToThread(String threadId, String userId, MessageCreateDto messageDto) {
        log.debug("Adding message to thread: {} for user: {}", threadId, userId);
        
        Long userIdLong = Long.parseLong(userId);
        
        // Verify thread exists and belongs to user
        ChatThread thread = chatThreadRepository.findByIdAndUserId(threadId, userIdLong)
            .orElseThrow(() -> new ChatThreadNotFoundException(threadId, userIdLong));
        
        // Create and save user message
        ChatMessage userMessage = ChatMessage.builder()
            .thread(thread)
            .role("user")
            .content(messageDto.content())
            .timestamp(LocalDateTime.now())
            .build();
        
        userMessage = chatMessageRepository.save(userMessage);
        
        // Update thread title if it's the first user message
        // Check if this is the first user message in the thread
        long userMessageCount = chatMessageRepository.countByThreadId(threadId);
        if (userMessageCount == 1) { // The message we just saved is the first one
            thread.updateTitleFromFirstMessage(messageDto.content());
            chatThreadRepository.save(thread);
            log.info("Updated thread title to: {}", thread.getTitle());
        }
        
        // Get recent messages for context (last 10 messages)
        List<ChatMessage> recentMessages = chatMessageRepository.findLatestMessagesByThreadId(threadId, 10);
        
        // Build context from recent messages
        StringBuilder contextBuilder = new StringBuilder();
        if (!recentMessages.isEmpty()) {
            contextBuilder.append("이전 대화 내용:\n");
            for (int i = recentMessages.size() - 1; i >= 0; i--) {
                ChatMessage msg = recentMessages.get(i);
                contextBuilder.append(msg.getRole().equals("user") ? "사용자: " : "AI: ");
                contextBuilder.append(msg.getContent()).append("\n");
            }
            contextBuilder.append("\n새로운 질문: ");
        }
        contextBuilder.append(messageDto.content());
        
        // Generate AI response using Gemini with context
        String aiResponseContent;
        try {
            aiResponseContent = geminiChatService.generateResponse(contextBuilder.toString());
        } catch (Exception e) {
            log.error("Error generating AI response, using fallback", e);
            aiResponseContent = "죄송합니다. 일시적인 오류가 발생했습니다. 다시 시도해주세요.";
        }
        
        // Create and save AI message
        ChatMessage aiMessage = ChatMessage.builder()
            .thread(thread)
            .role("assistant")
            .content(aiResponseContent)
            .timestamp(LocalDateTime.now())
            .build();
        
        aiMessage = chatMessageRepository.save(aiMessage);
        
        // Update thread's last message time
        thread.setLastMessageAt(aiMessage.getTimestamp());
        chatThreadRepository.save(thread);
        
        // Convert to DTOs
        MessageDto userDto = new MessageDto(
            userMessage.getId().toString(),
            threadId,
            userMessage.getRole(),
            userMessage.getContent(),
            userMessage.getTimestampMillis()
        );
        
        MessageDto aiDto = new MessageDto(
            aiMessage.getId().toString(),
            threadId,
            aiMessage.getRole(),
            aiMessage.getContent(),
            aiMessage.getTimestampMillis()
        );
        
        return List.of(userDto, aiDto);
    }

    @Override
    public List<MessageDto> getMessages(String threadId, String userId, int limit, Long before) {
        log.debug("Getting messages for thread: {} for user: {}, limit: {}, before: {}", 
                  threadId, userId, limit, before);
        
        Long userIdLong = Long.parseLong(userId);
        
        // Verify thread exists and belongs to user
        if (!chatThreadRepository.existsByIdAndUserId(threadId, userIdLong)) {
            log.warn("Thread not found or unauthorized: {} for user: {}", threadId, userId);
            return null;
        }
        
        List<ChatMessage> messages;
        
        if (before != null) {
            // Convert milliseconds to LocalDateTime
            LocalDateTime beforeTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(before),
                java.time.ZoneId.systemDefault()
            );
            
            PageRequest pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
            messages = chatMessageRepository.findByThreadIdAndTimestampBefore(threadId, beforeTime, pageable)
                .getContent();
        } else {
            // Get latest messages
            messages = chatMessageRepository.findLatestMessagesByThreadId(threadId, limit);
        }
        
        // Convert to DTOs and reverse order (to show oldest first)
        List<MessageDto> messageDtos = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            messageDtos.add(new MessageDto(
                msg.getId().toString(),
                threadId,
                msg.getRole(),
                msg.getContent(),
                msg.getTimestampMillis()
            ));
        }
        
        return messageDtos;
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
