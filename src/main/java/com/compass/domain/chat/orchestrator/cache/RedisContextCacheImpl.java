package com.compass.domain.chat.orchestrator.cache;

import com.compass.domain.chat.model.context.TravelContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

// Redis 기반 Context 캐시 구현체
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisContextCacheImpl implements ContextCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${context.cache.key-prefix:context:thread:}")
    private String keyPrefix;

    @Value("${context.cache.ttl-hours:24}")
    private long ttlHours;

    @Override
    public Optional<TravelContext> get(String threadId) {
        String key = keyPrefix + threadId;
        String contextJson = redisTemplate.opsForValue().get(key);

        if (contextJson != null) {
            try {
                TravelContext context = objectMapper.readValue(contextJson, TravelContext.class);
                log.debug("Redis 캐시 적중: threadId={}, userId={}, phase={}, waitingForConfirmation={}",
                    threadId, context.getUserId(), context.getCurrentPhase(), context.isWaitingForTravelConfirmation());
                return Optional.of(context);
            } catch (Exception e) {
                log.error("컨텍스트 역직렬화 실패: threadId={}, error={}", threadId, e.getMessage());
                return Optional.empty();
            }
        }

        log.debug("Redis 캐시 미스: threadId={}", threadId);
        return Optional.empty();
    }

    @Override
    public void put(String threadId, TravelContext context) {
        String key = keyPrefix + threadId;
        try {
            String contextJson = objectMapper.writeValueAsString(context);
            redisTemplate.opsForValue().set(key, contextJson, Duration.ofHours(ttlHours));
            log.debug("Redis 캐시 저장: threadId={}, userId={}, phase={}, waitingForConfirmation={}",
                threadId, context.getUserId(), context.getCurrentPhase(), context.isWaitingForTravelConfirmation());
        } catch (Exception e) {
            log.error("컨텍스트 직렬화 실패: threadId={}, error={}", threadId, e.getMessage());
        }
    }

    @Override
    public void evict(String threadId) {
        String key = keyPrefix + threadId;
        redisTemplate.delete(key);
        log.debug("Redis 캐시 삭제: threadId={}", threadId);
    }
}