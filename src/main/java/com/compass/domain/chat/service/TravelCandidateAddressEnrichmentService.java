package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class TravelCandidateAddressEnrichmentService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final KakaoMapService kakaoMapService;

    // 모든 TravelCandidate의 주소 정보 업데이트 (전체 엔티티 대상)
    @Transactional
    public int enrichAllCandidatesWithAddress() {
        log.info("모든 TravelCandidate 주소 정보 업데이트 시작");

        AtomicInteger updatedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // 모든 TravelCandidate 조회 (주소 유무 상관없이)
        List<TravelCandidate> candidates = travelCandidateRepository.findAll();

        log.info("전체 TravelCandidate 수: {}", candidates.size());

        candidates.forEach(candidate -> {
            try {
                // 장소명으로 카카오맵 API 검색
                String searchedAddress = kakaoMapService.searchAddressByPlaceName(
                    candidate.getName(),
                    candidate.getRegion()
                );

                if (searchedAddress != null && !searchedAddress.isEmpty()) {
                    // 주소 업데이트 (기존 주소 덮어쓰기)
                    candidate.setAddress(searchedAddress);
                    candidate.setDetailedAddress(searchedAddress);

                    travelCandidateRepository.save(candidate);
                    updatedCount.incrementAndGet();
                    log.debug("주소 업데이트 성공: {} - {}", candidate.getName(), searchedAddress);
                } else {
                    failedCount.incrementAndGet();
                    log.debug("주소 검색 실패: {}", candidate.getName());
                }

                // API 호출 제한을 위한 딜레이
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("주소 업데이트 실패 - {}: {}", candidate.getName(), e.getMessage());
                failedCount.incrementAndGet();
            }
        });

        log.info("주소 업데이트 완료 - 성공: {}, 실패: {}", updatedCount.get(), failedCount.get());
        return updatedCount.get();
    }

    // 특정 지역의 TravelCandidate 주소 정보 업데이트 (전체 엔티티 대상)
    @Transactional
    public int enrichCandidatesAddressByRegion(String region) {
        log.info("{} 지역 TravelCandidate 주소 정보 업데이트 시작", region);

        AtomicInteger updatedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // 해당 지역의 모든 TravelCandidate 조회 (주소 유무 상관없이)
        List<TravelCandidate> candidates = travelCandidateRepository.findByRegion(region);

        log.info("{} 지역 전체 TravelCandidate 수: {}", region, candidates.size());

        candidates.forEach(candidate -> {
            try {
                // 장소명으로 카카오맵 API 검색
                String searchedAddress = kakaoMapService.searchAddressByPlaceName(
                    candidate.getName(),
                    candidate.getRegion()
                );

                if (searchedAddress != null && !searchedAddress.isEmpty()) {
                    // 주소 업데이트 (기존 주소 덮어쓰기)
                    candidate.setAddress(searchedAddress);
                    candidate.setDetailedAddress(searchedAddress);

                    travelCandidateRepository.save(candidate);
                    updatedCount.incrementAndGet();
                    log.debug("주소 업데이트 성공: {} - {}", candidate.getName(), searchedAddress);
                } else {
                    failedCount.incrementAndGet();
                    log.debug("주소 검색 실패: {}", candidate.getName());
                }

                // API 호출 제한을 위한 딜레이
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("주소 업데이트 실패 - {}: {}", candidate.getName(), e.getMessage());
                failedCount.incrementAndGet();
            }
        });

        log.info("{} 지역 주소 업데이트 완료 - 성공: {}, 실패: {}", region, updatedCount.get(), failedCount.get());
        return updatedCount.get();
    }

    // 페이지 단위로 주소 업데이트 (대량 처리용)
    @Transactional
    public int enrichCandidatesAddressByPage(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<TravelCandidate> page = travelCandidateRepository.findAll(pageable);

        AtomicInteger updatedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        // 페이지의 모든 엔티티 대상 (주소 유무 상관없이)
        page.getContent().forEach(candidate -> {
            try {
                // 장소명으로 카카오맵 API 검색
                String searchedAddress = kakaoMapService.searchAddressByPlaceName(
                    candidate.getName(),
                    candidate.getRegion()
                );

                if (searchedAddress != null && !searchedAddress.isEmpty()) {
                    // 주소 업데이트 (기존 주소 덮어쓰기)
                    candidate.setAddress(searchedAddress);
                    candidate.setDetailedAddress(searchedAddress);

                    travelCandidateRepository.save(candidate);
                    updatedCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                }

                Thread.sleep(100);

            } catch (Exception e) {
                log.error("주소 업데이트 실패: {}", e.getMessage());
                failedCount.incrementAndGet();
            }
        });

        log.info("페이지 {} 주소 업데이트 완료 - 성공: {}, 실패: {}, 전체: {}",
            pageNumber, updatedCount.get(), failedCount.get(), page.getContent().size());

        return updatedCount.get();
    }

    // 비동기 배치 처리
    @Async
    @Transactional
    public CompletableFuture<Integer> enrichCandidatesAddressAsync(List<Long> candidateIds) {
        log.info("비동기 주소 업데이트 시작 - {} 개", candidateIds.size());

        AtomicInteger updatedCount = new AtomicInteger(0);

        List<TravelCandidate> candidates = travelCandidateRepository.findAllById(candidateIds);

        candidates.forEach(candidate -> {
            try {
                boolean updated = enrichCandidateAddress(candidate);
                if (updated) {
                    travelCandidateRepository.save(candidate);
                    updatedCount.incrementAndGet();
                }

                Thread.sleep(100);

            } catch (Exception e) {
                log.error("비동기 주소 업데이트 실패: {}", e.getMessage());
            }
        });

        log.info("비동기 주소 업데이트 완료 - 성공: {}", updatedCount.get());
        return CompletableFuture.completedFuture(updatedCount.get());
    }

    // 개별 후보의 주소 정보 업데이트 (간단한 버전 - 장소명 검색만 사용)
    private boolean enrichCandidateAddress(TravelCandidate candidate) {
        try {
            // 장소명으로 카카오맵 API 검색
            String searchedAddress = kakaoMapService.searchAddressByPlaceName(
                candidate.getName(),
                candidate.getRegion()
            );

            if (searchedAddress != null && !searchedAddress.isEmpty()) {
                // 주소 업데이트 (기존 주소 덮어쓰기)
                candidate.setAddress(searchedAddress);
                candidate.setDetailedAddress(searchedAddress);
                log.debug("주소 업데이트 성공: {} -> {}", candidate.getName(), searchedAddress);
                return true;
            }

            log.warn("주소 조회 실패: {}", candidate.getName());
            return false;

        } catch (Exception e) {
            log.error("주소 업데이트 중 오류 - {}: {}", candidate.getName(), e.getMessage());
            return false;
        }
    }


    // 통계 정보 조회
    public Map<String, Object> getEnrichmentStatistics() {
        long totalCount = travelCandidateRepository.count();
        long withAddressCount = travelCandidateRepository.countByAddressIsNotNull();
        long withoutAddressCount = totalCount - withAddressCount;

        return Map.of(
            "total", totalCount,
            "withAddress", withAddressCount,
            "withoutAddress", withoutAddressCount,
            "completionRate", (double) withAddressCount / totalCount * 100
        );
    }
}