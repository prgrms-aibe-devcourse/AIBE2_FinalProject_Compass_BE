package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.model.request.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 컨텍스트 관리자 테스트
class ContextManagerTest {

    private ContextManager contextManager;

    @BeforeEach
    void setUp() {
        contextManager = new ContextManager();
    }

    @Test
    @DisplayName("새 컨텍스트 생성")
    void testCreateNewContext() {
        // given
        String threadId = "thread-1";
        String userId = "user-1";

        // when
        var context = contextManager.getOrCreateContext(threadId, userId);

        // then
        assertThat(context).isNotNull();
        assertThat(context.getThreadId()).isEqualTo(threadId);
        assertThat(context.getUserId()).isEqualTo(userId);
        assertThat(context.getCurrentPhase()).isEqualTo(TravelPhase.INITIALIZATION.name());
    }

    @Test
    @DisplayName("기존 컨텍스트 조회")
    void testGetExistingContext() {
        // given
        String threadId = "thread-1";
        String userId = "user-1";
        var firstContext = contextManager.getOrCreateContext(threadId, userId);

        // when
        var secondContext = contextManager.getOrCreateContext(threadId, userId);

        // then
        assertThat(secondContext).isSameAs(firstContext);
    }

    @Test
    @DisplayName("ChatRequest로 컨텍스트 생성")
    void testCreateContextFromChatRequest() {
        // given
        var request = new ChatRequest();
        request.setThreadId("thread-1");
        request.setUserId("user-1");
        request.setMessage("테스트 메시지");

        // when
        var context = contextManager.getOrCreateContext(request);

        // then
        assertThat(context).isNotNull();
        assertThat(context.getThreadId()).isEqualTo("thread-1");
        assertThat(context.getUserId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("컨텍스트 업데이트")
    void testUpdateContext() {
        // given
        String threadId = "thread-1";
        String userId = "user-1";
        var context = contextManager.getOrCreateContext(threadId, userId);

        // when
        context.setCurrentPhase(TravelPhase.INFORMATION_COLLECTION.name());
        contextManager.updateContext(context);
        var updatedContext = contextManager.getContext(threadId).orElse(null);

        // then
        assertThat(updatedContext).isNotNull();
        assertThat(updatedContext.getCurrentPhase()).isEqualTo(TravelPhase.INFORMATION_COLLECTION.name());
    }

    @Test
    @DisplayName("컨텍스트 초기화")
    void testResetContext() {
        // given
        String threadId = "thread-1";
        String userId = "user-1";
        contextManager.getOrCreateContext(threadId, userId);

        // when
        contextManager.resetContext(threadId);
        var context = contextManager.getContext(threadId).orElse(null);

        // then
        assertThat(context).isNull();
    }

    @Test
    @DisplayName("null 컨텍스트 업데이트 처리")
    void testUpdateNullContext() {
        // when & then
        // 예외가 발생하지 않아야 함
        contextManager.updateContext(null);
    }

    @Test
    @DisplayName("null threadId로 초기화 시도")
    void testResetWithNullThreadId() {
        // when & then
        // 예외가 발생하지 않아야 함
        contextManager.resetContext(null);
    }

    @Test
    @DisplayName("컨텍스트 존재 확인")
    void testHasContext() {
        // given
        String threadId = "thread-1";
        String userId = "user-1";

        // when
        boolean beforeCreate = contextManager.hasContext(threadId);
        contextManager.getOrCreateContext(threadId, userId);
        boolean afterCreate = contextManager.hasContext(threadId);

        // then
        assertThat(beforeCreate).isFalse();
        assertThat(afterCreate).isTrue();
    }

    @Test
    @DisplayName("전체 컨텍스트 개수 조회")
    void testGetContextCount() {
        // given
        contextManager.getOrCreateContext("thread-1", "user-1");
        contextManager.getOrCreateContext("thread-2", "user-2");
        contextManager.getOrCreateContext("thread-3", "user-3");

        // when
        int count = contextManager.getContextCount();

        // then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("전체 컨텍스트 초기화")
    void testClearAll() {
        // given
        contextManager.getOrCreateContext("thread-1", "user-1");
        contextManager.getOrCreateContext("thread-2", "user-2");

        // when
        // clearAll() 메서드가 제거되었으므로 개별적으로 초기화
        contextManager.resetContext("thread-1");
        contextManager.resetContext("thread-2");
        int count = contextManager.getContextCount();

        // then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 스레드에서 동시 접근")
    void testConcurrentAccess() throws InterruptedException {
        // given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String threadId = "thread-" + index;
                String userId = "user-" + index;
                contextManager.getOrCreateContext(threadId, userId);
            });
            threads[i].start();
        }

        // 모든 스레드 종료 대기
        for (Thread thread : threads) {
            thread.join();
        }

        // then
        assertThat(contextManager.getContextCount()).isEqualTo(threadCount);
    }
}