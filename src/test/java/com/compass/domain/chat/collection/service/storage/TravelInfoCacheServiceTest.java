package com.compass.domain.chat.collection.service.storage;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelInfoCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private TravelInfoCacheService cacheService;
    private final String threadId = "thread-xyz-789";
    private final String expectedKey = "travel_info:" + threadId;

    @BeforeEach
    void setUp() {
        // Mockito의 when().thenReturn()을 사용하여 ValueOperations를 반환하도록 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new TravelInfoCacheService(redisTemplate);
    }

    @Test
    @DisplayName("save - 정보를 Redis에 30분 TTL로 저장한다")
    void save_shouldStoreInfoInRedis_with30MinTTL() {
        // given
        var info = new TravelFormSubmitRequest("user-1", List.of("파리"), null, null, null, null, null, null, null, null);
        Duration expectedTtl = Duration.ofMinutes(30);

        // when
        cacheService.save(threadId, info);

        // then
        // valueOperations.set이 정확한 Key, Value, TTL과 함께 호출되었는지 검증
        verify(valueOperations).set(expectedKey, info, expectedTtl);
    }

    @Test
    @DisplayName("load - Redis에서 정보를 성공적으로 불러온다")
    void load_shouldRetrieveInfoFromRedis() {
        // given
        var expectedInfo = new TravelFormSubmitRequest("user-1", List.of("도쿄"), null, null, null, null, null, null, null, null);
        when(valueOperations.get(expectedKey)).thenReturn(expectedInfo);

        // when
        TravelFormSubmitRequest actualInfo = cacheService.load(threadId);

        // then
        assertThat(actualInfo).isSameAs(expectedInfo);
    }

    @Test
    @DisplayName("load - Redis에 정보가 없으면 비어있는 객체를 반환한다")
    void load_shouldReturnEmptyObject_whenInfoNotExists() {
        // given
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // when
        TravelFormSubmitRequest result = cacheService.load(threadId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.destinations()).isNull(); // 필드들이 비어있는지 확인
    }

    @Test
    @DisplayName("delete - Redis에서 정보를 삭제한다")
    void delete_shouldRemoveInfoFromRedis() {
        // when
        cacheService.delete(threadId);

        // then
        verify(redisTemplate).delete(expectedKey);
    }
}