package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.Stage1ResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stage 1 결과 저장소
 * 
 * 7블록 전략으로 수집한 장소 데이터의 1차 저장 및 조회
 */
@Repository
public interface Stage1ResultRepository extends JpaRepository<Stage1ResultEntity, String> {
    
    /**
     * 스레드 ID로 Stage 1 결과 조회
     */
    Optional<Stage1ResultEntity> findByThreadId(String threadId);
    
    // 가이드대로 간단하게 - 기본 CRUD만 사용
}

