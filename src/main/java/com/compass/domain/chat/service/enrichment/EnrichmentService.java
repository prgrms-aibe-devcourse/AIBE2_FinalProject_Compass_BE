package com.compass.domain.chat.service.enrichment;

import com.compass.domain.chat.entity.TravelCandidate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 데이터 보강 서비스 공통 인터페이스
 */
public interface EnrichmentService {

    /**
     * 서비스 이름
     */
    String getServiceName();

    /**
     * 전체 데이터 보강
     */
    EnrichmentResult enrichAll();

    /**
     * 페이지 단위 보강
     */
    EnrichmentResult enrichByPage(int pageNumber, int pageSize);

    /**
     * 특정 지역 보강
     */
    EnrichmentResult enrichByRegion(String region);

    /**
     * 배치 비동기 보강
     */
    CompletableFuture<EnrichmentResult> enrichBatchAsync(List<Long> candidateIds);

    /**
     * 단일 엔티티 보강
     */
    boolean enrichSingle(TravelCandidate candidate);

    /**
     * 보강 가능 여부 판단
     */
    boolean isEligible(TravelCandidate candidate);

    /**
     * API 제한 속도 (milliseconds)
     */
    default int getRateLimitDelay() {
        return 100;
    }

    /**
     * 통계 정보 조회
     */
    Map<String, Object> getStatistics();

    /**
     * 우선순위 (낮을수록 먼저 실행)
     */
    default int getPriority() {
        return 50;
    }
}