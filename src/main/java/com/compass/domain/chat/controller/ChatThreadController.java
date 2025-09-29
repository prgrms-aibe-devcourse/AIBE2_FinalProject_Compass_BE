package com.compass.domain.chat.controller;

import com.compass.config.jwt.JwtTokenProvider;
import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.orchestrator.MainLLMOrchestrator;
import com.compass.domain.chat.repository.ChatMessageRepository;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.service.ChatThreadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chat/threads")
@RequiredArgsConstructor
public class ChatThreadController {

    private final ChatThreadService chatThreadService;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MainLLMOrchestrator mainLLMOrchestrator;
    private final JwtTokenProvider jwtTokenProvider;

    record CreateThreadRequest(Long userId, String initialMessage, String title) {}

    record ThreadDto(String id,
                     Long userId,
                     String title,
                     String lastMessage,
                     LocalDateTime lastMessageAt,
                     LocalDateTime createdAt,
                     LocalDateTime updatedAt) {}

    record MessageRequest(String content, String role, Map<String, Object> metadata) {}

    record MessageDto(Long id,
                      String threadId,
                      String role,
                      String content,
                      LocalDateTime timestamp,
                      String type,
                      String nextAction,
                      Object data,
                      String phase,
                      String model,
                      Map<String, Object> followUpQuestion) {}

    @PostMapping
    public ResponseEntity<ThreadDto> createThread(@RequestBody CreateThreadRequest request,
                                                  HttpServletRequest httpRequest,
                                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = resolveUserId(request.userId(), authHeader, httpRequest);
        ChatThread thread = chatThreadService.createThread(userId, request.title());

        if (StringUtils.hasText(request.initialMessage())) {
            chatThreadService.saveMessage(new ChatThreadService.MessageSaveRequest(
                thread.getId(), "user", request.initialMessage().trim()
            ));
        }

        ThreadDto dto = toThreadDto(thread, request.initialMessage());
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<ThreadDto>> getThreads(@RequestParam(required = false) Long userId,
                                                      @RequestParam(defaultValue = "0") int skip,
                                                      @RequestParam(defaultValue = "20") int limit,
                                                      HttpServletRequest httpRequest,
                                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long resolvedUserId = resolveUserId(userId, authHeader, httpRequest);

        int pageIndex = skip <= 0 ? 0 : skip / Math.max(limit, 1);
        Pageable pageable = PageRequest.of(pageIndex, limit, Sort.by(Sort.Direction.DESC, "lastMessageAt", "createdAt"));
        Page<ChatThread> page = chatThreadRepository.findByUserId(resolvedUserId, pageable);

        List<ThreadDto> items = page.getContent().stream()
            .map(thread -> toThreadDto(thread, null))
            .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    @GetMapping("/{threadId}/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable String threadId,
                                                        @RequestParam(defaultValue = "50") int limit) {
        List<ChatMessage> messages = chatMessageRepository.findByThreadIdOrderByTimestampDesc(threadId);
        List<MessageDto> payload = messages.stream()
            .sorted(Comparator.comparing(ChatMessage::getTimestamp))
            .limit(limit)
            .map(this::toMessageDto)
            .collect(Collectors.toList());

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/{threadId}/messages")
    @Transactional
    public ResponseEntity<List<MessageDto>> sendMessage(@PathVariable String threadId,
                                                        @RequestBody MessageRequest request,
                                                        HttpServletRequest httpRequest,
                                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StringUtils.hasText(request.content())) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = resolveUserId(null, authHeader, httpRequest);
        String userIdValue = userId != null ? userId.toString() : "anonymous";

        ChatRequest chatRequest = new ChatRequest(request.content(), threadId, userIdValue, request.metadata());
        ChatResponse chatResponse = mainLLMOrchestrator.processChat(chatRequest);

        // Fetch the latest two messages (user + assistant) to return to frontend
        List<ChatMessage> ordered = chatMessageRepository.findByThreadIdOrderByTimestampDesc(threadId)
            .stream()
            .sorted(Comparator.comparing(ChatMessage::getTimestamp))
            .collect(Collectors.toList());

        int size = ordered.size();
        List<ChatMessage> recentMessages = size <= 2
            ? ordered
            : ordered.subList(size - 2, size);

        List<MessageDto> payload = recentMessages.stream()
            .map(this::toMessageDto)
            .collect(Collectors.toList());

        if (!payload.isEmpty()) {
            MessageDto lastMessage = payload.get(payload.size() - 1);
            payload.set(payload.size() - 1, mergeAssistantMetadata(lastMessage, chatResponse));
        }

        return ResponseEntity.ok(payload);
    }

    private MessageDto mergeAssistantMetadata(MessageDto original, ChatResponse response) {
        Map<String, Object> followUp = null;
        if ("AWAIT_USER_INPUT".equals(response.getNextAction())) {
            followUp = new HashMap<>();
            followUp.put("sessionId", original.threadId());
            followUp.put("primaryQuestion", response.getContent());
            followUp.put("inputType", "text");
            followUp.put("currentStep", response.getPhase());
            followUp.put("progressPercentage", 0);
            followUp.put("remainingQuestions", 0);
        }

        return new MessageDto(
            original.id(),
            original.threadId(),
            original.role(),
            original.content(),
            original.timestamp(),
            response.getType(),
            response.getNextAction(),
            response.getData(),
            response.getPhase(),
            null,
            followUp
        );
    }

    private ThreadDto toThreadDto(ChatThread thread, String fallbackMessage) {
        String lastMessage = fallbackMessage;
        LocalDateTime lastMessageAt = thread.getLastMessageAt();

        if (lastMessage == null) {
            List<ChatMessage> messages = chatMessageRepository.findByThreadIdOrderByTimestampDesc(thread.getId());
            if (!messages.isEmpty()) {
                ChatMessage latest = messages.get(0);
                lastMessage = latest.getContent();
                lastMessageAt = latest.getTimestamp();
            }
        }

        return new ThreadDto(
            thread.getId(),
            thread.getUser().getId(),
            thread.getTitle(),
            lastMessage,
            lastMessageAt,
            thread.getCreatedAt(),
            thread.getUpdatedAt()
        );
    }

    private MessageDto toMessageDto(ChatMessage message) {
        return new MessageDto(
            message.getId(),
            message.getThread().getId(),
            message.getRole(),
            message.getContent(),
            message.getTimestamp(),
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private Long resolveUserId(Long userId,
                               String authHeader,
                               HttpServletRequest request) {
        if (userId != null) {
            return userId;
        }

        String token = resolveToken(authHeader, request);
        if (token != null) {
            String extracted = jwtTokenProvider.getUserId(token);
            if (extracted != null) {
                try {
                    return Long.parseLong(extracted);
                } catch (NumberFormatException ignored) {
                    // userId가 숫자가 아닌 경우 (이메일 형태 등) 지원 필요 시 확장
                }
            }
        }

        return null;
    }

    private String resolveToken(String authHeader, HttpServletRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return jwtTokenProvider.resolveToken(request);
    }
}
