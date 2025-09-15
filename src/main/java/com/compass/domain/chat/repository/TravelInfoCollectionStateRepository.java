package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TravelInfoCollectionState entity
 * Handles database operations for travel info collection states
 */
@Repository
public interface TravelInfoCollectionStateRepository extends JpaRepository<TravelInfoCollectionState, Long> {
    
    /**
     * Find state by session ID
     * @param sessionId Session ID
     * @return Optional of TravelInfoCollectionState
     */
    Optional<TravelInfoCollectionState> findBySessionId(String sessionId);
    
    /**
     * Find all states for a specific user
     * @param userId User ID
     * @return List of states
     */
    @Query("SELECT t FROM TravelInfoCollectionState t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<TravelInfoCollectionState> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find all states for a specific chat thread
     * @param threadId Thread ID
     * @return List of states
     */
    @Query("SELECT t FROM TravelInfoCollectionState t WHERE t.chatThread.id = :threadId ORDER BY t.createdAt DESC")
    List<TravelInfoCollectionState> findByChatThreadId(@Param("threadId") String threadId);
    
    /**
     * Find completed states for a user
     * @param userId User ID
     * @return List of completed states
     */
    @Query("SELECT t FROM TravelInfoCollectionState t WHERE t.user.id = :userId AND t.isCompleted = true ORDER BY t.completedAt DESC")
    List<TravelInfoCollectionState> findCompletedByUserId(@Param("userId") Long userId);
    
    /**
     * Check if a session exists
     * @param sessionId Session ID
     * @return true if exists
     */
    boolean existsBySessionId(String sessionId);
    
    /**
     * Delete old incomplete sessions (for cleanup)
     * @param userId User ID
     * @return Number of deleted records
     */
    @Query("DELETE FROM TravelInfoCollectionState t WHERE t.user.id = :userId AND t.isCompleted = false")
    int deleteIncompleteByUserId(@Param("userId") Long userId);
}