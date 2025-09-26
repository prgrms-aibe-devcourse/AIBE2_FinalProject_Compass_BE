package com.compass.domain.chat.service.enrichment;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 보강 서비스 추상 기본 클래스
 * 공통 로직 구현
 */
@Slf4j
public abstract class AbstractEnrichmentService implements EnrichmentService {

    protected final TravelCandidateRepository travelCandidateRepository;

    protected AbstractEnrichmentService(TravelCandidateRepository travelCandidateRepository) {
        this.travelCandidateRepository = travelCandidateRepository;
    }

    @Override
    @Transactional
    public EnrichmentResult enrichAll() {
        log.info("{} 전체 데이터 보강 시작", getServiceName());

        EnrichmentResult result = EnrichmentResult.builder()
            .serviceName(getServiceName())
            .startTime(LocalDateTime.now())
            .build();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        List<TravelCandidate> candidates = travelCandidateRepository.findAll();
        result.setTotalProcessed(candidates.size());

        candidates.forEach(candidate -> {
            try {
                if (!isEligible(candidate)) {
                    skippedCount.incrementAndGet();
                    return;
                }

                boolean enriched = enrichSingle(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                    log.debug("{} 보강 성공: {}", getServiceName(), candidate.getName());
                } else {
                    failedCount.incrementAndGet();
                    result.addWarning(String.format("보강 실패: %s", candidate.getName()));
                }

                // API 제한
                Thread.sleep(getRateLimitDelay());

            } catch (Exception e) {
                failedCount.incrementAndGet();
                String errorMsg = String.format("%s 보강 실패 - %s: %s",
                    getServiceName(), candidate.getName(), e.getMessage());
                log.error(errorMsg, e);
                result.addError(errorMsg);
            }
        });

        result.setEndTime(LocalDateTime.now());
        result.setSuccessCount(successCount.get());
        result.setFailedCount(failedCount.get());
        result.setSkippedCount(skippedCount.get());
        result.setStatus(determineStatus(successCount.get(), failedCount.get(), skippedCount.get()));

        log.info("{} 보강 완료 - 성공: {}, 실패: {}, 건너뜀: {}",
            getServiceName(), successCount.get(), failedCount.get(), skippedCount.get());

        return result;
    }

    @Override
    @Transactional
    public EnrichmentResult enrichByPage(int pageNumber, int pageSize) {
        log.info("{} 페이지 {} 보강 시작 (크기: {})", getServiceName(), pageNumber, pageSize);

        EnrichmentResult result = EnrichmentResult.builder()
            .serviceName(getServiceName())
            .startTime(LocalDateTime.now())
            .build();

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<TravelCandidate> page = travelCandidateRepository.findAll(pageable);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        result.setTotalProcessed(page.getContent().size());

        page.getContent().forEach(candidate -> {
            try {
                if (!isEligible(candidate)) {
                    skippedCount.incrementAndGet();
                    return;
                }

                boolean enriched = enrichSingle(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                }

                Thread.sleep(getRateLimitDelay());

            } catch (Exception e) {
                failedCount.incrementAndGet();
                result.addError(String.format("페이지 보강 실패: %s", e.getMessage()));
            }
        });

        result.setEndTime(LocalDateTime.now());
        result.setSuccessCount(successCount.get());
        result.setFailedCount(failedCount.get());
        result.setSkippedCount(skippedCount.get());
        result.setStatus(determineStatus(successCount.get(), failedCount.get(), skippedCount.get()));

        log.info("{} 페이지 {} 보강 완료 - 성공: {}/{}",
            getServiceName(), pageNumber, successCount.get(), page.getContent().size());

        return result;
    }

    @Override
    @Transactional
    public EnrichmentResult enrichByRegion(String region) {
        log.info("{} {} 지역 보강 시작", getServiceName(), region);

        EnrichmentResult result = EnrichmentResult.builder()
            .serviceName(getServiceName())
            .startTime(LocalDateTime.now())
            .build();

        List<TravelCandidate> candidates = travelCandidateRepository.findByRegion(region);
        result.setTotalProcessed(candidates.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        candidates.forEach(candidate -> {
            try {
                if (!isEligible(candidate)) {
                    skippedCount.incrementAndGet();
                    return;
                }

                boolean enriched = enrichSingle(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                }

                Thread.sleep(getRateLimitDelay());

            } catch (Exception e) {
                failedCount.incrementAndGet();
                result.addError(String.format("지역 보강 실패: %s", e.getMessage()));
            }
        });

        result.setEndTime(LocalDateTime.now());
        result.setSuccessCount(successCount.get());
        result.setFailedCount(failedCount.get());
        result.setSkippedCount(skippedCount.get());
        result.setStatus(determineStatus(successCount.get(), failedCount.get(), skippedCount.get()));

        log.info("{} {} 지역 보강 완료: {} 개",
            getServiceName(), region, successCount.get());

        return result;
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<EnrichmentResult> enrichBatchAsync(List<Long> candidateIds) {
        log.info("{} 비동기 배치 보강 시작 - {} 개", getServiceName(), candidateIds.size());

        EnrichmentResult result = EnrichmentResult.builder()
            .serviceName(getServiceName())
            .startTime(LocalDateTime.now())
            .build();

        List<TravelCandidate> candidates = travelCandidateRepository.findAllById(candidateIds);
        result.setTotalProcessed(candidates.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        candidates.parallelStream().forEach(candidate -> {
            try {
                if (!isEligible(candidate)) {
                    skippedCount.incrementAndGet();
                    return;
                }

                boolean enriched = enrichSingle(candidate);
                if (enriched) {
                    travelCandidateRepository.save(candidate);
                    successCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                }

                Thread.sleep(getRateLimitDelay());

            } catch (Exception e) {
                failedCount.incrementAndGet();
                result.addError(String.format("배치 보강 실패: %s", e.getMessage()));
            }
        });

        result.setEndTime(LocalDateTime.now());
        result.setSuccessCount(successCount.get());
        result.setFailedCount(failedCount.get());
        result.setSkippedCount(skippedCount.get());
        result.setStatus(determineStatus(successCount.get(), failedCount.get(), skippedCount.get()));

        return CompletableFuture.completedFuture(result);
    }

    /**
     * 기본 적격성 판단 (오버라이드 가능)
     */
    @Override
    public boolean isEligible(TravelCandidate candidate) {
        return candidate != null && candidate.getName() != null && !candidate.getName().isEmpty();
    }

    /**
     * 상태 결정 헬퍼 메서드
     */
    protected EnrichmentResult.EnrichmentStatus determineStatus(int successCount, int failedCount, int skippedCount) {
        int total = successCount + failedCount + skippedCount;

        if (total == 0 || failedCount == total) {
            return EnrichmentResult.EnrichmentStatus.FAILED;
        } else if (successCount == total || (successCount + skippedCount) == total) {
            return EnrichmentResult.EnrichmentStatus.SUCCESS;
        } else {
            return EnrichmentResult.EnrichmentStatus.PARTIAL_SUCCESS;
        }
    }

    /**
     * API 호출 헬퍼 메서드
     */
    protected void rateLimitDelay() {
        try {
            Thread.sleep(getRateLimitDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Rate limit delay interrupted");
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        long total = travelCandidateRepository.count();
        return Map.of(
            "serviceName", getServiceName(),
            "totalCandidates", total,
            "priority", getPriority(),
            "rateLimitDelay", getRateLimitDelay()
        );
    }
}