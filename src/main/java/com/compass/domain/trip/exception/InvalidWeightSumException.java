package com.compass.domain.trip.exception;

import java.math.BigDecimal;

/**
 * 가중치 합계 오류 예외
 */
public class InvalidWeightSumException extends RuntimeException {

    private final BigDecimal actualSum;
    private final BigDecimal expectedSum;

    public InvalidWeightSumException(BigDecimal actualSum, BigDecimal expectedSum) {
        super(String.format("가중치 합계가 %s이 아닙니다. 현재 합계: %s", expectedSum, actualSum));
        this.actualSum = actualSum;
        this.expectedSum = expectedSum;
    }

    public InvalidWeightSumException(BigDecimal actualSum) {
        this(actualSum, BigDecimal.ONE);
    }

    public BigDecimal getActualSum() {
        return actualSum;
    }

    public BigDecimal getExpectedSum() {
        return expectedSum;
    }
}
