package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.constant.TravelConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 여행 정보 수집 상태 캐시 서비스
 * REQ-FOLLOW-004: Redis 저장 | TravelContext 30분 TTL
 * 
 * Redis를 활용한 임시 저장소로 30분 TTL 관리
 * Redis가 비활성화된 경우 자동으로 무시됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class TravelInfoCollectionCacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    
    @Value("${travel.collection.cache.ttl:30}")
    private long cacheTtlMinutes = TravelConstants.CACHE_TTL_MINUTES;
    
    /**
     * 여행 정보 수집 상태를 Redis에 저장
     * 30분 TTL 자동 설정
     */
    public void saveTravelContext(String sessionId, TravelInfoCollectionState state) {
        try {
            String key = TravelConstants.CACHE_KEY_PREFIX + sessionId;
            String value = objectMapper.writeValueAsString(state);
            
            redisTemplate.opsForValue().set(
                key, 
                value, 
                cacheTtlMinutes, 
                TimeUnit.MINUTES
            );
            
            log.debug("Travel context cached with sessionId: {}, TTL: {} minutes", 
                     sessionId, cacheTtlMinutes);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize travel context for caching: {}", sessionId, e);
        } catch (Exception e) {
            log.warn("Redis caching failed for sessionId: {}, continuing without cache", sessionId, e);
        }
    }
    
    /**
     * Redis에서 여행 정보 수집 상태 조회
     */
    public Optional<TravelInfoCollectionState> getTravelContext(String sessionId) {
        try {
            String key = TravelConstants.CACHE_KEY_PREFIX + sessionId;
            String value = redisTemplate.opsForValue().get(key);
            
            if (value != null) {
                TravelInfoCollectionState state = objectMapper.readValue(
                    value, 
                    TravelInfoCollectionState.class
                );
                log.debug("Travel context retrieved from cache for sessionId: {}", sessionId);
                return Optional.of(state);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize travel context from cache: {}", sessionId, e);
        } catch (Exception e) {
            log.warn("Redis retrieval failed for sessionId: {}, falling back to DB", sessionId, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Redis에서 여행 정보 수집 상태 삭제
     */
    public void deleteTravelContext(String sessionId) {
        try {
            String key = TravelConstants.CACHE_KEY_PREFIX + sessionId;
            Boolean deleted = redisTemplate.delete(key);
            
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Travel context removed from cache for sessionId: {}", sessionId);
            }
        } catch (Exception e) {
            log.warn("Redis deletion failed for sessionId: {}, continuing", sessionId, e);
        }
    }
    
    /**
     * TTL 갱신 (활성 세션 유지)
     */
    public void refreshTTL(String sessionId) {
        try {
            String key = TravelConstants.CACHE_KEY_PREFIX + sessionId;
            Boolean success = redisTemplate.expire(key, cacheTtlMinutes, TimeUnit.MINUTES);
            
            if (Boolean.TRUE.equals(success)) {
                log.debug("TTL refreshed for sessionId: {}", sessionId);
            }
        } catch (Exception e) {
            log.warn("TTL refresh failed for sessionId: {}", sessionId, e);
        }
    }
    
    /**
     * 캐시 존재 여부 확인
     */
    public boolean exists(String sessionId) {
        try {
            String key = TravelConstants.CACHE_KEY_PREFIX + sessionId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis existence check failed for sessionId: {}", sessionId, e);
            return false;
        }
    }
}