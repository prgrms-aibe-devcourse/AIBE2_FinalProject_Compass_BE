package com.compass.domain.chat.function.followup;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// 다음에 할 Follow-up 질문을 선택하는 책임을 가진 클래스
@Component
public class FollowUpQuestionSelector {

    // 질문 생성 규칙을 우선순위대로 정의
    private final List<Function<TravelFormSubmitRequest, Optional<String>>> questionChecks = List.of(
            this::checkDestinations,
            this::checkTravelDates,
            this::checkDepartureLocation
    );


    public Optional<String> findNextQuestion(TravelFormSubmitRequest info) {
        // 정의된 규칙을 순서대로 실행하여 첫 번째로 발견된 질문을 반환합니다.
        return questionChecks.stream()
                .map(check -> check.apply(info))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    // 1. 목적지 확인 규칙
    private Optional<String> checkDestinations(TravelFormSubmitRequest info) {
        if (CollectionUtils.isEmpty(info.destinations()) || info.destinations().stream().anyMatch("목적지 미정"::equalsIgnoreCase)) {
            return Optional.of("어디로 여행을 떠나고 싶으신가요? 도시 이름을 알려주세요.");
        }
        return Optional.empty();
    }

    // 2. 여행 날짜 확인 규칙
    private Optional<String> checkTravelDates(TravelFormSubmitRequest info) {
        if (info.travelDates() == null || info.travelDates().startDate() == null || info.travelDates().endDate() == null) {
            return Optional.of("여행은 언제부터 언제까지 계획하고 계신가요?");
        }
        return Optional.empty();
    }

    // 3. 출발지 확인 규칙
    private Optional<String> checkDepartureLocation(TravelFormSubmitRequest info) {
        if (!StringUtils.hasText(info.departureLocation())) {
            return Optional.of("어디서 출발하시는지 알려주시겠어요?");
        }
        return Optional.empty();
    }
}