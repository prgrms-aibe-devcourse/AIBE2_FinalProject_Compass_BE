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

    // 컨텍스트 조회 또는 생성 (userId 검증 포함)
    public TravelContext getOrCreateContext(String threadId, String userId) {
        var existing = contextStore.get(threadId);

        // 기존 컨텍스트가 있는 경우 userId 검증
        if (existing != null) {
            if (!existing.getUserId().equals(userId)) {
                log.error("권한 없는 접근 시도: threadId={}, requestUserId={}, ownerUserId={}",
                    threadId, userId, existing.getUserId());
                throw new SecurityException("다른 사용자의 대화 스레드에 접근할 수 없습니다.");
            }
            return existing;
        }

        // 새 컨텍스트 생성
        return contextStore.computeIfAbsent(threadId, k -> createNewContext(threadId, userId));
    }

    // ChatRequest로부터 컨텍스트 조회 또는 생성
    public TravelContext getOrCreateContext(ChatRequest request) {
        return getOrCreateContext(request.getThreadId(), request.getUserId());
    }

    // 컨텍스트 조회 (userId 검증 포함)
    public Optional<TravelContext> getContext(String threadId, String userId) {
        return Optional.ofNullable(contextStore.get(threadId))
            .filter(context -> context.getUserId().equals(userId));
    }

    // 컨텍스트 조회 (검증 없는 내부용)
    private Optional<TravelContext> getContextInternal(String threadId) {
        return Optional.ofNullable(contextStore.get(threadId));
    }

    // 컨텍스트 업데이트 (userId 검증 포함)
    public void updateContext(TravelContext context, String userId) {
        if (!isValidContext(context)) {
            log.warn("유효하지 않은 컨텍스트 업데이트 시도");
            return;
        }

        // 기존 컨텍스트의 소유자 확인
        var existing = contextStore.get(context.getThreadId());
        if (existing != null && !existing.getUserId().equals(userId)) {
            log.error("권한 없는 업데이트 시도: threadId={}, requestUserId={}, ownerUserId={}",
                context.getThreadId(), userId, existing.getUserId());
            throw new SecurityException("다른 사용자의 컨텍스트를 수정할 수 없습니다.");
        }

        contextStore.put(context.getThreadId(), context);
        log.debug("컨텍스트 업데이트 완료: threadId={}, userId={}", context.getThreadId(), userId);
    }

    // 컨텍스트 초기화 (userId 검증 포함)
    public void resetContext(String threadId, String userId) {
        if (threadId == null) {
            log.warn("null threadId로 초기화 시도");
            return;
        }

        // 기존 컨텍스트의 소유자 확인
        var existing = contextStore.get(threadId);
        if (existing != null && !existing.getUserId().equals(userId)) {
            log.error("권한 없는 초기화 시도: threadId={}, requestUserId={}, ownerUserId={}",
                threadId, userId, existing.getUserId());
            throw new SecurityException("다른 사용자의 컨텍스트를 초기화할 수 없습니다.");
        }

        var removed = contextStore.remove(threadId);
        if (removed != null) {
            log.info("컨텍스트 초기화 완료: threadId={}, userId={}", threadId, userId);
        } else {
            log.debug("초기화할 컨텍스트 없음: threadId={}", threadId);
        }
    }

    // 컨텍스트 존재 확인 (userId 검증 포함)
    public boolean hasContext(String threadId, String userId) {
        var context = contextStore.get(threadId);
        return context != null && context.getUserId().equals(userId);
    }

    // 사용자별 컨텍스트 개수 조회
    public int getUserContextCount(String userId) {
        return (int) contextStore.values().stream()
            .filter(context -> context.getUserId().equals(userId))
            .count();
    }

    // 전체 컨텍스트 개수 조회 (관리자용)
    public int getTotalContextCount() {
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