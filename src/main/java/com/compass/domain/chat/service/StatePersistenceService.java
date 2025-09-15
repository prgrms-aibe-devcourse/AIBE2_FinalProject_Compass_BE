package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.exception.FollowUpException;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.repository.TravelInfoCollectionStateRepository;
import com.compass.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 상태 영속성 관리 서비스 - DB만 사용하는 심플한 버전
 * Redis 관련 복잡한 로직을 제거하고 DB로만 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatePersistenceService {
    
    private final TravelInfoCollectionStateRepository stateRepository;
    private final ChatThreadRepository chatThreadRepository;
    
    /**
     * ChatThread 생성 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatThread persistChatThread(User user, String title) {
        try {
            ChatThread chatThread = ChatThread.builder()
                    .user(user)
                    .title(title)
                    .createdAt(LocalDateTime.now())
                    .lastMessageAt(LocalDateTime.now())
                    .build();
            
            chatThread = chatThreadRepository.save(chatThread);
            chatThreadRepository.flush();
            
            log.info("Created new ChatThread with ID: {}", chatThread.getId());
            return chatThread;
        } catch (Exception e) {
            log.error("Failed to create ChatThread", e);
            throw new RuntimeException("Failed to create ChatThread", e);
        }
    }
    
    /**
     * 상태 저장 (독립 트랜잭션) - DB만 사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TravelInfoCollectionState persistState(TravelInfoCollectionState state) {
        try {
            log.info("Persisting state for session: {}", state.getSessionId());
            
            // DB에 저장
            state = stateRepository.save(state);
            stateRepository.flush();
            
            log.info("Successfully persisted state. ID: {}, SessionId: {}", 
                     state.getId(), state.getSessionId());
            
            // 검증
            boolean exists = stateRepository.existsBySessionId(state.getSessionId());
            log.info("Verification - State exists in database: {}", exists);
            
            return state;
        } catch (Exception e) {
            log.error("Failed to persist state for session: {}", state.getSessionId(), e);
            throw new FollowUpException.StateSaveException(state.getSessionId(), e);
        }
    }
    
    /**
     * ChatThread 업데이트 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateChatThreadLastMessage(String threadId) {
        try {
            chatThreadRepository.findById(threadId).ifPresent(thread -> {
                thread.setLastMessageAt(LocalDateTime.now());
                chatThreadRepository.save(thread);
                chatThreadRepository.flush();
                log.debug("Updated last message time for thread: {}", threadId);
            });
        } catch (Exception e) {
            log.error("Failed to update thread last message time", e);
        }
    }
}