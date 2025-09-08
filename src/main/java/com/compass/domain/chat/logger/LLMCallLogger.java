package com.compass.domain.chat.logger;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * LLM API 호출 로깅 서비스
 * REQ-MON-001: API 호출 로깅 구현
 */
@Slf4j
@Component
public class LLMCallLogger {
    
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String MODEL_KEY = "model";
    private static final String TOKENS_KEY = "tokens";
    private static final String COST_KEY = "cost";
    private static final String DURATION_KEY = "duration";
    private static final String STATUS_KEY = "status";
    
    /**
     * LLM API 호출 시작 로깅
     */
    public String startCall(Long userId, String model, String prompt) {
        String requestId = UUID.randomUUID().toString();
        
        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(USER_ID_KEY, userId != null ? userId.toString() : "anonymous");
        MDC.put(MODEL_KEY, model);
        
        log.info("LLM API call started | prompt_length: {}", 
                prompt != null ? prompt.length() : 0);
        
        return requestId;
    }
    
    /**
     * LLM API 호출 성공 로깅
     */
    public void logSuccess(String requestId, Instant startTime, 
                          int inputTokens, int outputTokens, 
                          double cost) {
        MDC.put(REQUEST_ID_KEY, requestId);
        
        long duration = Duration.between(startTime, Instant.now()).toMillis();
        int totalTokens = inputTokens + outputTokens;
        
        MDC.put(TOKENS_KEY, String.valueOf(totalTokens));
        MDC.put(COST_KEY, String.format("%.6f", cost));
        MDC.put(DURATION_KEY, String.valueOf(duration));
        MDC.put(STATUS_KEY, "SUCCESS");
        
        log.info("LLM API call completed | input_tokens: {} | output_tokens: {} | total_tokens: {} | cost: ${} | duration_ms: {}",
                inputTokens, outputTokens, totalTokens, cost, duration);
        
        // 메트릭 로깅
        logMetrics(requestId, "SUCCESS", duration, totalTokens, cost);
        
        clearMDC();
    }
    
    /**
     * LLM API 호출 실패 로깅
     */
    public void logFailure(String requestId, Instant startTime, 
                          String errorMessage, String errorType) {
        MDC.put(REQUEST_ID_KEY, requestId);
        
        long duration = Duration.between(startTime, Instant.now()).toMillis();
        
        MDC.put(DURATION_KEY, String.valueOf(duration));
        MDC.put(STATUS_KEY, "FAILURE");
        
        log.error("LLM API call failed | error_type: {} | error_message: {} | duration_ms: {}",
                errorType, errorMessage, duration);
        
        // 메트릭 로깅
        logMetrics(requestId, "FAILURE", duration, 0, 0.0);
        
        clearMDC();
    }
    
    /**
     * 재시도 로깅
     */
    public void logRetry(String requestId, int attemptNumber, String reason) {
        MDC.put(REQUEST_ID_KEY, requestId);
        
        log.warn("LLM API call retry | attempt: {} | reason: {}", 
                attemptNumber, reason);
    }
    
    /**
     * 토큰 제한 경고 로깅
     */
    public void logTokenLimitWarning(String requestId, int currentTokens, int maxTokens) {
        MDC.put(REQUEST_ID_KEY, requestId);
        
        double usage = (currentTokens * 100.0) / maxTokens;
        log.warn("Token limit warning | current_tokens: {} | max_tokens: {} | usage: {:.1f}%",
                currentTokens, maxTokens, usage);
    }
    
    /**
     * 비용 임계값 경고 로깅
     */
    public void logCostThresholdWarning(Long userId, double dailyCost, double threshold) {
        MDC.put(USER_ID_KEY, userId.toString());
        
        log.warn("Cost threshold warning | user_id: {} | daily_cost: ${} | threshold: ${}",
                userId, dailyCost, threshold);
    }
    
    /**
     * 메트릭 로깅 (별도 로거로 출력)
     */
    private void logMetrics(String requestId, String status, 
                           long duration, int tokens, double cost) {
        // metrics 로거로 출력
        org.slf4j.Logger metricsLogger = org.slf4j.LoggerFactory
                .getLogger("com.compass.domain.chat.metrics");
        
        metricsLogger.info("METRIC | request_id={} | status={} | duration_ms={} | tokens={} | cost={}",
                requestId, status, duration, tokens, cost);
    }
    
    /**
     * MDC 정리
     */
    private void clearMDC() {
        MDC.remove(REQUEST_ID_KEY);
        MDC.remove(USER_ID_KEY);
        MDC.remove(MODEL_KEY);
        MDC.remove(TOKENS_KEY);
        MDC.remove(COST_KEY);
        MDC.remove(DURATION_KEY);
        MDC.remove(STATUS_KEY);
    }
    
    /**
     * 토큰 수 추정 (간단한 추정 로직)
     * 실제로는 tiktoken 라이브러리 사용 권장
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 한국어: 평균 2-3자당 1토큰
        // 영어: 평균 4자당 1토큰
        // 간단한 추정: 3자당 1토큰
        return text.length() / 3;
    }
    
    /**
     * 비용 계산
     */
    public double calculateCost(String model, int inputTokens, int outputTokens) {
        // 모델별 가격 (1K 토큰당 USD)
        double inputPrice = 0.0;
        double outputPrice = 0.0;
        
        switch (model.toLowerCase()) {
            case "gemini-2.0-flash":
                inputPrice = 0.0001875;  // $0.1875 per 1M tokens
                outputPrice = 0.00075;   // $0.75 per 1M tokens
                break;
            case "gpt-4o-mini":
                inputPrice = 0.00015;    // $0.15 per 1M tokens
                outputPrice = 0.0006;     // $0.60 per 1M tokens
                break;
            default:
                inputPrice = 0.001;       // Default pricing
                outputPrice = 0.002;
        }
        
        return (inputTokens * inputPrice / 1000.0) + (outputTokens * outputPrice / 1000.0);
    }
}