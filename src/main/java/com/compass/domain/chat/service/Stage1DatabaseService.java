package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.Stage1ResultEntity;
import com.compass.domain.chat.repository.Stage1ResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Stage 1 데이터베이스 서비스
 * 
 * 7블록 전략으로 수집한 데이터의 1차 저장 및 Tour API 보완 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Stage1DatabaseService {
    
    private final Stage1ResultRepository stage1ResultRepository;
    
    /**
     * Stage 1 결과 1차 저장 (Perplexity 데이터)
     * 
     * @param threadId 스레드 ID
     * @param places 수집된 장소 리스트
     */
    @Transactional
    public void savePrimaryResult(String threadId, List<PlaceDeduplicator.TourPlace> places) {
        log.info("Stage 1 1차 결과 저장 시작: threadId={}, 장소수={}", threadId, places.size());
        
        try {
            Stage1ResultEntity entity = new Stage1ResultEntity();
            entity.setThreadId(threadId);
            entity.setPlaces(places);
            
            stage1ResultRepository.save(entity);
            log.info("Stage 1 1차 결과 저장 완료: threadId={}", threadId);
            
        } catch (Exception e) {
            log.error("Stage 1 1차 결과 저장 실패: threadId={}", threadId, e);
            throw new RuntimeException("Stage 1 결과 저장 실패", e);
        }
    }
    
    /**
     * Tour API 보완 완료 표시 (간단하게)
     * 
     * @param threadId 스레드 ID
     * @param enhancedPlaces 보완된 장소 리스트
     */
    @Transactional
    public void markAsEnhanced(String threadId, List<PlaceDeduplicator.TourPlace> enhancedPlaces) {
        log.info("Tour API 보완 완료 표시: threadId={}", threadId);
        
        Optional<Stage1ResultEntity> entityOpt = stage1ResultRepository.findById(threadId);
        if (entityOpt.isPresent()) {
            Stage1ResultEntity entity = entityOpt.get();
            entity.setPlaces(enhancedPlaces);  // 보완된 데이터로 업데이트
            
            stage1ResultRepository.save(entity);
            log.info("Tour API 보완 완료: threadId={}, 최종 장소수={}", threadId, enhancedPlaces.size());
        } else {
            log.warn("Tour API 보완 대상을 찾을 수 없음: threadId={}", threadId);
        }
    }
    
    /**
     * Stage 1 결과 조회
     * 
     * @param threadId 스레드 ID
     * @return 저장된 장소 리스트
     */
    public Optional<List<PlaceDeduplicator.TourPlace>> getResult(String threadId) {
        return stage1ResultRepository.findById(threadId)
            .map(Stage1ResultEntity::getPlaces);
    }
    
    /**
     * 결과 존재 여부 확인
     * 
     * @param threadId 스레드 ID
     * @return 존재 여부
     */
    public boolean existsResult(String threadId) {
        return stage1ResultRepository.existsById(threadId);
    }
}
