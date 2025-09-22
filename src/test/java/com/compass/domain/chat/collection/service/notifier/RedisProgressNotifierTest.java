package com.compass.domain.chat.collection.service.notifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisProgressNotifierTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private RedisProgressNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new RedisProgressNotifier(redisTemplate);
    }

    @Test
    @DisplayName("notify - 유효한 threadId와 진행률이 주어지면, Redis에 정확한 채널과 메시지를 발행한다")
    void notify_shouldPublishToCorrectChannel_withValidInputs() {
        // given
        String threadId = "thread-abc-123";
        int progress = 75;
        String expectedChannel = "progress:notifications:" + threadId;
        String expectedMessage = String.valueOf(progress);

        // when
        notifier.notify(threadId, progress);

        // then
        // redisTemplate.convertAndSend가 정확한 인자와 함께 1번 호출되었는지 검증
        verify(redisTemplate, times(1)).convertAndSend(expectedChannel, expectedMessage);
    }

    @Test
    @DisplayName("notify - threadId가 null이거나 비어있으면, Redis 발행을 시도하지 않는다")
    void notify_shouldNotPublish_whenThreadIdIsInvalid() {
        // when
        notifier.notify(null, 50);
        notifier.notify("   ", 50);

        // then
        // redisTemplate.convertAndSend가 한 번도 호출되지 않았는지 검증
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("notify - Redis 발행 중 예외가 발생해도, 애플리케이션이 중단되지 않는다")
    void notify_shouldNotThrowException_whenRedisFails() {
        // given
        String threadId = "thread-fail-456";
        int progress = 30;
        String channel = "progress:notifications:" + threadId;

        // Mocking: Redis 발행 시 예외를 던지도록 설정
        doThrow(new RuntimeException("Redis connection failed"))
                .when(redisTemplate).convertAndSend(channel, String.valueOf(progress));

        // when & then: 예외가 발생하지 않아야 함 (내부에서 처리)
        notifier.notify(threadId, progress);

        // verify: 예외가 발생했음에도 불구하고 호출 시도는 있었는지 확인
        verify(redisTemplate, times(1)).convertAndSend(channel, String.valueOf(progress));
    }
}