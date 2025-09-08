package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ChatMessage entity
 * Handles database operations for chat messages
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Find all messages in a thread, ordered by timestamp descending
     * @param threadId Thread ID
     * @return List of messages
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.thread.id = :threadId ORDER BY cm.timestamp DESC")
    List<ChatMessage> findByThreadIdOrderByTimestampDesc(@Param("threadId") String threadId);
    
    /**
     * Find messages in a thread with pagination
     * @param threadId Thread ID
     * @param pageable Pagination information
     * @return Page of messages
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.thread.id = :threadId")
    Page<ChatMessage> findByThreadId(@Param("threadId") String threadId, Pageable pageable);
    
    /**
     * Find messages before a specific timestamp (for pagination)
     * @param threadId Thread ID
     * @param before Timestamp to search before
     * @param pageable Pagination information
     * @return Page of messages
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.thread.id = :threadId AND cm.timestamp < :before")
    Page<ChatMessage> findByThreadIdAndTimestampBefore(
        @Param("threadId") String threadId, 
        @Param("before") LocalDateTime before, 
        Pageable pageable
    );
    
    /**
     * Find latest N messages in a thread
     * @param threadId Thread ID
     * @param limit Number of messages to retrieve
     * @return List of messages
     */
    @Query(value = "SELECT * FROM chat_messages WHERE thread_id = :threadId ORDER BY timestamp DESC LIMIT :limit", 
           nativeQuery = true)
    List<ChatMessage> findLatestMessagesByThreadId(@Param("threadId") String threadId, @Param("limit") int limit);
    
    /**
     * Count messages in a thread
     * @param threadId Thread ID
     * @return Number of messages
     */
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.thread.id = :threadId")
    long countByThreadId(@Param("threadId") String threadId);
    
    /**
     * Find messages by role in a thread
     * @param threadId Thread ID
     * @param role Message role (user, assistant, system)
     * @return List of messages
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.thread.id = :threadId AND cm.role = :role ORDER BY cm.timestamp DESC")
    List<ChatMessage> findByThreadIdAndRole(@Param("threadId") String threadId, @Param("role") String role);
    
    /**
     * Delete all messages in a thread (for cascade operations)
     * @param threadId Thread ID
     */
    @Query("DELETE FROM ChatMessage cm WHERE cm.thread.id = :threadId")
    void deleteByThreadId(@Param("threadId") String threadId);
}