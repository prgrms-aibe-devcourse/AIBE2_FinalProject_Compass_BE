package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 여행 정보 수집 상태 리포지토리
 * REQ-FOLLOW-002: 정보 수집 상태 관리를 위한 데이터 액세스 계층
 */
@Repository
public interface TravelInfoCollectionRepository extends JpaRepository<TravelInfoCollectionState, Long> {
    
    /**
     * 세션 ID로 수집 상태 조회
     */
    Optional<TravelInfoCollectionState> findBySessionId(String sessionId);
    
    /**
     * 사용자의 최신 미완료 수집 상태 조회
     */
    Optional<TravelInfoCollectionState> findFirstByUserAndIsCompletedFalseOrderByCreatedAtDesc(User user);
    
    /**
     * 채팅 스레드의 수집 상태 조회
     */
    Optional<TravelInfoCollectionState> findByChatThreadAndIsCompletedFalse(ChatThread chatThread);
    
    /**
     * 사용자의 모든 수집 상태 조회
     */
    List<TravelInfoCollectionState> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 사용자의 완료된 수집 상태 조회
     */
    List<TravelInfoCollectionState> findByUserAndIsCompletedTrueOrderByCompletedAtDesc(User user);
    
    /**
     * 특정 기간 내 생성된 미완료 상태 조회 (타임아웃 처리용)
     */
    @Query("SELECT t FROM TravelInfoCollectionState t WHERE t.isCompleted = false AND t.createdAt < :timeoutTime")
    List<TravelInfoCollectionState> findIncompleteStatesBeforeTime(@Param("timeoutTime") LocalDateTime timeoutTime);
    
    /**
     * 사용자와 채팅 스레드로 수집 상태 조회
     */
    Optional<TravelInfoCollectionState> findByUserAndChatThread(User user, ChatThread chatThread);
    
    /**
     * 세션 ID 존재 여부 확인
     */
    boolean existsBySessionId(String sessionId);
    
    /**
     * 사용자의 진행 중인 수집 상태 개수 조회
     */
    long countByUserAndIsCompletedFalse(User user);
    
    /**
     * 특정 단계에 있는 수집 상태 조회
     */
    @Query("SELECT t FROM TravelInfoCollectionState t WHERE t.currentStep = :step AND t.isCompleted = false")
    List<TravelInfoCollectionState> findByCurrentStep(@Param("step") TravelInfoCollectionState.CollectionStep step);
    
    /**
     * 완료율이 특정 퍼센트 이상인 미완료 상태 조회
     */
    @Query("""
        SELECT t FROM TravelInfoCollectionState t 
        WHERE t.isCompleted = false 
        AND (
            (CASE WHEN t.destinationCollected = true THEN 1 ELSE 0 END +
             CASE WHEN t.datesCollected = true THEN 1 ELSE 0 END +
             CASE WHEN t.durationCollected = true THEN 1 ELSE 0 END +
             CASE WHEN t.companionsCollected = true THEN 1 ELSE 0 END +
             CASE WHEN t.budgetCollected = true THEN 1 ELSE 0 END) * 100 / 5
        ) >= :percentage
        """)
    List<TravelInfoCollectionState> findIncompleteWithCompletionPercentageAbove(@Param("percentage") int percentage);
}