package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.ChatThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatThread entity
 * Handles database operations for chat threads
 */
@Repository
public interface ChatThreadRepository extends JpaRepository<ChatThread, String> {
    
    /**
     * Find all threads for a specific user, ordered by last message time
     * @param userId User ID
     * @return List of chat threads
     */
    @Query("SELECT ct FROM ChatThread ct WHERE ct.user.id = :userId ORDER BY ct.lastMessageAt DESC NULLS LAST, ct.createdAt DESC")
    List<ChatThread> findByUserIdOrderByLastMessageAtDesc(@Param("userId") Long userId);
    
    /**
     * Find threads for a user with pagination
     * @param userId User ID
     * @param pageable Pagination information
     * @return Page of chat threads
     */
    @Query("SELECT ct FROM ChatThread ct WHERE ct.user.id = :userId")
    Page<ChatThread> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find a specific thread by ID and user ID (for security)
     * @param threadId Thread ID
     * @param userId User ID
     * @return Optional of ChatThread
     */
    @Query("SELECT ct FROM ChatThread ct WHERE ct.id = :threadId AND ct.user.id = :userId")
    Optional<ChatThread> findByIdAndUserId(@Param("threadId") String threadId, @Param("userId") Long userId);
    
    /**
     * Check if a thread exists for a specific user
     * @param threadId Thread ID
     * @param userId User ID
     * @return true if exists
     */
    @Query("SELECT COUNT(ct) > 0 FROM ChatThread ct WHERE ct.id = :threadId AND ct.user.id = :userId")
    boolean existsByIdAndUserId(@Param("threadId") String threadId, @Param("userId") Long userId);
    
    /**
     * Count threads for a specific user
     * @param userId User ID
     * @return Number of threads
     */
    @Query("SELECT COUNT(ct) FROM ChatThread ct WHERE ct.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}