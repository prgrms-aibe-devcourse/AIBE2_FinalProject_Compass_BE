package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// 컨텍스트 관리자
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextManager {

    // 스레드별 컨텍스트 저장소
    private final Map<String, TravelContext> contextStore = new ConcurrentHashMap<>();

    // 컨텍스트 조회 또는 생성
    public TravelContext getOrCreateContext(String threadId, String userId) {
        return contextStore.computeIfAbsent(threadId, k -> createNewContext(threadId, userId));
    }

    // ChatRequest로부터 컨텍스트 조회 또는 생성
    public TravelContext getOrCreateContext(ChatRequest request) {
        return getOrCreateContext(request.getThreadId(), request.getUserId());
    }

    // 컨텍스트 조회
    public Optional<TravelContext> getContext(String threadId) {
        return Optional.ofNullable(contextStore.get(threadId));
    }

    // 컨텍스트 업데이트
    public void updateContext(TravelContext context) {
        if (!isValidContext(context)) {
            log.warn("유효하지 않은 컨텍스트 업데이트 시도");
            return;
        }

        contextStore.put(context.getThreadId(), context);
        log.debug("컨텍스트 업데이트 완료: threadId={}", context.getThreadId());
    }

    // 컨텍스트 초기화
    public void resetContext(String threadId) {
        if (threadId == null) {
            log.warn("null threadId로 초기화 시도");
            return;
        }

        var removed = contextStore.remove(threadId);
        if (removed != null) {
            log.info("컨텍스트 초기화 완료: threadId={}", threadId);
        } else {
            log.debug("초기화할 컨텍스트 없음: threadId={}", threadId);
        }
    }

    // 컨텍스트 존재 확인
    public boolean hasContext(String threadId) {
        return contextStore.containsKey(threadId);
    }

    // 전체 컨텍스트 개수 조회
    public int getContextCount() {
        return contextStore.size();
    }

    // 새 컨텍스트 생성
    private TravelContext createNewContext(String threadId, String userId) {
        log.debug("새 컨텍스트 생성: threadId={}, userId={}", threadId, userId);
        return TravelContext.builder()
            .threadId(threadId)
            .userId(userId)
            .currentPhase(TravelPhase.INITIALIZATION.name())
            .build();
    }

    // 컨텍스트 유효성 검사
    private boolean isValidContext(TravelContext context) {
        return context != null && context.getThreadId() != null;
    }
}