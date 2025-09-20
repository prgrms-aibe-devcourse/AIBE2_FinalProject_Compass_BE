package com.compass.domain.chat.collection.service.strategy;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// 누락된 필수 정보를 순서대로 질문하는 Follow-up 전략
@Component("missingFieldStrategy")
public class MissingFieldStrategy implements FollowUpStrategy {

    // 질문 생성 규칙을 우선순위대로 정의
    private final List<Function<TravelFormSubmitRequest, Optional<String>>> questionChecks = List.of(
            this::checkDestinations,
            this::checkTravelDates,
            this::checkDepartureLocation
            // 필요 시 예산, 동행자 등 다른 규칙 추가
    );

    @Override
    public Optional<String> findNextQuestion(TravelFormSubmitRequest info) {
        return questionChecks.stream()
                .map(check -> check.apply(info))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    // 목적지 확인 규칙
    private Optional<String> checkDestinations(TravelFormSubmitRequest info) {
        if (CollectionUtils.isEmpty(info.destinations()) || info.destinations().stream().anyMatch("목적지 미정"::equalsIgnoreCase)) {
            return Optional.of("어디로 여행을 떠나고 싶으신가요? 도시 이름을 알려주세요.");
        }
        return Optional.empty();
    }

    // 여행 날짜 확인 규칙
    private Optional<String> checkTravelDates(TravelFormSubmitRequest info) {
        if (info.travelDates() == null || info.travelDates().startDate() == null || info.travelDates().endDate() == null) {
            return Optional.of("여행은 언제부터 언제까지 계획하고 계신가요?");
        }
        return Optional.empty();
    }

    // 출발지 확인 규칙
    private Optional<String> checkDepartureLocation(TravelFormSubmitRequest info) {
        if (!StringUtils.hasText(info.departureLocation())) {
            return Optional.of("어디서 출발하시나요?");
        }
        return Optional.empty();
    }
}