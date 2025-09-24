package com.compass.domain.chat.collection.service.storage;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// Redis를 저장소로 사용하는 TravelInfoStorage 구현체
@Slf4j
@Service("travelInfoCacheService")
@RequiredArgsConstructor
public class TravelInfoCacheService implements TravelInfoStorage {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "travel_info:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private String getKey(String threadId) {
        return KEY_PREFIX + threadId;
    }

    @Override
    public void save(String threadId, TravelFormSubmitRequest info) {
        try {
            String key = getKey(threadId);
            // TravelFormSubmitRequest 객체를 Redis에 저장하고 30분의 유효기간 설정
            redisTemplate.opsForValue().set(key, info, CACHE_TTL);
            log.info("Redis에 여행 정보 저장 완료: key='{}'", key);
        } catch (Exception e) {
            log.error("Redis 여행 정보 저장 실패: key='{}'", getKey(threadId), e);
        }
    }

    @Override
    public TravelFormSubmitRequest load(String threadId) {
        try {
            String key = getKey(threadId);
            Object data = redisTemplate.opsForValue().get(key);

            if (data instanceof TravelFormSubmitRequest) {
                log.info("Redis에서 여행 정보 로드 완료: key='{}'", key);
                return (TravelFormSubmitRequest) data;
            }
        } catch (Exception e) {
            log.error("Redis 여행 정보 로드 실패: key='{}'", getKey(threadId), e);
        }
        // [수정] 생성자 인자 개수를 10개로 맞춤
        return new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public void delete(String threadId) {
        try {
            String key = getKey(threadId);
            redisTemplate.delete(key);
            log.info("Redis에서 여행 정보 삭제 완료: key='{}'", key);
        } catch (Exception e) {
            log.error("Redis 여행 정보 삭제 실패: key='{}'", getKey(threadId), e);
        }
    }
}