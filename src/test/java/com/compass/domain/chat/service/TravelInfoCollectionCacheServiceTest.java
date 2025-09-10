package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * REQ-FOLLOW-004: Redis 저장 | TravelContext 30분 TTL 테스트
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("TravelInfoCollectionCacheService 테스트")
class TravelInfoCollectionCacheServiceTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @InjectMocks
    private TravelInfoCollectionCacheService cacheService;
    
    private TravelInfoCollectionState testState;
    private String sessionId;
    private String cacheKey;
    
    @BeforeEach
    void setUp() {
        sessionId = "test-session-123";
        cacheKey = "travel:collection:" + sessionId;
        
        testState = TravelInfoCollectionState.builder()
                .sessionId(sessionId)
                .destination("서울")
                .destinationCollected(true)
                .durationNights(2)
                .durationCollected(true)
                .build();
        
        // TTL 설정
        ReflectionTestUtils.setField(cacheService, "cacheTtlMinutes", 30L);
    }
    
    @Test
    @DisplayName("여행 컨텍스트를 Redis에 저장하고 30분 TTL을 설정한다")
    void saveTravelContext_Success() throws Exception {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String jsonValue = "{\"sessionId\":\"test-session-123\"}";
        when(objectMapper.writeValueAsString(testState)).thenReturn(jsonValue);
        
        // when
        cacheService.saveTravelContext(sessionId, testState);
        
        // then
        verify(valueOperations).set(cacheKey, jsonValue, 30L, TimeUnit.MINUTES);
    }
    
    @Test
    @DisplayName("직렬화 실패 시 로그만 남기고 계속 진행한다")
    void saveTravelContext_SerializationFailure() throws Exception {
        // given
        when(objectMapper.writeValueAsString(testState))
                .thenThrow(new RuntimeException("Serialization error"));
        
        // when
        cacheService.saveTravelContext(sessionId, testState);
        
        // then
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    @DisplayName("Redis에서 여행 컨텍스트를 조회한다")
    void getTravelContext_Success() throws Exception {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String jsonValue = "{\"sessionId\":\"test-session-123\"}";
        when(valueOperations.get(cacheKey)).thenReturn(jsonValue);
        when(objectMapper.readValue(jsonValue, TravelInfoCollectionState.class))
                .thenReturn(testState);
        
        // when
        Optional<TravelInfoCollectionState> result = cacheService.getTravelContext(sessionId);
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSessionId()).isEqualTo(sessionId);
    }
    
    @Test
    @DisplayName("캐시에 데이터가 없으면 빈 Optional을 반환한다")
    void getTravelContext_NotFound() {
        // given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(cacheKey)).thenReturn(null);
        
        // when
        Optional<TravelInfoCollectionState> result = cacheService.getTravelContext(sessionId);
        
        // then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("TTL을 30분으로 갱신한다")
    void refreshTTL_Success() {
        // given
        when(redisTemplate.expire(cacheKey, 30L, TimeUnit.MINUTES)).thenReturn(true);
        
        // when
        cacheService.refreshTTL(sessionId);
        
        // then
        verify(redisTemplate).expire(cacheKey, 30L, TimeUnit.MINUTES);
    }
    
    @Test
    @DisplayName("Redis에서 여행 컨텍스트를 삭제한다")
    void deleteTravelContext_Success() {
        // given
        when(redisTemplate.delete(cacheKey)).thenReturn(true);
        
        // when
        cacheService.deleteTravelContext(sessionId);
        
        // then
        verify(redisTemplate).delete(cacheKey);
    }
    
    @Test
    @DisplayName("캐시 존재 여부를 확인한다")
    void exists_ReturnsTrue() {
        // given
        when(redisTemplate.hasKey(cacheKey)).thenReturn(true);
        
        // when
        boolean exists = cacheService.exists(sessionId);
        
        // then
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("Redis 장애 시 false를 반환한다")
    void exists_RedisFailure_ReturnsFalse() {
        // given
        when(redisTemplate.hasKey(cacheKey)).thenThrow(new RuntimeException("Redis error"));
        
        // when
        boolean exists = cacheService.exists(sessionId);
        
        // then
        assertThat(exists).isFalse();
    }
}