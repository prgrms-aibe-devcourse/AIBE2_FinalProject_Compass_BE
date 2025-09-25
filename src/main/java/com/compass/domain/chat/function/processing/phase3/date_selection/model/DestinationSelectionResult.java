package com.compass.domain.chat.function.processing.phase3.date_selection.model;

import java.time.LocalDateTime;
import java.util.List;

// 여행지 선별에서 수집한 장소 데이터
public record DestinationSelectionResult(
    String id,
    String threadId,
    List<TourPlace> places,  // 수집된 모든 장소 (200+개)
    LocalDateTime createdAt,
    String status  // "COMPLETED", "IN_PROGRESS"
) {
    // Builder 패턴
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String threadId;
        private List<TourPlace> places;
        private LocalDateTime createdAt;
        private String status;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder threadId(String threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder places(List<TourPlace> places) {
            this.places = places;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public DestinationSelectionResult build() {
            return new DestinationSelectionResult(id, threadId, places, createdAt, status);
        }
    }
}