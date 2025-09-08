package com.compass.domain.chat.interceptor;

import com.compass.domain.chat.logger.LLMCallLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * LLM API 호출 인터셉터
 * REQ-MON-001: API 호출 로깅 구현
 * AOP를 통한 자동 로깅 처리
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LLMLoggingInterceptor {
    
    private final LLMCallLogger llmCallLogger;
    
    /**
     * Spring AI ChatModel 호출 포인트컷
     */
    @Pointcut("execution(* org.springframework.ai.chat.model.ChatModel.call(..))")
    public void chatModelCall() {}
    
    /**
     * Gemini/OpenAI Service 호출 포인트컷
     */
    @Pointcut("execution(* com.compass.domain.chat.service.impl.*ChatService.generate*(..))")
    public void chatServiceCall() {}
    
    /**
     * LLM 호출 로깅 처리
     */
    @Around("chatModelCall() || chatServiceCall()")
    public Object logLLMCall(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant startTime = Instant.now();
        String requestId = null;
        String model = extractModelName(joinPoint);
        Long userId = extractUserId(joinPoint);
        
        try {
            // 프롬프트 추출
            String prompt = extractPrompt(joinPoint);
            
            // 호출 시작 로깅
            requestId = llmCallLogger.startCall(userId, model, prompt);
            
            // 실제 메서드 실행
            Object result = joinPoint.proceed();
            
            // 응답 처리 및 로깅
            if (result instanceof ChatResponse) {
                ChatResponse response = (ChatResponse) result;
                logChatResponse(requestId, startTime, model, prompt, response);
            } else if (result instanceof String) {
                logStringResponse(requestId, startTime, model, prompt, (String) result);
            }
            
            return result;
            
        } catch (Exception e) {
            // 실패 로깅
            if (requestId != null) {
                llmCallLogger.logFailure(requestId, startTime, 
                        e.getMessage(), e.getClass().getSimpleName());
            }
            throw e;
        }
    }
    
    /**
     * ChatResponse 로깅
     */
    private void logChatResponse(String requestId, Instant startTime, 
                                 String model, String prompt, 
                                 ChatResponse response) {
        try {
            // 토큰 수 계산 (메타데이터가 있는 경우)
            int inputTokens = llmCallLogger.estimateTokens(prompt);
            int outputTokens = llmCallLogger.estimateTokens(
                    response.getResult().getOutput().getContent());
            
            // 비용 계산
            double cost = llmCallLogger.calculateCost(model, inputTokens, outputTokens);
            
            // 성공 로깅
            llmCallLogger.logSuccess(requestId, startTime, 
                    inputTokens, outputTokens, cost);
                    
        } catch (Exception e) {
            log.error("Error logging ChatResponse", e);
        }
    }
    
    /**
     * String 응답 로깅
     */
    private void logStringResponse(String requestId, Instant startTime,
                                  String model, String prompt, 
                                  String response) {
        try {
            // 토큰 수 추정
            int inputTokens = llmCallLogger.estimateTokens(prompt);
            int outputTokens = llmCallLogger.estimateTokens(response);
            
            // 비용 계산
            double cost = llmCallLogger.calculateCost(model, inputTokens, outputTokens);
            
            // 성공 로깅
            llmCallLogger.logSuccess(requestId, startTime, 
                    inputTokens, outputTokens, cost);
                    
        } catch (Exception e) {
            log.error("Error logging String response", e);
        }
    }
    
    /**
     * 모델명 추출
     */
    private String extractModelName(ProceedingJoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        if (className.contains("Gemini")) {
            return "gemini-2.0-flash";
        } else if (className.contains("OpenAi") || className.contains("GPT")) {
            return "gpt-4o-mini";
        }
        
        return "unknown";
    }
    
    /**
     * 사용자 ID 추출 (ThreadLocal 또는 SecurityContext에서)
     */
    private Long extractUserId(ProceedingJoinPoint joinPoint) {
        // TODO: SecurityContext 또는 ThreadLocal에서 사용자 ID 추출
        // 현재는 null 반환 (anonymous로 로깅됨)
        return null;
    }
    
    /**
     * 프롬프트 추출
     */
    private String extractPrompt(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        
        for (Object arg : args) {
            if (arg instanceof Prompt) {
                return ((Prompt) arg).getContents();
            } else if (arg instanceof String) {
                return (String) arg;
            }
        }
        
        return "";
    }
}