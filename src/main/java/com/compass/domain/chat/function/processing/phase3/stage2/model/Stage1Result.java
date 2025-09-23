package com.compass.domain.chat.function.processing.phase3.stage2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stage1에서 수집한 장소 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stage1Result {
    private String id;
    private String threadId;
    private List<TourPlace> places;  // 수집된 모든 장소 (200+개)
    private LocalDateTime createdAt;
    private String status;  // "COMPLETED", "IN_PROGRESS"
}