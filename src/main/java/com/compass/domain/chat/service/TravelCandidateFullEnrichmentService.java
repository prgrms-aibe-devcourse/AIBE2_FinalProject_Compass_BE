package com.compass.domain.chat.service;

import com.compass.domain.chat.service.enrichment.impl.GooglePlacesEnrichmentServiceImpl;
import com.compass.domain.chat.service.enrichment.impl.KakaoMapEnrichmentServiceImpl;
import com.compass.domain.chat.service.enrichment.impl.TourApiEnrichmentServiceImpl;
import com.compass.domain.chat.service.enrichment.impl.AiEnrichmentServiceImpl;
import com.compass.domain.chat.service.enrichment.EnrichmentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Travel Candidates 전체 데이터 보강 실행 서비스
 * 이름과 주소를 제외한 모든 컬럼 채우기
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TravelCandidateFullEnrichmentService {

    private final GooglePlacesEnrichmentServiceImpl googlePlacesService;
    private final KakaoMapEnrichmentServiceImpl kakaoMapService;
    private final TourApiEnrichmentServiceImpl tourApiService;
    private final AiEnrichmentServiceImpl aiService;

    /**
     * 모든 API를 활용한 전체 컬럼 보강
     * 순차적으로 실행하여 데이터 완성도 최대화
     */
    public Map<String, Object> executeFullEnrichment() {
        log.info("========================================");
        log.info("Travel Candidates 전체 데이터 보강 시작");
        log.info("========================================");

        long startTime = System.currentTimeMillis();
        int totalEnriched = 0;

        try {
            // Phase 1: Google Places API - 핵심 정보 수집
            // 좌표, 평점, 리뷰수, 가격대, 사진, 전화번호, 웹사이트, 영업시간
            log.info("\n[Phase 1/4] Google Places API 보강 시작");
            log.info("수집 항목: 좌표, 평점, 리뷰수, 가격대, 사진URL, 전화번호, 웹사이트, 영업시간, Place ID");

            EnrichmentResult googleResult = googlePlacesService.enrichAll();
            int googleEnriched = googleResult.getSuccessCount();
            totalEnriched += googleEnriched;

            log.info("Google Places 보강 완료: {}개 데이터 업데이트", googleEnriched);
            log.info("예상 소요시간: 약 17분 (10,000개 기준)");

            // Phase 2: 카카오맵 API - 한국 특화 정보 보완
            // 정확한 한국 주소, 카테고리 정제, 전화번호 보완
            log.info("\n[Phase 2/4] 카카오맵 API 보강 시작");
            log.info("수집 항목: 카테고리 정제, 전화번호 보완, 좌표 보정");

            EnrichmentResult kakaoResult = kakaoMapService.enrichAll();
            int kakaoEnriched = kakaoResult.getSuccessCount();
            totalEnriched += kakaoEnriched;

            log.info("카카오맵 보강 완료: {}개 데이터 업데이트", kakaoEnriched);
            log.info("예상 소요시간: 약 6분 (10,000개 기준)");

            // Phase 3: Tour API - 편의시설 정보
            // 주차, 반려동물, 휠체어, 와이파이, 영업시간, 휴무일, 입장료
            log.info("\n[Phase 3/4] Tour API 보강 시작 (관광지 위주)");
            log.info("수집 항목: 주차가능, 반려동물, 휠체어접근, 와이파이, 휴무일, 입장료");

            EnrichmentResult tourResult = tourApiService.enrichAll();
            int tourEnriched = tourResult.getSuccessCount();
            totalEnriched += tourEnriched;

            log.info("Tour API 보강 완료: {}개 데이터 업데이트", tourEnriched);
            log.info("예상 소요시간: 약 10분 (관광지 3,000개 기준)");

            // Phase 4: AI 보강 - 설명 및 추천 정보 (선택적, 상위 100개)
            log.info("\n[Phase 4/4] AI 보강 시작 (상위 100개)");
            log.info("수집 항목: 추천 방문시간, 주요 특징, 팁, 근처 명소");

            int aiEnriched = aiService.enrichTopPlaces(100);
            totalEnriched += aiEnriched;

            log.info("AI 보강 완료: {}개 데이터 업데이트", aiEnriched);
            log.info("예상 소요시간: 약 33분 (100개 기준)");

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000; // 초 단위

            log.info("\n========================================");
            log.info("전체 보강 프로세스 완료");
            log.info("총 처리 시간: {}분 {}초", duration / 60, duration % 60);
            log.info("총 업데이트 수: {}개", totalEnriched);
            log.info("========================================");

            return Map.of(
                "status", "completed",
                "totalEnriched", totalEnriched,
                "googleEnriched", googleEnriched,
                "kakaoEnriched", kakaoEnriched,
                "tourEnriched", tourEnriched,
                "aiEnriched", aiEnriched,
                "durationSeconds", duration,
                "message", String.format("전체 보강 완료: %d개 데이터 업데이트 (소요시간: %d분)",
                    totalEnriched, duration / 60)
            );

        } catch (Exception e) {
            log.error("전체 보강 프로세스 중 오류 발생", e);

            return Map.of(
                "status", "error",
                "error", e.getMessage(),
                "totalEnriched", totalEnriched,
                "message", "보강 프로세스 중 오류가 발생했습니다"
            );
        }
    }

    /**
     * 비동기 전체 보강 (백그라운드 실행)
     */
    public CompletableFuture<Map<String, Object>> executeFullEnrichmentAsync() {
        return CompletableFuture.supplyAsync(this::executeFullEnrichment);
    }

    /**
     * 빠른 보강 (Google + Kakao만)
     * 약 23분 소요
     */
    public Map<String, Object> executeFastEnrichment() {
        log.info("빠른 보강 모드 시작 (Google + Kakao)");

        long startTime = System.currentTimeMillis();
        int totalEnriched = 0;

        try {
            // Google Places API
            log.info("[1/2] Google Places API 보강");
            EnrichmentResult googleResult = googlePlacesService.enrichAll();
            int googleEnriched = googleResult.getSuccessCount();
            totalEnriched += googleEnriched;

            // 카카오맵 API
            log.info("[2/2] 카카오맵 API 보강");
            EnrichmentResult kakaoResult = kakaoMapService.enrichAll();
            int kakaoEnriched = kakaoResult.getSuccessCount();
            totalEnriched += kakaoEnriched;

            long duration = (System.currentTimeMillis() - startTime) / 1000;

            return Map.of(
                "status", "completed",
                "totalEnriched", totalEnriched,
                "googleEnriched", googleEnriched,
                "kakaoEnriched", kakaoEnriched,
                "durationSeconds", duration,
                "message", String.format("빠른 보강 완료: %d개 데이터 업데이트", totalEnriched)
            );

        } catch (Exception e) {
            log.error("빠른 보강 중 오류 발생", e);
            return Map.of(
                "status", "error",
                "error", e.getMessage()
            );
        }
    }

    /**
     * 필수 정보만 보강 (Google Places만)
     * 약 17분 소요
     */
    public Map<String, Object> executeEssentialEnrichment() {
        log.info("필수 정보 보강 시작 (Google Places만)");

        long startTime = System.currentTimeMillis();

        try {
            EnrichmentResult googleResult = googlePlacesService.enrichAll();
            int googleEnriched = googleResult.getSuccessCount();
            long duration = (System.currentTimeMillis() - startTime) / 1000;

            return Map.of(
                "status", "completed",
                "totalEnriched", googleEnriched,
                "durationSeconds", duration,
                "message", String.format("필수 정보 보강 완료: %d개 데이터 업데이트", googleEnriched)
            );

        } catch (Exception e) {
            log.error("필수 정보 보강 중 오류 발생", e);
            return Map.of(
                "status", "error",
                "error", e.getMessage()
            );
        }
    }

    /**
     * 보강 상태 통계
     */
    public Map<String, Object> getEnrichmentStatistics() {
        Map<String, Object> googleStats = googlePlacesService.getStatistics();
        Map<String, Object> kakaoStats = kakaoMapService.getStatistics();
        Map<String, Object> tourStats = tourApiService.getStatistics();
        Map<String, Object> aiStats = aiService.getStatistics();

        return Map.of(
            "google", googleStats,
            "kakao", kakaoStats,
            "tour", tourStats,
            "ai", aiStats,
            "summary", Map.of(
                "message", "각 API별 보강 상태",
                "recommendation", determineRecommendation(googleStats, kakaoStats, tourStats, aiStats)
            )
        );
    }

    private String determineRecommendation(Map<String, Object> google,
                                          Map<String, Object> kakao,
                                          Map<String, Object> tour,
                                          Map<String, Object> ai) {
        // 간단한 추천 로직
        if (google.get("completionRate") != null &&
            (double) google.get("completionRate") < 50) {
            return "Google Places API 보강을 먼저 실행하세요 (필수 정보)";
        } else if (kakao.get("addressCompletionRate") != null &&
                  (double) kakao.get("addressCompletionRate") < 80) {
            return "카카오맵 API로 주소 정보를 보완하세요";
        } else if (ai.get("aiEnrichmentRate") != null &&
                  (double) ai.get("aiEnrichmentRate") < 10) {
            return "상위 장소에 AI 보강을 추가하여 품질을 높이세요";
        } else {
            return "데이터 보강이 양호합니다";
        }
    }
}