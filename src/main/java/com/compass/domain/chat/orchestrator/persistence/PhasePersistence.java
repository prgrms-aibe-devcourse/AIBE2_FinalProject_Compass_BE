package com.compass.domain.chat.orchestrator.persistence;

import com.compass.domain.chat.model.enums.TravelPhase;

import java.util.Optional;

// Phase 영구 저장소 인터페이스
public interface PhasePersistence {
    Optional<TravelPhase> findByThreadId(String threadId);
    void save(String threadId, TravelPhase phase);
    void deleteByThreadId(String threadId);
}