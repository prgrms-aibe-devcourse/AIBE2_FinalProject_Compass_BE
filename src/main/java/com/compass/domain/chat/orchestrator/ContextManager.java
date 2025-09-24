package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import com.compass.domain.chat.orchestrator.cache.ContextCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextManager {

    private final ContextCache contextCache;

    public TravelContext getOrCreateContext(String threadId, String userId) {
        var existingOpt = contextCache.get(threadId);

        if (existingOpt.isPresent()) {
            var existing = existingOpt.get();

            // [수정] NullPointerException 방어 및 "주인 없는 컨텍스트" 처리 로직
            if (existing.getUserId() == null && userId != null) {
                log.warn("기존 컨텍스트에 userId가 없어 현재 요청의 userId({})로 업데이트합니다.", userId);
                existing.setUserId(userId);
                contextCache.put(threadId, existing);
            } else if (existing.getUserId() != null && !existing.getUserId().equals(userId)) {
                log.error("권한 없는 접근 시도: threadId={}, requestUserId={}, ownerUserId={}",
                        threadId, userId, existing.getUserId());
                throw new SecurityException("다른 사용자의 대화 스레드에 접근할 수 없습니다.");
            }

            return existing;
        }

        var newContext = createNewContext(threadId, userId);
        contextCache.put(threadId, newContext);
        return newContext;
    }

    public TravelContext getOrCreateContext(ChatRequest request) {
        return getOrCreateContext(request.getThreadId(), request.getUserId());
    }

    // ... (나머지 코드는 변경 없음) ...
    public Optional<TravelContext> getContext(String threadId, String userId) {
        return contextCache.get(threadId)
                .filter(context -> context.getUserId() != null && context.getUserId().equals(userId));
    }

    public void updateContext(TravelContext context, String userId) {
        if (!isValidContext(context)) {
            log.warn("유효하지 않은 컨텍스트 업데이트 시도");
            return;
        }

        var existingOpt = contextCache.get(context.getThreadId());
        if (existingOpt.isPresent()) {
            var existingContext = existingOpt.get();
            if (existingContext.getUserId() != null && !existingContext.getUserId().equals(userId)) {
                log.error("권한 없는 업데이트 시도: threadId={}, requestUserId={}, ownerUserId={}",
                        context.getThreadId(), userId, existingOpt.get().getUserId());
                throw new SecurityException("다른 사용자의 컨텍스트를 수정할 수 없습니다.");
            }
        }

        contextCache.put(context.getThreadId(), context);
    }

    public void resetContext(String threadId, String userIdOrEmail) {
        if (threadId == null) return;
        var newContext = createNewContext(threadId, userIdOrEmail);
        contextCache.put(threadId, newContext);
        log.info("✅ 컨텍스트 초기화 완료: threadId={}, userId={}", threadId, userIdOrEmail);
    }

    private TravelContext createNewContext(String threadId, String userId) {
        return TravelContext.builder()
                .threadId(threadId)
                .userId(userId)
                .currentPhase(TravelPhase.INITIALIZATION.name())
                .build();
    }

    private boolean isValidContext(TravelContext context) {
        return context != null && context.getThreadId() != null && context.getUserId() != null;
    }
}