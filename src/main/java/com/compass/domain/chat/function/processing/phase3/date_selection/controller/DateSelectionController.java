package com.compass.domain.chat.function.processing.phase3.date_selection.controller;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.DateSelectionOutput;
import com.compass.domain.chat.function.processing.phase3.date_selection.service.DateSelectionDistributor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Phase3 날짜별선별 테스트용 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/phase3/date-selection")
@RequiredArgsConstructor
public class DateSelectionController {

    private final DateSelectionDistributor dateSelectionDistributor;

    // 날짜별 선별 일정 분산 처리
    @PostMapping("/distribute")
    public ResponseEntity<DateSelectionOutput> distributeDaily(
            @RequestParam String threadId,
            @RequestParam(defaultValue = "3") int tripDays) {

        MDC.put("threadId", threadId);
        MDC.put("tripDays", String.valueOf(tripDays));

        try {
            log.info("날짜별 선별 일정 분산 요청");
            var output = dateSelectionDistributor.processDistribution(threadId, tripDays);
            log.info("날짜별 선별 처리 완료: {}개 일정", output.dailyItineraries().size());
            return ResponseEntity.ok(output);

        } catch (Exception e) {
            log.error("날짜별 선별 처리 실패", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    // Mock 데이터로 테스트
    @GetMapping("/test")
    public ResponseEntity<DateSelectionOutput> testWithMockData(
            @RequestParam(defaultValue = "3") int tripDays) {

        var testThreadId = "test-" + System.currentTimeMillis();
        MDC.put("threadId", testThreadId);
        MDC.put("tripDays", String.valueOf(tripDays));

        try {
            log.info("Mock 데이터 테스트");
            var output = dateSelectionDistributor.processDistribution(testThreadId, tripDays);
            return ResponseEntity.ok(output);

        } catch (Exception e) {
            log.error("테스트 실패", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }
}