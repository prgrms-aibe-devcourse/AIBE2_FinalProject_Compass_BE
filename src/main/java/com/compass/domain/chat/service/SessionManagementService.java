package com.compass.domain.chat.service;

import com.compass.domain.chat.constant.FollowUpConstants;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.repository.TravelInfoCollectionStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 세션 관리 전용 서비스 - DB만 사용하는 심플한 버전
 * Redis 관련 복잡한 로직을 제거하고 DB로만 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManagementService {
    
    private final TravelInfoCollectionStateRepository stateRepository;
    
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
     * DB에 세션 저장 (단순화)
     */
    @Transactional
    public void saveSession(String sessionId, TravelInfoCollectionState state) {
        try {
            // 이미 DB에 저장되어 있으므로 추가 작업 불필요
            log.debug("Session already saved in DB: {}", sessionId);
        } catch (Exception e) {
            log.error("Failed to verify session in DB: {}", sessionId, e);
        }
    }
    
    /**
     * DB에서 세션 로드 (단순화)
     */
    @Transactional(readOnly = true)
    public TravelInfoCollectionState loadSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("Invalid session ID: {}", sessionId);
            return null;
        }
        
        try {
            TravelInfoCollectionState state = stateRepository.findBySessionId(sessionId).orElse(null);
            
            if (state != null) {
                log.debug("Session loaded from DB: {}", sessionId);
            } else {
                log.warn("Session not found in DB: {}", sessionId);
            }
            
            return state;
        } catch (Exception e) {
            log.error("Failed to load session from DB: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 세션 삭제 (단순화)
     */
    @Transactional
    public boolean deleteSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        try {
            stateRepository.findBySessionId(sessionId).ifPresent(state -> {
                stateRepository.delete(state);
                log.info("Session deleted from DB: {}", sessionId);
            });
            return true;
        } catch (Exception e) {
            log.error("Failed to delete session from DB: {}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 세션 존재 여부 확인 (단순화)
     */
    @Transactional(readOnly = true)
    public boolean sessionExists(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return false;
        }
        
        try {
            return stateRepository.existsBySessionId(sessionId);
        } catch (Exception e) {
            log.error("Failed to check session existence: {}", sessionId, e);
            return false;
        }
    }
    
    /**
     * 세션 TTL 연장 - DB 사용 시 불필요하므로 항상 true 반환
     */
    public boolean extendSessionTTL(String sessionId) {
        // DB 사용 시 TTL 개념이 없으므로 세션 존재 여부만 확인
        return sessionExists(sessionId);
    }
}