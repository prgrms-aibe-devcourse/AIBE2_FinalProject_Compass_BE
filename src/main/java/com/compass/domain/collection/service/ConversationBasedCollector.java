package com.compass.domain.collection.service;

import com.compass.domain.collection.dto.DateRange;
import com.compass.domain.collection.dto.TravelInfo;
import com.compass.domain.parser.service.NaturalLanguageParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// 자연스러운 대화를 통해 여행 정보를 수집하는 구현체
// 요구사항 2.3.2: TravelInfoCollector의 구현체로서, 대화형 입력을 파싱하여 TravelInfo를 점진적으로 완성.
@Component("conversationBasedCollector")
@RequiredArgsConstructor
public class ConversationBasedCollector implements TravelInfoCollector {

    private final NaturalLanguageParser naturalLanguageParser;

    // 대화형 입력을 위한 데이터 구조
    // infoType: 현재 수집하려는 정보의 종류 (예: "DESTINATION", "DATE_RANGE")
    // userMessage: 사용자의 답변 (예: "제주도", "다음 주 금요일부터 3일간")
    // existingInfo: 현재까지 수집된 여행 정보
    public record ConversationInput(
            String infoType,
            String userMessage,
            TravelInfo existingInfo
    ) {}

    // 사용자의 대화형 입력을 파싱하여 기존 TravelInfo 객체를 업데이트합니다.
    // input: ConversationInput 타입의 객체여야 합니다.
    // 반환값: 업데이트된 TravelInfo 객체
    // 예외: 입력이 ConversationInput 타입이 아니거나, 파싱에 실패할 경우 IllegalArgumentException 발생
    @Override
    public TravelInfo collect(Object input) {
        if (!(input instanceof ConversationInput convInput)) {
            throw new IllegalArgumentException("Input for ConversationBasedCollector must be of type ConversationInput");
        }

        // 파서를 사용하여 사용자 입력을 구조화된 데이터로 변환
        // 예: "다음 주 금요일" -> DateRange 객체
        var parsedData = naturalLanguageParser.parse(convInput.userMessage(), convInput.infoType());

        // 기존 정보와 새로 파싱된 정보를 합쳐 새로운 TravelInfo 객체를 생성
        return updateTravelInfo(convInput.existingInfo(), convInput.infoType(), parsedData);
    }

    // 파싱된 데이터를 기반으로 TravelInfo 객체를 업데이트하는 헬퍼 메서드
    private TravelInfo updateTravelInfo(TravelInfo existingInfo, String infoType, Object parsedData) {
        // 대화의 첫 턴이라 기존 정보가 없으면, 비어있는 객체로 시작합니다.
        var currentInfo = (existingInfo != null) ? existingInfo : new TravelInfo(null, null, null, null, null);

        // infoType에 따라 적절한 필드를 업데이트하여 새로운 TravelInfo 객체를 반환합니다.
        return switch (infoType) {
            case "DESTINATION" -> new TravelInfo(
                    (List<String>) parsedData,
                    currentInfo.travelDates(),
                    currentInfo.budget(),
                    currentInfo.companions(),
                    currentInfo.travelStyle()
            );
            case "DATE_RANGE" -> new TravelInfo(
                    currentInfo.destinations(),
                    (DateRange) parsedData,
                    currentInfo.budget(),
                    currentInfo.companions(),
                    currentInfo.travelStyle()
            );
            // TODO: BUDGET, COMPANIONS, TRAVEL_STYLE 등 다른 정보 타입에 대한 처리 로직을 추가해야 합니다.
            default -> throw new IllegalArgumentException("지원하지 않는 정보 타입입니다: " + infoType);
        };
    }
}
