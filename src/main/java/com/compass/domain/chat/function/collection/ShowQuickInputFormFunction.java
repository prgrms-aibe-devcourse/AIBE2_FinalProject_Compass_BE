package com.compass.domain.chat.function.collection;

import com.compass.domain.chat.model.dto.QuickInputFormDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;


//빠른 입력 폼 UI 구조를 생성하는 Function
// 사용자가 "여행 계획 짜줘"와 같이 여행 계획 의도를 처음 밝혔을 때 호출.

@Slf4j
@Component("showQuickInputForm")
public class ShowQuickInputFormFunction implements Function<ShowQuickInputFormFunction.Request, QuickInputFormDto> {


    public record Request() {}

    @Override
    public QuickInputFormDto apply(Request request) {
        log.info("빠른 입력 폼 생성을 시작합니다.");

        var formFields = List.of(
                createDestinationField(),
                createDepartureField(),
                createTravelDatesField(),
                createCompanionsField(),
                createBudgetField(),
                createTravelStyleField(),
                createReservationDocumentField()
        );


        var validationRules = Map.<String, Object>of();


        return new QuickInputFormDto("QUICK_INPUT_V1", formFields, validationRules);
    }



    // 1. 목적지 필드
    private QuickInputFormDto.FormField createDestinationField() {
        return new QuickInputFormDto.FormField(
                "destinations",
                "tag-input",
                "목적지",
                "어디로 여행가고 싶으세요? (입력 후 Enter)",
                List.of("목적지 미정"), // "목적지 미정" 옵션 포함
                true
        );
    }

    // 2. 출발지 필드
    private QuickInputFormDto.FormField createDepartureField() {
        return new QuickInputFormDto.FormField("departureLocation", "text-input", "출발지", "어디서 출발하세요?", null, true);
    }

    // 3. 여행 날짜 필드
    private QuickInputFormDto.FormField createTravelDatesField() {
        return new QuickInputFormDto.FormField("travelDates", "date-range-picker", "여행 날짜", "언제 여행을 떠나시나요?", null, true);
    }

    // 4. 동행자 필드
    private QuickInputFormDto.FormField createCompanionsField() {
        return new QuickInputFormDto.FormField("companions", "select", "동행자", "누구와 함께 가시나요?", List.of("혼자", "친구와", "연인과", "가족과", "기타"), false);
    }

    // 5. 예산 필드
    private QuickInputFormDto.FormField createBudgetField() {
        return new QuickInputFormDto.FormField("budget", "number-input", "예산 (1인 기준)", "예산을 알려주세요 (단위: 원)", null, false);
    }

    // 6. 여행 스타일 필드
    private QuickInputFormDto.FormField createTravelStyleField() {
        return new QuickInputFormDto.FormField("travelStyle", "tag-input", "여행 스타일", "원하는 여행 스타일을 선택해주세요. (입력 후 Enter)", List.of("휴양", "관광", "맛집", "쇼핑", "액티비티", "자연", "문화/예술"), false);
    }

    // 7. 예약 정보(항공권/숙소) 필드
    private QuickInputFormDto.FormField createReservationDocumentField() {
        return new QuickInputFormDto.FormField("reservationDocument", "file-input", "예약 정보 업로드", "항공권, 숙소 예약 내역을 이미지로 올려주시면 자동으로 내용을 읽어 반영해 드려요.", null, false);
    }
}
