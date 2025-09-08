package com.compass.domain.chat.context;

import com.compass.domain.chat.entity.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConversationContextManager (REQ-LLM-006)
 */
@Tag("unit")
@DisplayName("대화 컨텍스트 관리자 테스트")
class ConversationContextManagerTest {
    
    private ConversationContextManager contextManager;
    
    @BeforeEach
    void setUp() {
        contextManager = new ConversationContextManager();
    }
    
    @Test
    @DisplayName("새로운 대화 컨텍스트 생성 테스트")
    void testCreateNewContext() {
        // Given
        String threadId = "test-thread-1";
        
        // When
        ConversationContextManager.ConversationContext context = contextManager.getContext(threadId);
        
        // Then
        assertThat(context).isNotNull();
        assertThat(context.getMessageCount()).isEqualTo(0);
        assertThat(context.getTokenCount()).isEqualTo(0);
        assertThat(context.getSummary()).isEmpty();
    }
    
    @Test
    @DisplayName("메시지 추가 테스트")
    void testAddMessage() {
        // Given
        String threadId = "test-thread-2";
        ChatMessage userMessage = ChatMessage.builder()
                .role("user")
                .content("안녕하세요, 여행 계획을 도와주세요.")
                .build();
        ChatMessage assistantMessage = ChatMessage.builder()
                .role("assistant")
                .content("안녕하세요! 어떤 여행을 계획하고 계신가요?")
                .build();
        
        // When
        contextManager.addMessage(threadId, userMessage);
        contextManager.addMessage(threadId, assistantMessage);
        ConversationContextManager.ConversationContext context = contextManager.getContext(threadId);
        
        // Then
        assertThat(context.getMessageCount()).isEqualTo(2);
        assertThat(context.getTokenCount()).isGreaterThan(0);
        
        List<Message> messages = contextManager.getMessagesForAI(threadId);
        assertThat(messages).hasSize(2);
    }
    
    @Test
    @DisplayName("최대 메시지 수 제한 테스트")
    void testMaxMessagesLimit() {
        // Given
        String threadId = "test-thread-3";
        
        // When - Add 12 messages (more than MAX_MESSAGES=10)
        for (int i = 1; i <= 12; i++) {
            ChatMessage message = ChatMessage.builder()
                        .role(i % 2 == 1 ? "user" : "assistant")
                    .content("메시지 " + i)
                    .build();
            contextManager.addMessage(threadId, message);
        }
        
        ConversationContextManager.ConversationContext context = contextManager.getContext(threadId);
        
        // Then - Should keep only 10 most recent messages
        assertThat(context.getMessageCount()).isLessThanOrEqualTo(10);
        
        // Check that older messages were removed and summary was updated
        List<ChatMessage> recentMessages = context.getRecentMessages();
        assertThat(recentMessages.get(recentMessages.size() - 1).getContent()).isEqualTo("메시지 12");
    }
    
    @Test
    @DisplayName("토큰 제한 테스트")
    void testTokenLimit() {
        // Given
        String threadId = "test-thread-4";
        String longMessage = "이것은 매우 긴 메시지입니다. ".repeat(500); // Very long message
        
        // When - Add messages that exceed token limit
        for (int i = 1; i <= 5; i++) {
            ChatMessage message = ChatMessage.builder()
                        .role("user")
                    .content(longMessage + i)
                    .build();
            contextManager.addMessage(threadId, message);
        }
        
        ConversationContextManager.ConversationContext context = contextManager.getContext(threadId);
        
        // Then - Token count should be within limit
        assertThat(context.getTokenCount()).isLessThanOrEqualTo(8000);
        assertThat(context.getMessageCount()).isLessThanOrEqualTo(10);
    }
    
    @Test
    @DisplayName("Spring AI 메시지 변환 테스트")
    void testMessagesForAI() {
        // Given
        String threadId = "test-thread-5";
        contextManager.addMessage(threadId, ChatMessage.builder()
                .role("user")
                .content("제주도 여행 계획 짜줘")
                .build());
        contextManager.addMessage(threadId, ChatMessage.builder()
                .role("assistant")
                .content("제주도 여행 일정을 도와드리겠습니다.")
                .build());
        
        // When
        List<Message> aiMessages = contextManager.getMessagesForAI(threadId);
        
        // Then
        assertThat(aiMessages).hasSize(2);
        assertThat(aiMessages.get(0).getContent()).isEqualTo("제주도 여행 계획 짜줘");
        assertThat(aiMessages.get(1).getContent()).isEqualTo("제주도 여행 일정을 도와드리겠습니다.");
    }
    
    @Test
    @DisplayName("대화 컨텍스트 초기화 테스트")
    void testClearContext() {
        // Given
        String threadId = "test-thread-6";
        contextManager.addMessage(threadId, ChatMessage.builder()
                .role("user")
                .content("테스트 메시지")
                .build());
        
        // When
        contextManager.clearContext(threadId);
        ConversationContextManager.ConversationContext context = contextManager.getContext(threadId);
        
        // Then - Should be a new empty context
        assertThat(context.getMessageCount()).isEqualTo(0);
        assertThat(context.getTokenCount()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("컨텍스트 요약 생성 테스트")
    void testContextSummary() {
        // Given
        String threadId = "test-thread-7";
        
        // When - Add messages to create context
        for (int i = 1; i <= 3; i++) {
            contextManager.addMessage(threadId, ChatMessage.builder()
                        .role(i % 2 == 1 ? "user" : "assistant")
                    .content("메시지 " + i)
                    .build());
        }
        
        // Then
        String summary = contextManager.getContextSummary(threadId);
        assertThat(summary).contains("대화 중");
        assertThat(summary).contains("메시지");
        assertThat(summary).contains("토큰");
    }
    
    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시 접근")
    void testConcurrency() throws InterruptedException {
        // Given
        String threadId = "test-thread-concurrent";
        
        // When - Multiple threads adding messages simultaneously
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                ChatMessage message = ChatMessage.builder()
                                .role("user")
                        .content("Thread " + index + " message")
                        .build();
                contextManager.addMessage(threadId, message);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then - All messages should be added safely
        ConversationContextManager.ConversationContext context = contextManager.getContext(threadId);
        assertThat(context.getMessageCount()).isEqualTo(10);
    }
}