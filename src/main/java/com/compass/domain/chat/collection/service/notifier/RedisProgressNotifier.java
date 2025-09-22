package com.compass.domain.chat.collection.service.notifier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

// Redis Pub/Sub을 사용한 실시간 진행률 알림 시스템 구현체
@Slf4j
@Component("redisProgressNotifier")
@RequiredArgsConstructor
public class RedisProgressNotifier implements ProgressNotifier {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis 채널 이름을 정의. "progress:notifications:{threadId}" 형식
    private String getChannelName(String threadId) {
        return "progress:notifications:" + threadId;
    }

    @Override
    public void notify(String threadId, int progress) {
        if (threadId == null || threadId.isBlank()) {
            log.warn("Thread ID가 없어 진행률 알림을 보낼 수 없습니다.");
            return;
        }

        try {
            String channel = getChannelName(threadId);
            // progress 값을 문자열로 변환하여 해당 채널에 publish(발행)
            redisTemplate.convertAndSend(channel, String.valueOf(progress));
            log.info("채널 '{}'에 진행률 {}% 발행 완료", channel, progress);
        } catch (Exception e) {
            log.error("Redis에 진행률 알림 발행 실패: threadId={}, progress={}", threadId, progress, e);
        }
    }

    @Override
    public void subscribe(String threadId) {
        // 실제 구독 로직은 프론트엔드와 연결되는 WebSocket 또는 SSE 핸들러에서 처리
        // 로깅 용도
        log.info("Thread ID '{}'에 대한 진행률 구독이 요청되었습니다. (실제 구독은 WebSocket/SSE에서 처리)", threadId);
    }
}
