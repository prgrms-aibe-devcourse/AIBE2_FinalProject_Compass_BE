package com.compass.domain.intent;

/**
 * 의도 분류 결과를 담는 레코드
 * @param intent 분류된 의도
 * @param confidenceScore 신뢰도 점수 (0.0 ~ 1.0)
 */
public record IntentClassification(
        Intent intent,
        double confidenceScore
) {
    // 신뢰도 점수가 임계값 이상인지 확인하는 헬퍼 메서드
    public boolean isConfident(double threshold) {
        return this.confidenceScore >= threshold;
    }
}
