package com.compass.domain.chat.function.followup;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Optional;

// 다음에 할 Follow-up 질문을 선택하는 책임을 가진 클래스
@Component
public class FollowUpQuestionSelector {

    // 누락된 정보에 대한 다음 질문을 찾아서 반환합니다.
    // 더 이상 질문할 것이 없으면 비어있는 Optional을 반환합니다.
    public Optional<String> findNextQuestion(TravelFormSubmitRequest info) {
        // 1. 목적지 확인 (가장 중요한 정보)
        if (CollectionUtils.isEmpty(info.destinations()) || info.destinations().stream().anyMatch("목적지 미정"::equalsIgnoreCase)) {
            return Optional.of("어디로 여행을 떠나고 싶으신가요? 도시 이름을 알려주세요.");
        }

        // 2. 여행 날짜 확인
        if (info.travelDates() == null || info.travelDates().startDate() == null || info.travelDates().endDate() == null) {
            return Optional.of("여행은 언제부터 언제까지 계획하고 계신가요?");
        }

        // 3. 출발지 확인
        if (!StringUtils.hasText(info.departureLocation())) {
            return Optional.of("어디서 출발하시는지 알려주시겠어요?");
        }

        // 모든 정보가 다 있으므로 질문할 것이 없음
        return Optional.empty();
    }
}