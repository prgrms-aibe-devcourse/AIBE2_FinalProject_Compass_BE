package com.compass.domain.chat.service.enrichment;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 보강 작업 결과
 */
@Data
@Builder
public class EnrichmentResult {

    private String serviceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalProcessed;
    private int successCount;
    private int failedCount;
    private int skippedCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private EnrichmentStatus status;

    public enum EnrichmentStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED,
        CANCELLED
    }

    public Duration getDuration() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }
        return Duration.ZERO;
    }

    public double getSuccessRate() {
        if (totalProcessed == 0) return 0;
        return (double) successCount / totalProcessed * 100;
    }

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
}