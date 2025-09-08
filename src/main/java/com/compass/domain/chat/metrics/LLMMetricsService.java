package com.compass.domain.chat.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLM API 메트릭 수집 서비스
 * REQ-MON-001: API 호출 로깅 및 메트릭 수집
 */
@Slf4j
@Service
public class LLMMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicLong> dailyCosts = new ConcurrentHashMap<>();
    
    // 메트릭 카운터
    private final Counter totalCallsCounter;
    private final Counter successCallsCounter;
    private final Counter failureCallsCounter;
    private final Counter retryCounter;
    
    // 타이머
    private final Timer apiCallTimer;
    
    public LLMMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 카운터 초기화
        this.totalCallsCounter = Counter.builder("llm.api.calls.total")
                .description("Total number of LLM API calls")
                .register(meterRegistry);
                
        this.successCallsCounter = Counter.builder("llm.api.calls.success")
                .description("Number of successful LLM API calls")
                .register(meterRegistry);
                
        this.failureCallsCounter = Counter.builder("llm.api.calls.failure")
                .description("Number of failed LLM API calls")
                .register(meterRegistry);
                
        this.retryCounter = Counter.builder("llm.api.retries")
                .description("Number of LLM API call retries")
                .register(meterRegistry);
        
        // 타이머 초기화
        this.apiCallTimer = Timer.builder("llm.api.duration")
                .description("LLM API call duration")
                .register(meterRegistry);
    }
    
    /**
     * API 호출 시작 기록
     */
    public void recordCallStart(String model) {
        totalCallsCounter.increment();
        
        // 모델별 카운터
        Counter.builder("llm.api.calls.by.model")
                .tag("model", model)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * API 호출 성공 기록
     */
    public void recordCallSuccess(String model, Duration duration, 
                                  int tokens, double cost) {
        successCallsCounter.increment();
        apiCallTimer.record(duration);
        
        // 모델별 성공 카운터
        Counter.builder("llm.api.calls.success.by.model")
                .tag("model", model)
                .register(meterRegistry)
                .increment();
        
        // 토큰 사용량 기록
        meterRegistry.gauge("llm.tokens.used", tokens);
        
        // 비용 기록
        meterRegistry.gauge("llm.cost.per.call", cost);
        
        // 일일 비용 누적
        updateDailyCost(model, cost);
    }
    
    /**
     * API 호출 실패 기록
     */
    public void recordCallFailure(String model, String errorType) {
        failureCallsCounter.increment();
        
        // 모델별, 에러 타입별 실패 카운터
        Counter.builder("llm.api.calls.failure.by.type")
                .tag("model", model)
                .tag("error", errorType)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 재시도 기록
     */
    public void recordRetry(String model, int attemptNumber) {
        retryCounter.increment();
        
        // 모델별 재시도 카운터
        Counter.builder("llm.api.retries.by.model")
                .tag("model", model)
                .tag("attempt", String.valueOf(attemptNumber))
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 일일 비용 업데이트
     */
    private void updateDailyCost(String model, double cost) {
        String key = model + "_" + java.time.LocalDate.now();
        dailyCosts.computeIfAbsent(key, k -> new AtomicLong(0))
                .addAndGet((long)(cost * 1000000)); // 마이크로센트로 저장
    }
    
    /**
     * 일일 비용 조회
     */
    public double getDailyCost(String model) {
        String key = model + "_" + java.time.LocalDate.now();
        AtomicLong costInMicroCents = dailyCosts.get(key);
        if (costInMicroCents == null) {
            return 0.0;
        }
        return costInMicroCents.get() / 1000000.0;
    }
    
    /**
     * 성공률 계산
     */
    public double getSuccessRate() {
        double total = totalCallsCounter.count();
        if (total == 0) {
            return 0.0;
        }
        return (successCallsCounter.count() / total) * 100;
    }
    
    /**
     * 평균 응답 시간
     */
    public double getAverageResponseTime() {
        return apiCallTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * 메트릭 요약 생성
     */
    public MetricsSummary getSummary() {
        return MetricsSummary.builder()
                .totalCalls((long) totalCallsCounter.count())
                .successCalls((long) successCallsCounter.count())
                .failureCalls((long) failureCallsCounter.count())
                .retries((long) retryCounter.count())
                .successRate(getSuccessRate())
                .averageResponseTime(getAverageResponseTime())
                .dailyCostGemini(getDailyCost("gemini-2.0-flash"))
                .dailyCostGPT(getDailyCost("gpt-4o-mini"))
                .build();
    }
    
    /**
     * 메트릭 요약 DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class MetricsSummary {
        private long totalCalls;
        private long successCalls;
        private long failureCalls;
        private long retries;
        private double successRate;
        private double averageResponseTime;
        private double dailyCostGemini;
        private double dailyCostGPT;
    }
}