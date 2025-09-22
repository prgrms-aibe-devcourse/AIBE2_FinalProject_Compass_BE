package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.orchestrator.cache.ContextCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 컨텍스트 관리자 (Redis 기반)
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextManager {

    // Redis 기반 컨텍스트 캐시
    private final ContextCache contextCache;

    // 컨텍스트 조회 또는 생성 (userId 검증 포함)
    public TravelContext getOrCreateContext(String threadId, String userId) {
        var existingOpt = contextCache.get(threadId);

        // 기존 컨텍스트가 있는 경우 userId 검증
        if (existingOpt.isPresent()) {
            var existing = existingOpt.get();
            if (!existing.getUserId().equals(userId)) {
                log.error("권한 없는 접근 시도: threadId={}, requestUserId={}, ownerUserId={}",
                    threadId, userId, existing.getUserId());
                throw new SecurityException("다른 사용자의 대화 스레드에 접근할 수 없습니다.");
            }
            log.debug("기존 컨텍스트 로드: threadId={}, phase={}, waitingForConfirmation={}",
                threadId, existing.getCurrentPhase(), existing.isWaitingForTravelConfirmation());
            return existing;
        }

        // 새 컨텍스트 생성
        var newContext = createNewContext(threadId, userId);
        contextCache.put(threadId, newContext);
        log.debug("새 컨텍스트 생성 및 저장: threadId={}, userId={}, phase={}",
            threadId, userId, newContext.getCurrentPhase());
        return newContext;
    }

    // ChatRequest로부터 컨텍스트 조회 또는 생성
    public TravelContext getOrCreateContext(ChatRequest request) {
        return getOrCreateContext(request.getThreadId(), request.getUserId());
    }

    // 컨텍스트 조회 (userId 검증 포함)
    public Optional<TravelContext> getContext(String threadId, String userId) {
        return contextCache.get(threadId)
            .filter(context -> context.getUserId().equals(userId));
    }

    // 컨텍스트 조회 (검증 없는 내부용)
    private Optional<TravelContext> getContextInternal(String threadId) {
        return contextCache.get(threadId);
    }

    // 컨텍스트 업데이트 (userId 검증 포함)
    public void updateContext(TravelContext context, String userId) {
        if (!isValidContext(context)) {
            log.warn("유효하지 않은 컨텍스트 업데이트 시도");
            return;
        }

        // 기존 컨텍스트의 소유자 확인
        var existingOpt = contextCache.get(context.getThreadId());
        if (existingOpt.isPresent() && !existingOpt.get().getUserId().equals(userId)) {
            log.error("권한 없는 업데이트 시도: threadId={}, requestUserId={}, ownerUserId={}",
                context.getThreadId(), userId, existingOpt.get().getUserId());
            throw new SecurityException("다른 사용자의 컨텍스트를 수정할 수 없습니다.");
        }

        contextCache.put(context.getThreadId(), context);
        log.debug("컨텍스트 업데이트 완료: threadId={}, userId={}, phase={}, waitingForConfirmation={}",
            context.getThreadId(), userId, context.getCurrentPhase(), context.isWaitingForTravelConfirmation());
    }

    // 컨텍스트 초기화 (userId 검증 포함)
    public void resetContext(String threadId, String userIdOrEmail) {
        if (threadId == null) {
            log.warn("null threadId로 초기화 시도");
            return;
        }

        // 기존 컨텍스트 조회
        var existingOpt = contextCache.get(threadId);
        if (existingOpt.isPresent()) {
            var existingContext = existingOpt.get();
            // userId가 숫자일 수도 있고 email일 수도 있으므로 유연하게 체크
            boolean isOwner = existingContext.getUserId().equals(userIdOrEmail) ||
                             existingContext.getUserId().equalsIgnoreCase(userIdOrEmail) ||
                             userIdOrEmail.contains("@") && existingContext.getUserId().contains("@");

            if (!isOwner) {
                log.warn("컨텍스트 초기화 권한 경고: threadId={}, requestUser={}, owner={}",
                    threadId, userIdOrEmail, existingContext.getUserId());
                // 개발 환경에서는 경고만 하고 진행
            }
        }

        // 새 컨텍스트 생성 후 저장 (초기화 = 새 컨텍스트로 교체)
        var newContext = createNewContext(threadId, userIdOrEmail);
        contextCache.put(threadId, newContext);
        log.info("✅ 컨텍스트 초기화 완료: threadId={}, userId={}", threadId, userIdOrEmail);
    }

    // 컨텍스트 존재 확인 (userId 검증 포함)
    public boolean hasContext(String threadId, String userId) {
        var contextOpt = contextCache.get(threadId);
        return contextOpt.isPresent() && contextOpt.get().getUserId().equals(userId);
    }

    // 캐시 제거 (필요시)
    public void evictContext(String threadId) {
        contextCache.evict(threadId);
        log.debug("컨텍스트 캐시 제거: threadId={}", threadId);
    }

    // 새 컨텍스트 생성
    private TravelContext createNewContext(String threadId, String userId) {
        log.debug("새 컨텍스트 생성: threadId={}, userId={}", threadId, userId);
        return TravelContext.builder()
            .threadId(threadId)
            .userId(userId)
            .currentPhase(TravelPhase.INITIALIZATION.name())
            .waitingForTravelConfirmation(false)
            .conversationCount(0)
            .build();
    }

    // 컨텍스트 유효성 검사
    private boolean isValidContext(TravelContext context) {
        return context != null && context.getThreadId() != null && context.getUserId() != null;
    }
}