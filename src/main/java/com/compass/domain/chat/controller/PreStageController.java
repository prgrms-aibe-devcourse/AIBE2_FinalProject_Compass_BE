package com.compass.domain.chat.controller;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.service.PreStageGooglePlacesService;
import com.compass.domain.chat.service.CSVImportService;
import com.compass.domain.chat.service.TravelCandidateAddressEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Pre-Stage 데이터 수집 컨트롤러 (관리자용)
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/pre-stage")
@RequiredArgsConstructor
public class PreStageController {

    private final PreStageGooglePlacesService preStageGooglePlacesService;
    private final CSVImportService csvImportService;
    private final TravelCandidateAddressEnrichmentService addressEnrichmentService;

    // 특정 도시의 여행지 데이터 수집
    @PostMapping("/collect/{city}")
    public ResponseEntity<Map<String, Object>> collectCityData(
            @PathVariable String city,
            @RequestParam(defaultValue = "false") boolean forceUpdate) {
        log.info("Pre-Stage 데이터 수집 시작: {} (강제 업데이트: {})", city, forceUpdate);

        try {
            int collectedCount;
            List<TravelCandidate> candidates;

            if (forceUpdate) {
                // 강제 업데이트 모드: 새로 수집
                collectedCount = preStageGooglePlacesService.collectAndSaveForRegion(city, true);
                candidates = preStageGooglePlacesService.getPlacesForStage1(city, null, null);
            } else {
                // 기본 모드: 기존 데이터 확인 후 필요시만 수집
                candidates = preStageGooglePlacesService.collectAndSavePlacesForCity(city);
                collectedCount = candidates.size();
            }

            Map<String, Object> response = Map.of(
                "city", city,
                "collectedCount", candidates.size(),
                "message", String.format("%s 지역 %d개 여행지 수집 완료", city, candidates.size()),
                "candidates", candidates
            );

            log.info("Pre-Stage 데이터 수집 완료: {} - {}개 수집", city, candidates.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Pre-Stage 데이터 수집 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                "city", city,
                "error", e.getMessage(),
                "message", "데이터 수집 중 오류가 발생했습니다"
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 모든 도시의 여행지 데이터 일괄 수집
    @PostMapping("/collect-all")
    public ResponseEntity<Map<String, Object>> collectAllCitiesData() {
        log.info("Pre-Stage 전체 도시 데이터 수집 시작");

        try {
            Map<String, List<TravelCandidate>> allCandidates = preStageGooglePlacesService.collectAndSaveAllCities();

            int totalCount = allCandidates.values().stream()
                .mapToInt(List::size)
                .sum();

            Map<String, Object> response = Map.of(
                "totalCities", allCandidates.size(),
                "totalCandidates", totalCount,
                "message", String.format("전체 %d개 도시, 총 %d개 여행지 수집 완료",
                    allCandidates.size(), totalCount),
                "citySummary", allCandidates.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                    ))
            );

            log.info("Pre-Stage 전체 데이터 수집 완료: {}개 도시, {}개 여행지",
                allCandidates.size(), totalCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Pre-Stage 전체 데이터 수집 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                "error", e.getMessage(),
                "message", "전체 데이터 수집 중 오류가 발생했습니다"
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // CSV 파일 임포트
    @PostMapping("/import-csv")
    public ResponseEntity<Map<String, Object>> importCSVFiles() {
        log.info("CSV 파일 임포트 요청");

        try {
            int importedCount = csvImportService.importAllCSVFiles();

            Map<String, Object> response = Map.of(
                "totalImported", importedCount,
                "message", String.format("CSV 임포트 완료: 총 %d개 데이터 저장", importedCount),
                "status", "success"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("CSV 임포트 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                "error", e.getMessage(),
                "message", "CSV 임포트 중 오류가 발생했습니다",
                "status", "error"
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 특정 CSV 파일 임포트
    @PostMapping("/import-csv/{fileName}")
    public ResponseEntity<Map<String, Object>> importSingleCSVFile(@PathVariable String fileName) {
        log.info("특정 CSV 파일 임포트 요청: {}", fileName);

        try {
            String filePath = "/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE/list/" + fileName;
            int importedCount = csvImportService.importCSVFile(filePath);

            Map<String, Object> response = Map.of(
                "fileName", fileName,
                "importedCount", importedCount,
                "message", String.format("%s 파일 임포트 완료: %d개 데이터 저장", fileName, importedCount),
                "status", "success"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("CSV 파일 {} 임포트 실패: {}", fileName, e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                "fileName", fileName,
                "error", e.getMessage(),
                "message", "CSV 파일 임포트 중 오류가 발생했습니다",
                "status", "error"
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 모든 travel_candidates 데이터의 주소 정보 업데이트
    @PostMapping("/enrich-addresses")
    public ResponseEntity<Map<String, Object>> enrichAllAddresses() {
        log.info("전체 travel_candidates 주소 정보 업데이트 시작");

        try {
            int updatedCount = addressEnrichmentService.enrichAllCandidatesWithAddress();

            Map<String, Object> statistics = addressEnrichmentService.getEnrichmentStatistics();

            Map<String, Object> response = Map.of(
                "updatedCount", updatedCount,
                "statistics", statistics,
                "message", String.format("%d개 여행지 주소 정보 업데이트 완료", updatedCount),
                "status", "success"
            );

            log.info("주소 정보 업데이트 완료: {}개", updatedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("주소 정보 업데이트 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                "error", e.getMessage(),
                "message", "주소 정보 업데이트 중 오류가 발생했습니다",
                "status", "error"
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 특정 지역의 travel_candidates 주소 정보 업데이트
    @PostMapping("/enrich-addresses/{region}")
    public ResponseEntity<Map<String, Object>> enrichRegionAddresses(@PathVariable String region) {
        log.info("{} 지역 travel_candidates 주소 정보 업데이트 시작", region);

        try {
            int updatedCount = addressEnrichmentService.enrichCandidatesAddressByRegion(region);

            Map<String, Object> response = Map.of(
                "region", region,
                "updatedCount", updatedCount,
                "message", String.format("%s 지역 %d개 여행지 주소 정보 업데이트 완료", region, updatedCount),
                "status", "success"
            );

            log.info("{} 지역 주소 정보 업데이트 완료: {}개", region, updatedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("{} 지역 주소 정보 업데이트 실패: {}", region, e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                "region", region,
                "error", e.getMessage(),
                "message", "주소 정보 업데이트 중 오류가 발생했습니다",
                "status", "error"
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // 주소 정보 업데이트 통계 조회
    @GetMapping("/address-statistics")
    public ResponseEntity<Map<String, Object>> getAddressStatistics() {
        try {
            Map<String, Object> statistics = addressEnrichmentService.getEnrichmentStatistics();

            Map<String, Object> response = Map.of(
                "statistics", statistics,
                "message", "주소 정보 통계 조회 성공",
                "status", "success"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("주소 정보 통계 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = Map.of(
                "error", e.getMessage(),
                "message", "통계 조회 중 오류가 발생했습니다",
                "status", "error"
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}