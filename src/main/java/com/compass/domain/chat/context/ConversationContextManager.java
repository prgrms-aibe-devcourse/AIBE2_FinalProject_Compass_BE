package com.compass.domain.chat.context;

import com.compass.domain.chat.entity.ChatMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * REQ-LLM-006: Conversation Context Management
 * Manages conversation history with token limits and message window
 */
@Component
public class ConversationContextManager {
    
    private static final int MAX_MESSAGES = 10;
    private static final int MAX_TOKENS = 8000; // 8K token limit
    private static final int AVG_CHARS_PER_TOKEN = 4; // Approximate for Korean/English mix
    
    // Thread-safe storage for conversation contexts per thread ID
    private final Map<String, ConversationContext> contexts = new ConcurrentHashMap<>();
    
    /**
     * Get or create conversation context for a thread
     */
    public ConversationContext getContext(String threadId) {
        return contexts.computeIfAbsent(threadId, k -> new ConversationContext(threadId));
    }
    
    /**
     * Add a message to the conversation context
     */
    public void addMessage(String threadId, ChatMessage message) {
        ConversationContext context = getContext(threadId);
        context.addMessage(message);
    }
    
    /**
     * Get messages formatted for Spring AI
     */
    public List<Message> getMessagesForAI(String threadId) {
        ConversationContext context = getContext(threadId);
        return context.getMessagesForAI();
    }
    
    /**
     * Clear context for a thread
     */
    public void clearContext(String threadId) {
        contexts.remove(threadId);
    }
    
    /**
     * Get conversation summary for long contexts
     */
    public String getContextSummary(String threadId) {
        ConversationContext context = getContext(threadId);
        return context.getSummary();
    }
    
    /**
     * Inner class representing a single conversation context
     */
    public static class ConversationContext {
        private final String threadId;
        private final LinkedList<ChatMessage> messages;
        private int totalTokens;
        private String summary;
        
        public ConversationContext(String threadId) {
            this.threadId = threadId;
            this.messages = new LinkedList<>();
            this.totalTokens = 0;
            this.summary = "";
        }
        
        /**
         * Add message with automatic trimming if needed
         */
        public synchronized void addMessage(ChatMessage message) {
            messages.addLast(message);
            totalTokens += estimateTokens(message);
            
            // Trim messages if exceeding limits
            while (shouldTrim()) {
                trimOldestMessage();
            }
        }
        
        /**
         * Check if trimming is needed
         */
        private boolean shouldTrim() {
            return messages.size() > MAX_MESSAGES || totalTokens > MAX_TOKENS;
        }
        
        /**
         * Remove oldest message and update token count
         */
        private void trimOldestMessage() {
            if (!messages.isEmpty()) {
                ChatMessage removed = messages.removeFirst();
                totalTokens -= estimateTokens(removed);
                
                // Update summary with removed message for context preservation
                updateSummary(removed);
            }
        }
        
        /**
         * Estimate token count for a message
         */
        private int estimateTokens(ChatMessage message) {
            String content = message.getContent();
            if (content == null) return 0;
            
            // Simple estimation: characters divided by average chars per token
            // More accurate for Korean text which typically has higher char/token ratio
            return Math.max(1, content.length() / AVG_CHARS_PER_TOKEN);
        }
        
        /**
         * Update summary with trimmed messages
         */
        private void updateSummary(ChatMessage removed) {
            // For now, just note that older context exists
            // In production, could use AI to generate actual summary
            if (summary.isEmpty()) {
                summary = "이전 대화 내용이 있습니다. ";
            }
        }
        
        /**
         * Convert to Spring AI Message format
         */
        public List<Message> getMessagesForAI() {
            List<Message> aiMessages = new ArrayList<>();
            
            // Add summary as system message if exists
            if (!summary.isEmpty()) {
                aiMessages.add(new SystemMessage(summary));
            }
            
            // Convert chat messages to Spring AI format
            for (ChatMessage msg : messages) {
                if ("user".equalsIgnoreCase(msg.getRole())) {
                    aiMessages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                    aiMessages.add(new AssistantMessage(msg.getContent()));
                }
            }
            
            return aiMessages;
        }
        
        /**
         * Get recent messages for display
         */
        public List<ChatMessage> getRecentMessages() {
            return new ArrayList<>(messages);
        }
        
        /**
         * Get context summary
         */
        public String getSummary() {
            if (summary.isEmpty() && !messages.isEmpty()) {
                return String.format("대화 중 (메시지 %d개, 약 %d 토큰)", 
                    messages.size(), totalTokens);
            }
            return summary;
        }
        
        /**
         * Get current token count
         */
        public int getTokenCount() {
            return totalTokens;
        }
        
        /**
         * Get message count
         */
        public int getMessageCount() {
            return messages.size();
        }
    }
}