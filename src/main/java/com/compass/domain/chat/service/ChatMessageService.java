package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ChatMessage 생성 및 저장을 담당하는 서비스
 * Single Responsibility Principle 적용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
    
    private final ChatMessageRepository chatMessageRepository;
    
    /**
     * 사용자 메시지 저장
     * 별도 트랜잭션으로 독립적 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatMessage saveUserMessage(ChatThread thread, String content) {
        if (thread == null || content == null || content.trim().isEmpty()) {
            return null;
        }
        
        try {
            ChatMessage message = ChatMessage.builder()
                    .thread(thread)
                    .role("user")
                    .content(content)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            message = chatMessageRepository.save(message);
            chatMessageRepository.flush();
            log.debug("Saved user message for thread: {}", thread.getId());
            return message;
        } catch (Exception e) {
            log.error("Failed to save user message for thread: {}", thread.getId(), e);
            // 메시지 저장 실패는 critical하지 않으므로 null 반환
            return null;
        }
    }
    
    /**
     * 시스템 메시지 저장
     * 별도 트랜잭션으로 독립적 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatMessage saveSystemMessage(ChatThread thread, String content, Map<String, Object> metadata) {
        if (thread == null || content == null) {
            return null;
        }
        
        try {
            ChatMessage message = ChatMessage.builder()
                    .thread(thread)
                    .role("system")
                    .content(content)
                    .timestamp(LocalDateTime.now())
                    .metadata(metadata)
                    .build();
            
            message = chatMessageRepository.save(message);
            chatMessageRepository.flush();
            log.debug("Saved system message for thread: {}", thread.getId());
            return message;
        } catch (Exception e) {
            log.error("Failed to save system message for thread: {}", thread.getId(), e);
            return null;
        }
    }
    
    /**
     * 어시스턴트 메시지 저장
     * 별도 트랜잭션으로 독립적 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatMessage saveAssistantMessage(ChatThread thread, String content, Map<String, Object> metadata) {
        if (thread == null || content == null) {
            return null;
        }
        
        try {
            ChatMessage message = ChatMessage.builder()
                    .thread(thread)
                    .role("assistant")
                    .content(content)
                    .timestamp(LocalDateTime.now())
                    .metadata(metadata)
                    .build();
            
            message = chatMessageRepository.save(message);
            chatMessageRepository.flush();
            log.debug("Saved assistant message for thread: {}", thread.getId());
            return message;
        } catch (Exception e) {
            log.error("Failed to save assistant message for thread: {}", thread.getId(), e);
            return null;
        }
    }
    
    /**
     * 꼬리질문 시작 메시지 저장
     * 각 메시지는 독립적으로 저장됨
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFollowUpStartMessages(ChatThread thread, String initialMessage, String sessionId) {
        if (thread == null) {
            return;
        }
        
        try {
            // 사용자의 초기 메시지 저장
            saveUserMessage(thread, initialMessage);
            
            // 시스템 메시지로 꼬리질문 시작 기록
            saveSystemMessage(
                thread,
                "[꼬리질문 시작] 여행 정보를 수집하기 위한 대화를 시작합니다.",
                Map.of("type", "follow_up_start", "sessionId", sessionId)
            );
            
            log.info("Saved follow-up start messages for thread: {}, session: {}", thread.getId(), sessionId);
        } catch (Exception e) {
            log.error("Failed to save follow-up start messages", e);
            // 메시지 저장 실패는 플로우를 중단시키지 않음
        }
    }
    
    /**
     * 꼬리질문 저장
     * 독립적 트랜잭션으로 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFollowUpQuestion(ChatThread thread, String question, String step) {
        if (thread == null || question == null) {
            return;
        }
        
        try {
            saveAssistantMessage(
                thread,
                question,
                Map.of("type", "follow_up_question", "step", step != null ? step : "")
            );
            log.debug("Saved follow-up question for thread: {}, step: {}", thread.getId(), step);
        } catch (Exception e) {
            log.error("Failed to save follow-up question", e);
        }
    }
}