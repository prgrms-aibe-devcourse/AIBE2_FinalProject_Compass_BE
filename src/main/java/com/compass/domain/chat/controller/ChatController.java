package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.ChatDtos;
import com.compass.domain.chat.service.ChatModelService;
import com.compass.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Chat Controller
 * Handles chat-related API endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Chat API for AI model interactions")
public class ChatController {
    
    private final ChatModelService geminiChatService;
    private final ChatModelService openAiChatService;
    
    public ChatController(ChatModelService geminiChatService,
                          @Qualifier("openAIChatService") ChatModelService openAiChatService, ChatService chatService) {
        this.geminiChatService = geminiChatService;
        this.openAiChatService = openAiChatService;
        this.chatService = chatService;
    }
    
    @Operation(summary = "Send message to Gemini", description = "Process user message with Gemini 2.5 Flash model")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully processed message"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/gemini")
    public ResponseEntity<Map<String, Object>> chatWithGemini(
            @Parameter(description = "User message to process", required = true)
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Message is required");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            String response = geminiChatService.generateResponse(message);
            
            Map<String, Object> result = new HashMap<>();
            result.put("model", geminiChatService.getModelName());
            result.put("message", message);
            result.put("response", response);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in Gemini chat: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to process message");
            error.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @Operation(summary = "Send message to OpenAI", description = "Process user message with GPT-4o-mini model (fallback)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully processed message"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/openai")
    public ResponseEntity<Map<String, Object>> chatWithOpenAI(
            @Parameter(description = "User message to process", required = true)
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Message is required");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            String response = openAiChatService.generateResponse(message);
            
            Map<String, Object> result = new HashMap<>();
            result.put("model", openAiChatService.getModelName());
            result.put("message", message);
            result.put("response", response);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in OpenAI chat: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to process message");
            error.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @Operation(summary = "Test chat service", description = "Simple test endpoint to verify chat service is working")
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testChat() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Chat service is running");
        response.put("gemini", "Ready");
        response.put("openai", "Ready (fallback)");
        return ResponseEntity.ok(response);
    }
    private final ChatService chatService;

    /**
     * REQ-CHAT-001: 채팅방 생성 API
     */
    @PostMapping("/threads")
    public ResponseEntity<ChatDtos.ThreadDto> createChatThread(@RequestHeader("X-User-ID") String userId) {
        ChatDtos.ThreadDto newThread = chatService.createThread(userId);
        return new ResponseEntity<>(newThread, HttpStatus.CREATED);
    }

    /**
     * REQ-CHAT-002: 채팅 목록 조회 API
     */
    @GetMapping("/threads")
    public ResponseEntity<List<ChatDtos.ThreadDto>> getChatThreads(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "20") int limit) {
        List<ChatDtos.ThreadDto> threads = chatService.getUserThreads(userId, skip, limit);
        return ResponseEntity.ok(threads);
    }

    /**
     * REQ-CHAT-003: 메시지 전송 API
     */
    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<List<ChatDtos.MessageDto>> sendMessage(
            @PathVariable String threadId,
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody ChatDtos.MessageCreateDto messageDto) {
        List<ChatDtos.MessageDto> messages = chatService.addMessageToThread(threadId, userId, messageDto);
        if (messages == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(messages);
    }

    /**
     * REQ-CHAT-004: 대화 조회 API
     */
    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<List<ChatDtos.MessageDto>> getMessagesInThread(
            @PathVariable String threadId,
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) Long before) {
        List<ChatDtos.MessageDto> messages = chatService.getMessages(threadId, userId, limit, before);
        if (messages == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(messages);
    }
}