package com.compass.domain.chat.orchestrator.cache;

import com.compass.domain.chat.model.context.TravelContext;
import java.util.Optional;

// 컨텍스트 캐시 인터페이스
public interface ContextCache {

    // 캐시에서 컨텍스트 조회
    Optional<TravelContext> get(String threadId);

    // 캐시에 컨텍스트 저장
    void put(String threadId, TravelContext context);

    // 캐시에서 컨텍스트 삭제
    void evict(String threadId);
}