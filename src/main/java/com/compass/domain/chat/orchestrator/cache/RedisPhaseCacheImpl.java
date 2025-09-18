package com.compass.domain.chat.orchestrator.cache;

import com.compass.domain.chat.model.enums.TravelPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

// Redis 기반 Phase 캐시 구현체
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPhaseCacheImpl implements PhaseCache {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${phase.cache.key-prefix:phase:thread:}")
    private String keyPrefix;

    @Value("${phase.cache.ttl-hours:24}")
    private long ttlHours;

    @Override
    public Optional<TravelPhase> get(String threadId) {
        String key = keyPrefix + threadId;
        String phaseName = redisTemplate.opsForValue().get(key);

        if (phaseName != null) {
            log.debug("Redis 캐시 적중: threadId={}, phase={}", threadId, phaseName);
            return Optional.of(TravelPhase.valueOf(phaseName));
        }

        log.debug("Redis 캐시 미스: threadId={}", threadId);
        return Optional.empty();
    }

    @Override
    public void put(String threadId, TravelPhase phase) {
        String key = keyPrefix + threadId;
        redisTemplate.opsForValue().set(key, phase.name(), Duration.ofHours(ttlHours));
        log.debug("Redis 캐시 저장: threadId={}, phase={}", threadId, phase);
    }

    @Override
    public void evict(String threadId) {
        String key = keyPrefix + threadId;
        redisTemplate.delete(key);
        log.debug("Redis 캐시 삭제: threadId={}", threadId);
    }
}