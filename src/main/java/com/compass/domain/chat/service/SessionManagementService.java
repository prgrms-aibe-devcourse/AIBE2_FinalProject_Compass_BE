package com.compass.domain.chat.service;

import com.compass.domain.chat.constant.FollowUpConstants;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 세션 관리 전용 서비스
 * Single Responsibility Principle 적용 - 세션 관리 책임만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 새로운 세션 ID 생성
     */
    public String generateSessionId() {
        String sessionId = FollowUpConstants.SESSION_ID_PREFIX + 
                          UUID.randomUUID().toString().replace("-", "");
        log.debug("Generated new session ID: {}", sessionId);
        return sessionId;
    }
    
    /**
     * Redis에 세션 저장
     * REQ-FOLLOW-004: Redis 저장 (30분 TTL)
     */
    public void saveSession(String sessionId, TravelInfoCollectionState state) {
        validateSessionId(sessionId);
        validateState(state);
        
        String key = buildRedisKey(sessionId);
        redisTemplate.opsForValue().set(
            key, 
            state, 
            FollowUpConstants.SESSION_TTL_MINUTES, 
            TimeUnit.MINUTES
        );
        
        log.info("Session saved to Redis - ID: {}, TTL: {} minutes", 
                sessionId, FollowUpConstants.SESSION_TTL_MINUTES);
    }
    
    /**
     * Redis에서 세션 로드
     */
    public TravelInfoCollectionState loadSession(String sessionId) {
        validateSessionId(sessionId);
        
        String key = buildRedisKey(sessionId);
        TravelInfoCollectionState state = (TravelInfoCollectionState) 
            redisTemplate.opsForValue().get(key);
        
        if (state != null) {
            log.debug("Session loaded from Redis: {}", sessionId);
        } else {
            log.warn("Session not found in Redis: {}", sessionId);
        }
        
        return state;
    }
    
    /**
     * 세션 삭제
     */
    public boolean deleteSession(String sessionId) {
        validateSessionId(sessionId);
        
        String key = buildRedisKey(sessionId);
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.info("Session deleted from Redis: {}", sessionId);
            return true;
        } else {
            log.warn("Failed to delete session or session not found: {}", sessionId);
            return false;
        }
    }
    
    /**
     * 세션 만료 시간 연장
     */
    public boolean extendSessionTTL(String sessionId) {
        validateSessionId(sessionId);
        
        String key = buildRedisKey(sessionId);
        Boolean extended = redisTemplate.expire(
            key, 
            FollowUpConstants.SESSION_TTL_MINUTES, 
            TimeUnit.MINUTES
        );
        
        if (Boolean.TRUE.equals(extended)) {
            log.debug("Session TTL extended: {}", sessionId);
            return true;
        } else {
            log.warn("Failed to extend session TTL: {}", sessionId);
            return false;
        }
    }
    
    /**
     * 세션 존재 여부 확인
     */
    public boolean sessionExists(String sessionId) {
        validateSessionId(sessionId);
        
        String key = buildRedisKey(sessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Redis 키 생성
     */
    private String buildRedisKey(String sessionId) {
        return FollowUpConstants.SESSION_PREFIX + sessionId;
    }
    
    /**
     * 세션 ID 유효성 검증
     */
    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
    }
    
    /**
     * 상태 객체 유효성 검증
     */
    private void validateState(TravelInfoCollectionState state) {
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        if (state.getSessionId() == null || state.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("State must have a valid session ID");
        }
    }
}