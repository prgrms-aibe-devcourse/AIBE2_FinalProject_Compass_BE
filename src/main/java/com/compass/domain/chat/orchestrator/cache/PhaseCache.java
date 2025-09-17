package com.compass.domain.chat.orchestrator.cache;

import com.compass.domain.chat.model.enums.TravelPhase;

import java.util.Optional;

// Phase 캐시 인터페이스 - Redis 구현과 분리
public interface PhaseCache {
    Optional<TravelPhase> get(String threadId);
    void put(String threadId, TravelPhase phase);
    void evict(String threadId);
}