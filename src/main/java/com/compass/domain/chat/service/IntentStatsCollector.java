package com.compass.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * REQ-INTENT-004: 의도 분류 결과 통계 수집기
 * IntentRouter의 분류 결과를 로깅하고, 의도별 빈도를 집계하여 통계를 수집합니다.
 */
@Slf4j
@Service
public class IntentStatsCollector {

    // 동시성 환경에서 안전하게 카운트를 누적하기 위해 LongAdder 사용
    private final Map<IntentRouter.Intent, LongAdder> intentCounts = new ConcurrentHashMap<>();

    /**
     * 의도 분류 결과를 기록하고 통계를 집계합니다.
     * @param userInput 원본 사용자 입력
     * @param intent 분류된 의도
     * @param source 분류 소스 ("Keyword", "LLM", "LLM_Fallback")
     */
    public void record(String userInput, IntentRouter.Intent intent, String source) {
        // 1. 분류 결과 로그 기록
        log.info("Intent Classified: [Intent: {}, Source: {}, Input: '{}']", intent, source, userInput);

        // 2. 의도별 카운트 집계
        intentCounts.computeIfAbsent(intent, k -> new LongAdder()).increment();
    }

    /**
     * 현재까지 집계된 의도별 통계를 반환합니다.
     * (향후 모니터링 대시보드 등에 활용 가능)
     * @return 의도별 집계 맵
     */
    public Map<IntentRouter.Intent, Long> getStatistics() {
        Map<IntentRouter.Intent, Long> stats = new ConcurrentHashMap<>();
        intentCounts.forEach((intent, adder) -> stats.put(intent, adder.sum()));
        return stats;
    }
    
}
