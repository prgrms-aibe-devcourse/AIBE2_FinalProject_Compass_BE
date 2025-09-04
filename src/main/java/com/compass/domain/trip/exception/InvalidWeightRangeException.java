package com.compass.domain.trip.exception;

import java.math.BigDecimal;
import java.util.List;

/**
 * 가중치 범위 오류 예외
 */
public class InvalidWeightRangeException extends RuntimeException {

    private final List<InvalidWeight> invalidWeights;

    public InvalidWeightRangeException(List<InvalidWeight> invalidWeights) {
        super("가중치는 0.0 ~ 1.0 범위 내여야 합니다.");
        this.invalidWeights = invalidWeights;
    }

    public List<InvalidWeight> getInvalidWeights() {
        return invalidWeights;
    }

    /**
     * 유효하지 않은 가중치 정보
     */
    public static class InvalidWeight {
        private final String travelStyle;
        private final BigDecimal weight;
        private final String error;

        public InvalidWeight(String travelStyle, BigDecimal weight, String error) {
            this.travelStyle = travelStyle;
            this.weight = weight;
            this.error = error;
        }

        public String getTravelStyle() {
            return travelStyle;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public String getError() {
            return error;
        }
    }
}
