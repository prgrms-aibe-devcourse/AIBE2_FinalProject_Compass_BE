package com.compass.domain.chat.service.enrichment;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * API 호출 속도 제한 관리자
 */
@Component
@Slf4j
public class ApiRateLimiter {

    // API별 Rate Limiter
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    // API별 QPS (Queries Per Second) 설정
    private static final Map<String, Double> API_LIMITS = Map.of(
        "google_places", 10.0,     // 10 QPS
        "kakao_map", 30.0,         // 30 QPS
        "tour_api", 5.0,           // 5 QPS
        "perplexity", 0.05,        // 3/min = 0.05 QPS
        "openai", 0.833            // 50/min = 0.833 QPS
    );

    // API별 백오프 전략
    private final Map<String, BackoffStrategy> backoffStrategies = new ConcurrentHashMap<>();

    /**
     * API 호출 전 허가 요청
     */
    public boolean tryAcquire(String apiName) {
        RateLimiter limiter = getLimiter(apiName);
        return limiter.tryAcquire();
    }

    /**
     * API 호출 전 허가 요청 (타임아웃 포함)
     */
    public boolean tryAcquire(String apiName, long timeout, TimeUnit unit) {
        RateLimiter limiter = getLimiter(apiName);
        return limiter.tryAcquire(timeout, unit);
    }

    /**
     * API 호출 전 허가 요청 (블로킹)
     */
    public void acquire(String apiName) {
        RateLimiter limiter = getLimiter(apiName);
        double waitTime = limiter.acquire();
        if (waitTime > 0) {
            log.debug("{} API 대기 시간: {}초", apiName, waitTime);
        }
    }

    /**
     * 여러 허가 동시 요청
     */
    public void acquire(String apiName, int permits) {
        RateLimiter limiter = getLimiter(apiName);
        double waitTime = limiter.acquire(permits);
        if (waitTime > 0) {
            log.debug("{} API {} 개 허가 대기 시간: {}초", apiName, permits, waitTime);
        }
    }

    /**
     * Rate Limiter 가져오기 (없으면 생성)
     */
    private RateLimiter getLimiter(String apiName) {
        return limiters.computeIfAbsent(apiName, name -> {
            Double qps = API_LIMITS.getOrDefault(name, 1.0);
            log.info("{} API Rate Limiter 생성: {} QPS", name, qps);
            return RateLimiter.create(qps);
        });
    }

    /**
     * 특정 API의 QPS 동적 변경
     */
    public void updateRate(String apiName, double qps) {
        RateLimiter limiter = getLimiter(apiName);
        limiter.setRate(qps);
        log.info("{} API Rate 변경: {} QPS", apiName, qps);
    }

    /**
     * 백오프 전략 처리
     */
    public void handleBackoff(String apiName, Exception error) {
        BackoffStrategy strategy = backoffStrategies.computeIfAbsent(apiName,
            name -> new BackoffStrategy());

        if (isRateLimitError(error)) {
            strategy.recordFailure();
            long backoffMs = strategy.getNextBackoffMillis();

            log.warn("{} API 속도 제한 발생, {} ms 대기", apiName, backoffMs);

            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Rate limiter 속도 감소
            double currentRate = getLimiter(apiName).getRate();
            updateRate(apiName, Math.max(0.1, currentRate * 0.5));

        } else {
            strategy.recordSuccess();

            // Rate limiter 속도 복구
            Double originalRate = API_LIMITS.get(apiName);
            if (originalRate != null) {
                double currentRate = getLimiter(apiName).getRate();
                if (currentRate < originalRate) {
                    updateRate(apiName, Math.min(originalRate, currentRate * 1.1));
                }
            }
        }
    }

    /**
     * Rate Limit 에러 판별
     */
    private boolean isRateLimitError(Exception error) {
        if (error == null || error.getMessage() == null) return false;

        String message = error.getMessage().toLowerCase();
        return message.contains("rate limit") ||
               message.contains("too many requests") ||
               message.contains("429") ||
               message.contains("quota exceeded");
    }

    /**
     * 백오프 전략 클래스
     */
    private static class BackoffStrategy {
        private int consecutiveFailures = 0;
        private long lastFailureTime = 0;
        private static final long RESET_THRESHOLD_MS = 60000; // 1분

        void recordFailure() {
            consecutiveFailures++;
            lastFailureTime = System.currentTimeMillis();
        }

        void recordSuccess() {
            if (System.currentTimeMillis() - lastFailureTime > RESET_THRESHOLD_MS) {
                consecutiveFailures = 0;
            }
        }

        long getNextBackoffMillis() {
            // Exponential backoff with jitter
            long baseDelay = Math.min(1000L * (long) Math.pow(2, consecutiveFailures), 60000L);
            long jitter = (long) (Math.random() * 1000);
            return baseDelay + jitter;
        }
    }

    /**
     * 통계 정보 조회
     */
    public Map<String, Object> getStatistics(String apiName) {
        RateLimiter limiter = limiters.get(apiName);
        if (limiter == null) {
            return Map.of("status", "not_initialized");
        }

        return Map.of(
            "api", apiName,
            "currentRate", limiter.getRate(),
            "originalRate", API_LIMITS.getOrDefault(apiName, 1.0),
            "backoffActive", backoffStrategies.containsKey(apiName)
        );
    }

    /**
     * 모든 API 통계
     */
    public Map<String, Map<String, Object>> getAllStatistics() {
        Map<String, Map<String, Object>> stats = new ConcurrentHashMap<>();
        for (String apiName : limiters.keySet()) {
            stats.put(apiName, getStatistics(apiName));
        }
        return stats;
    }

    /**
     * Rate Limiter 리셋
     */
    public void reset(String apiName) {
        Double originalRate = API_LIMITS.get(apiName);
        if (originalRate != null) {
            updateRate(apiName, originalRate);
            backoffStrategies.remove(apiName);
            log.info("{} API Rate Limiter 리셋", apiName);
        }
    }

    /**
     * 모든 Rate Limiter 리셋
     */
    public void resetAll() {
        for (String apiName : limiters.keySet()) {
            reset(apiName);
        }
    }
}