package com.compass.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrichmentOrchestrationService {

    private final GooglePlacesEnrichmentService googlePlacesService;
    private final KakaoMapEnrichmentService kakaoMapService;
    private final TourApiEnrichmentService tourApiService;
    private final AiEnrichmentService aiService;

    // 진행 상태 추적
    private final Map<String, Object> currentStatus = new HashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger currentPhase = new AtomicInteger(0);

    // 전체 보강 프로세스 실행
    @Async
    public CompletableFuture<Map<String, Object>> executeFullEnrichment() {
        if (isRunning.get()) {
            log.warn("보강 프로세스가 이미 실행 중입니다");
            return CompletableFuture.completedFuture(Map.of(
                "status", "already_running",
                "message", "보강 프로세스가 이미 실행 중입니다"
            ));
        }

        isRunning.set(true);
        currentPhase.set(0);
        totalProcessed.set(0);

        Map<String, Object> report = new HashMap<>();
        report.put("startTime", LocalDateTime.now());

        try {
            // Phase 1: Google Places (필수 정보)
            currentPhase.set(1);
            updateStatus("Google Places API 보강 중", 1, 4);
            log.info("Phase 1: Google Places API 보강 시작");

            int googleEnriched = googlePlacesService.enrichAllWithGooglePlaces();
            report.put("googleEnriched", googleEnriched);
            totalProcessed.addAndGet(googleEnriched);
            log.info("Google Places 보강 완료: {} 개", googleEnriched);

            // Phase 2: 카카오맵 (한국 주소 정확도)
            currentPhase.set(2);
            updateStatus("카카오맵 API 보강 중", 2, 4);
            log.info("Phase 2: 카카오맵 API 보강 시작");

            int kakaoEnriched = kakaoMapService.enrichAllWithKakaoMap();
            report.put("kakaoEnriched", kakaoEnriched);
            totalProcessed.addAndGet(kakaoEnriched);
            log.info("카카오맵 보강 완료: {} 개", kakaoEnriched);

            // Phase 3: Tour API (편의시설)
            currentPhase.set(3);
            updateStatus("Tour API 보강 중", 3, 4);
            log.info("Phase 3: Tour API 보강 시작");

            int tourEnriched = tourApiService.enrichTouristAttractions();
            report.put("tourEnriched", tourEnriched);
            totalProcessed.addAndGet(tourEnriched);
            log.info("Tour API 보강 완료: {} 개", tourEnriched);

            // Phase 4: AI (설명 및 추천) - 선별적
            currentPhase.set(4);
            updateStatus("AI 보강 중", 4, 4);
            log.info("Phase 4: AI 보강 시작 (상위 100개)");

            int aiEnriched = aiService.enrichTopPlacesWithAI(100);
            report.put("aiEnriched", aiEnriched);
            totalProcessed.addAndGet(aiEnriched);
            log.info("AI 보강 완료: {} 개", aiEnriched);

            report.put("endTime", LocalDateTime.now());
            report.put("totalProcessed", totalProcessed.get());
            report.put("status", "completed");
            report.put("message", "전체 보강 프로세스 완료");

            updateStatus("보강 완료", 4, 4);

        } catch (Exception e) {
            log.error("보강 프로세스 실패", e);
            report.put("status", "failed");
            report.put("error", e.getMessage());
            report.put("message", "보강 프로세스 중 오류 발생");
            updateStatus("오류 발생", currentPhase.get(), 4);

        } finally {
            isRunning.set(false);
            currentPhase.set(0);
        }

        return CompletableFuture.completedFuture(report);
    }

    // 선택적 API 보강
    @Async
    public CompletableFuture<Map<String, Object>> executeSelectiveEnrichment(String apiType) {
        if (isRunning.get()) {
            return CompletableFuture.completedFuture(Map.of(
                "status", "already_running",
                "message", "다른 보강 프로세스가 실행 중입니다"
            ));
        }

        isRunning.set(true);
        Map<String, Object> result = new HashMap<>();

        try {
            int enriched = 0;

            switch (apiType.toLowerCase()) {
                case "google":
                    updateStatus("Google Places 보강 중", 1, 1);
                    enriched = googlePlacesService.enrichAllWithGooglePlaces();
                    result.put("googleEnriched", enriched);
                    break;

                case "kakao":
                    updateStatus("카카오맵 보강 중", 1, 1);
                    enriched = kakaoMapService.enrichAllWithKakaoMap();
                    result.put("kakaoEnriched", enriched);
                    break;

                case "tour":
                    updateStatus("Tour API 보강 중", 1, 1);
                    enriched = tourApiService.enrichAllWithTourApi();
                    result.put("tourEnriched", enriched);
                    break;

                case "ai":
                    updateStatus("AI 보강 중", 1, 1);
                    enriched = aiService.enrichTopPlacesWithAI(50);
                    result.put("aiEnriched", enriched);
                    break;

                default:
                    result.put("status", "invalid_api");
                    result.put("message", "유효하지 않은 API 타입");
                    return CompletableFuture.completedFuture(result);
            }

            result.put("status", "completed");
            result.put("totalEnriched", enriched);
            result.put("message", apiType + " API 보강 완료");

        } catch (Exception e) {
            log.error("선택적 보강 실패: {}", apiType, e);
            result.put("status", "failed");
            result.put("error", e.getMessage());

        } finally {
            isRunning.set(false);
            updateStatus("대기 중", 0, 0);
        }

        return CompletableFuture.completedFuture(result);
    }

    // 점진적 보강 (스케줄링)
    @Scheduled(cron = "0 0 */6 * * *") // 6시간마다
    public void incrementalEnrichment() {
        if (isRunning.get()) {
            log.info("보강 프로세스가 이미 실행 중이므로 건너뜁니다");
            return;
        }

        log.info("점진적 보강 시작");

        try {
            // 페이지 단위로 처리 (한 번에 500개씩)
            googlePlacesService.enrichByPage(0, 500);
            Thread.sleep(60000); // 1분 대기

            kakaoMapService.enrichByRegion("서울");
            Thread.sleep(60000);

            tourApiService.enrichTouristAttractions();
            Thread.sleep(60000);

            aiService.enrichTopPlacesWithAI(10);

            log.info("점진적 보강 완료");

        } catch (Exception e) {
            log.error("점진적 보강 실패", e);
        }
    }

    // 상태 업데이트
    private void updateStatus(String message, int current, int total) {
        currentStatus.put("message", message);
        currentStatus.put("currentPhase", current);
        currentStatus.put("totalPhases", total);
        currentStatus.put("isRunning", isRunning.get());
        currentStatus.put("totalProcessed", totalProcessed.get());
        currentStatus.put("timestamp", LocalDateTime.now());

        log.info("상태 업데이트: {} ({}/{})", message, current, total);
    }

    // 현재 상태 조회
    public Map<String, Object> getCurrentStatus() {
        Map<String, Object> status = new HashMap<>(currentStatus);

        // 각 API별 통계 추가
        status.put("googleStats", googlePlacesService.getEnrichmentStatistics());
        status.put("kakaoStats", kakaoMapService.getKakaoEnrichmentStatistics());
        status.put("tourStats", tourApiService.getTourApiStatistics());
        status.put("aiStats", aiService.getAIEnrichmentStatistics());

        return status;
    }

    // 보강 프로세스 중지
    public void stopEnrichment() {
        if (isRunning.get()) {
            log.info("보강 프로세스 중지 요청");
            isRunning.set(false);
            updateStatus("중지됨", currentPhase.get(), 4);
        }
    }

    // 보강 가능 여부 확인
    public boolean canStartEnrichment() {
        return !isRunning.get();
    }
}