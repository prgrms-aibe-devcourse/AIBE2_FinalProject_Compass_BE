package com.compass.domain.chat.controller;

import com.compass.config.jwt.JwtTokenProvider;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.model.response.ChatResponse;
import com.compass.domain.chat.orchestrator.ContextManager;
import com.compass.domain.chat.orchestrator.MainLLMOrchestrator;
import com.compass.domain.chat.stage_integration.service.StageIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat/follow-up")
@RequiredArgsConstructor
public class FollowUpController {

    private final MainLLMOrchestrator mainLLMOrchestrator;
    private final ContextManager contextManager;
    private final StageIntegrationService stageIntegrationService;
    private final JwtTokenProvider jwtTokenProvider;

    record StartRequest(String message, Long userId, String dedupeKey) {}
    record RespondRequest(String sessionId, String response, Long userId) {}

    @PostMapping("/start")
    @Transactional
    public ResponseEntity<Map<String, Object>> startFollowUp(@RequestBody StartRequest request,
                                                             HttpServletRequest httpRequest,
                                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (!StringUtils.hasText(request.message())) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = resolveUserId(request.userId(), authHeader, httpRequest);
        String userIdValue = userId != null ? userId.toString() : "anonymous";

        String threadId = UUID.randomUUID().toString();
        ChatResponse chatResponse = mainLLMOrchestrator.processChat(new ChatRequest(request.message(), threadId, userIdValue));

        Map<String, Object> payload = buildFollowUpPayload(threadId, chatResponse, false);
        payload.put("createdAt", LocalDateTime.now());
        payload.put("dedupeKey", request.dedupeKey());

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/respond")
    @Transactional
    public ResponseEntity<Map<String, Object>> respond(@RequestBody RespondRequest request,
                                                       HttpServletRequest httpRequest,
                                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!StringUtils.hasText(request.sessionId()) || !StringUtils.hasText(request.response())) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = resolveUserId(request.userId(), authHeader, httpRequest);
        String userIdValue = userId != null ? userId.toString() : "anonymous";

        ChatResponse chatResponse = mainLLMOrchestrator.processChat(new ChatRequest(request.response(), request.sessionId(), userIdValue));
        Map<String, Object> payload = buildFollowUpPayload(request.sessionId(), chatResponse, true);
        payload.put("message", chatResponse.getContent());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/status/{sessionId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> status(@PathVariable String sessionId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("threadId", sessionId);
        payload.put("isComplete", false);

        contextManager.getContext(sessionId).ifPresent(context -> {
            payload.put("currentPhase", context.getCurrentPhase());
            payload.put("collectedInfo", context.getCollectedInfo());
        });

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/generate-plan")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> generatePlan(@RequestBody Map<String, String> requestBody) {
        String sessionId = requestBody.get("sessionId");
        if (!StringUtils.hasText(sessionId)) {
            return ResponseEntity.badRequest().build();
        }

        TravelContext context = contextManager.getContext(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("여행 컨텍스트를 찾을 수 없습니다."));

        Map<String, Object> plan = stageIntegrationService.processStage3(context);
        return ResponseEntity.ok(plan);
    }

    private Map<String, Object> buildFollowUpPayload(String sessionId, ChatResponse response, boolean isResponse) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("threadId", sessionId);
        payload.put("question", response.getContent() != null ? response.getContent() : "");
        payload.put("nextAction", response.getNextAction());
        payload.put("data", response.getData());
        payload.put("phase", response.getPhase());

        boolean shouldGeneratePlan = "TRIGGER_PLAN_GENERATION".equals(response.getNextAction())
            || "REVIEW_AND_CONFIRM".equals(response.getNextAction())
            || "FINAL_ITINERARY_CREATED".equals(response.getType());

        boolean isComplete = shouldGeneratePlan
            || "STAGE1_PLACES_LOADED".equals(response.getType());

        payload.put("questionType", isComplete ? "complete" : "follow_up");

        payload.put("isComplete", isComplete);
        payload.put("canGeneratePlan", shouldGeneratePlan);
        payload.put("progressPercentage", isComplete ? 100 : 0);
        payload.put("remainingQuestions", isComplete ? 0 : 1);
        payload.put("timestamp", LocalDateTime.now());

        return payload;
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
                    // ignore
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
