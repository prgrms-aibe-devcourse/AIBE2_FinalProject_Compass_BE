package com.compass.domain.chat.followup.service;

import com.compass.domain.chat.collection.dto.TravelInfo;
import com.compass.domain.chat.followup.dto.FollowUpQuestion;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 누락된 필수 정보를 찾아 후속 질문을 생성하는 전략 구현체
// 요구사항: 날짜 > 목적지 > 예산 > 동행자 순서로 우선순위를 적용하여 질문을 생성합니다.
@Component("missingFieldStrategy")
public class MissingFieldStrategy implements FollowUpStrategy {

    // 주어진 여행 정보를 바탕으로 누락된 필드를 찾아 후속 질문을 생성합니다.
    @Override
    public Optional<FollowUpQuestion> generate(TravelInfo travelInfo) {
        // travelInfo가 null인 경우, 가장 우선순위가 높은 날짜 질문부터 시작합니다.
        if (travelInfo == null) {
            return Optional.of(new FollowUpQuestion(
                    "DATE_RANGE",
                    "언제 여행을 떠나고 싶으신가요? 날짜를 알려주세요."
            ));
        }

        // 1. 날짜 정보 확인 (우선순위 1)
        if (travelInfo.travelDates() == null) {
            return Optional.of(new FollowUpQuestion(
                    "DATE_RANGE",
                    "아직 여행 날짜를 알려주지 않으셨네요. 언제 출발하실 예정인가요?"
            ));
        }

        // 2. 목적지 정보 확인 (우선순위 2)
        if (travelInfo.destinations() == null || travelInfo.destinations().isEmpty()) {
            return Optional.of(new FollowUpQuestion(
                    "DESTINATION",
                    "어디로 떠나고 싶으신가요? 여행 목적지를 알려주세요."
            ));
        }

        // 3. 예산 정보 확인 (우선순위 3)
        if (travelInfo.budget() == null) {
            return Optional.of(new FollowUpQuestion(
                    "BUDGET",
                    "이번 여행의 예산은 어느 정도로 생각하고 계신가요?"
            ));
        }

        // 4. 동행자 정보 확인 (우선순위 4)
        if (travelInfo.companions() == null || travelInfo.companions().isBlank()) {
            return Optional.of(new FollowUpQuestion(
                    "COMPANIONS",
                    "이번 여행은 누구와 함께 가시나요?"
            ));
        }

        // 모든 필수 정보가 채워져 있으면 빈 Optional을 반환합니다.
        return Optional.empty();
    }
}
