package com.compass.domain.chat.followup.service;

import com.compass.domain.chat.collection.dto.TravelInfo;
import com.compass.domain.chat.followup.dto.FollowUpQuestion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

// 사용자의 입력이 모호할 경우, 구체화를 위한 질문을 생성하는 전략 구현체
// 요구사항: 장소, 예산 등의 정보가 불명확할 때 각 맥락에 맞는 맞춤형 질문을 생성합니다.
@Component("clarificationStrategy")
public class ClarificationStrategy implements FollowUpStrategy {

    // 모호하다고 판단할 광역 도시 목록
    private static final List<String> BROAD_CITIES = List.of("서울", "부산", "제주");

    // 주어진 여행 정보를 바탕으로 모호한 부분을 찾아 구체화를 위한 질문을 생성합니다.
    @Override
    public Optional<FollowUpQuestion> generate(TravelInfo travelInfo) {
        // 정보가 아예 없으면 이 전략은 동작하지 않습니다.
        if (travelInfo == null) {
            return Optional.empty();
        }

        // 1. 장소 정보가 모호한지 확인 (예: 목적지가 "서울" 하나만 있는 경우)
        if (isDestinationAmbiguous(travelInfo)) {
            var destination = travelInfo.destinations().get(0);
            var questionText = String.format("'%s' 전체를 구경하실 건가요, 아니면 특정 지역(예: 강남, 명동)을 중심으로 다니실 건가요?", destination);
            return Optional.of(new FollowUpQuestion("DESTINATION_CLARIFICATION", questionText));
        }

        // 2. 예산 정보가 모호한지 확인 (예: 여행 스타일은 "저렴하게"인데, 구체적인 예산이 없는 경우)
        if (isBudgetAmbiguous(travelInfo)) {
            return Optional.of(new FollowUpQuestion(
                    "BUDGET_CLARIFICATION",
                    "대략적인 예산은 얼마정도 생각하시나요? (예: 50만원, 100만원)"
            ));
        }

        // TODO: 다른 정보(여행 스타일 등)에 대한 구체화 질문 로직 추가 가능

        // 구체화할 정보가 없으면 빈 Optional을 반환합니다.
        return Optional.empty();
    }

    // 목적지 정보가 구체화가 필요한지 판단하는 헬퍼 메서드
    private boolean isDestinationAmbiguous(TravelInfo travelInfo) {
        // 목적지가 있고, 단 하나만 있으며, 그 목적지가 광역 도시 목록에 포함되는 경우
        return travelInfo.destinations() != null &&
               travelInfo.destinations().size() == 1 &&
               BROAD_CITIES.contains(travelInfo.destinations().get(0));
    }

    // 예산 정보가 구체화가 필요한지 판단하는 헬퍼 메서드
    private boolean isBudgetAmbiguous(TravelInfo travelInfo) {
        // 예산은 아직 입력되지 않았고, 여행 스타일이 "저렴하게"와 같은 모호한 표현일 경우
        return travelInfo.budget() == null &&
               travelInfo.travelStyle() != null &&
               travelInfo.travelStyle().contains("저렴하게");
    }
}
