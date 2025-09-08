package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.ChatDtos;
import com.compass.domain.chat.dto.PromptRequest;
import com.compass.domain.chat.dto.PromptResponse;
import com.compass.domain.chat.service.ChatService;
import com.compass.domain.chat.service.ChatModelService;
import com.compass.domain.chat.service.PromptTemplateService;
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
import java.util.List;
import java.util.Map;

/**
 * Chat Controller
 * Handles chat-related API endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Chat API for AI model interactions")
public class ChatController {
    
    private final ChatService chatService;
    private final ChatModelService geminiChatService;
    private final ChatModelService openAiChatService;
    private final PromptTemplateService promptTemplateService;
    
    public ChatController(
            ChatService chatService,
            @Qualifier("geminiChatService") ChatModelService geminiChatService,
            @Qualifier("openAIChatService") ChatModelService openAiChatService,
            PromptTemplateService promptTemplateService) {
        this.chatService = chatService;
        this.geminiChatService = geminiChatService;
        this.openAiChatService = openAiChatService;
        this.promptTemplateService = promptTemplateService;
    }

    // ============= CHAT1 팀 기능 (기본 CRUD) =============
    
    /**
     * REQ-CHAT-001: 채팅방 생성 API
     */
    @PostMapping("/threads")
    public ResponseEntity<ChatDtos.ThreadDto> createChatThread(
            @RequestHeader("X-User-ID") String userId) {
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

    // ============= CHAT2 팀 기능 (LLM 통합) =============
    
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

    @Operation(summary = "Chat with template-based prompt", description = "Process user message with intelligent template selection and personalization")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully processed message"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/travel")
    public ResponseEntity<Map<String, Object>> chatWithTemplate(
            @Parameter(description = "User message and context", required = true)
            @RequestBody Map<String, Object> request) {
        
        String message = (String) request.get("message");
        Long userId = request.get("userId") != null ? 
            Long.parseLong(request.get("userId").toString()) : null;
        
        if (message == null || message.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Message is required");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            // 1. 템플릿 자동 선택
            String templateName = promptTemplateService.selectTemplate(message, request);
            log.info("Selected template: {} for message: {}", templateName, message);
            
            // 2. 파라미터 추출
            Map<String, Object> parameters = promptTemplateService.extractParameters(
                templateName, message, request);
            
            // 3. 프롬프트 요청 생성
            PromptRequest promptRequest = new PromptRequest();
            promptRequest.setTemplateName(templateName);
            promptRequest.setParameters(parameters);
            promptRequest.setUserId(userId != null ? userId.toString() : null);
            
            // 4. 강화된 프롬프트 생성
            PromptResponse promptResponse = promptTemplateService.buildEnrichedPrompt(promptRequest);
            
            // 5. LLM에 전달하여 응답 생성
            String aiResponse = geminiChatService.generateResponse(promptResponse.getPrompt());
            
            // 6. 응답 구성
            Map<String, Object> result = new HashMap<>();
            result.put("model", geminiChatService.getModelName());
            result.put("message", message);
            result.put("response", aiResponse);
            result.put("template", templateName);
            result.put("detectedParameters", parameters);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error in template-based chat: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to process message");
            error.put("details", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @Operation(summary = "Get available templates", description = "Retrieve list of available prompt templates")
    @GetMapping("/templates")
    public ResponseEntity<Map<String, Object>> getTemplates() {
        Map<String, Object> response = new HashMap<>();
        response.put("templates", promptTemplateService.getAvailableTemplates());
        response.put("count", promptTemplateService.getAvailableTemplates().size());
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Get template details", description = "Retrieve detailed information about a specific template")
    @GetMapping("/templates/{templateName}")
    public ResponseEntity<Map<String, Object>> getTemplateDetails(
            @PathVariable String templateName) {
        
        if (!promptTemplateService.getAvailableTemplates().contains(templateName)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Template not found: " + templateName);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(promptTemplateService.getTemplateDetails(templateName));
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
}
