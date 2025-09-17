package com.compass.domain.chat.orchestrator.strategy;

// Phase 로드 전략 열거형
public enum PhaseLoadStrategy {
    CACHE_FIRST,    // 진행중인 대화 - Redis 우선
    DB_FIRST        // 새 대화나 히스토리 조회 - DB 우선
}