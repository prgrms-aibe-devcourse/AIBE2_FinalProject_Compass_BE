package com.compass.domain.completion.service;

import com.compass.domain.collection.dto.TravelInfo;
import com.compass.domain.completion.dto.CompletionStatus;
import org.springframework.stereotype.Component;

// 각 정보의 중요도에 따라 가중치를 부여하여 여행 계획의 진행률을 계산하는 클래스
// 요구사항 2.3.5: 가중치 기반 계산 및 상태별 메시지 제공
@Component
public class WeightedCompletionCalculator {

    // 정보별 가중치 상수 정의
    private static final int DESTINATION_WEIGHT = 30;
    private static final int DATES_WEIGHT = 30;
    private static final int BUDGET_WEIGHT = 20;
    private static final int COMPANIONS_WEIGHT = 10;
    private static final int STYLE_WEIGHT = 10;

    // 필수 정보(목적지, 날짜, 예산)의 가중치 합
    private static final int CORE_INFO_TOTAL_WEIGHT = DESTINATION_WEIGHT + DATES_WEIGHT + BUDGET_WEIGHT;

    // 여행 정보 완성도를 계산하고, 현재 상태에 맞는 메시지를 반환합니다.
    public CompletionStatus calculate(TravelInfo travelInfo) {
        if (travelInfo == null) {
            return new CompletionStatus(0, "여행 계획을 시작하려면 정보를 입력해주세요.");
        }

        var progress = 0;

        // 1. 목적지 (가중치 30%)
        if (travelInfo.destinations() != null && !travelInfo.destinations().isEmpty()) {
            progress += DESTINATION_WEIGHT;
        }
        // 2. 날짜 (가중치 30%)
        if (travelInfo.travelDates() != null) {
            progress += DATES_WEIGHT;
        }
        // 3. 예산 (가중치 20%)
        if (travelInfo.budget() != null) {
            progress += BUDGET_WEIGHT;
        }
        // 4. 동행자 (가중치 10%)
        if (travelInfo.companions() != null && !travelInfo.companions().isBlank()) {
            progress += COMPANIONS_WEIGHT;
        }
        // 5. 여행 스타일 (가중치 10%)
        if (travelInfo.travelStyle() != null && !travelInfo.travelStyle().isBlank()) {
            progress += STYLE_WEIGHT;
        }

        var message = createMessage(progress);

        return new CompletionStatus(progress, message);
    }

    // 진행률에 따라 적절한 상태 메시지를 생성하는 헬퍼 메서드
    private String createMessage(int progress) {
        // 핵심 정보가 모두 입력되었으나, 선택 정보가 남아있는 경우 (진행률 80%)
        if (progress == CORE_INFO_TOTAL_WEIGHT) {
            return "필수 정보가 모두 입력되었어요! 더 정확한 추천을 위해 동행자나 여행 스타일에 대해서도 알려주시겠어요?";
        }
        if (progress == 100) {
            return "모든 정보가 입력되었습니다! 이제 여행 계획을 생성할 수 있습니다.";
        }
        return String.format("여행 정보의 %d%%가 입력되었습니다.", progress);
    }
}
