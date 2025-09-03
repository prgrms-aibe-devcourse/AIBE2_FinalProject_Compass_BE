package com.compass.domain.chat.parser;

import com.compass.domain.chat.dto.TripPlanningRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatInputParser 테스트")
class ChatInputParserTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private ChatInputParser chatInputParser;

    @BeforeEach
    void setUp() {
        // Mock AI response if needed
        ChatResponse mockResponse = new ChatResponse(
            java.util.List.of(new Generation(new AssistantMessage("{}"), null))
        );
        when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);
    }

    @Test
    @DisplayName("여행지 추출 테스트")
    void testExtractDestination() {
        // Given
        String input = "다음달에 제주도 여행 가고 싶어요";
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("제주");
    }

    @Test
    @DisplayName("날짜 추출 테스트 - 구체적 날짜")
    void testExtractSpecificDates() {
        // Given
        String input = "2024년 12월 25일부터 12월 28일까지 부산 여행";
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("부산");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2024, 12, 25));
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 28));
    }

    @Test
    @DisplayName("날짜 추출 테스트 - 상대적 날짜")
    void testExtractRelativeDates() {
        // Given
        String input = "다음주에 서울 여행 2박3일로 가려고 해요";
        LocalDate today = LocalDate.now();
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("서울");
        assertThat(result.getStartDate()).isAfter(today);
        assertThat(result.getEndDate()).isAfter(result.getStartDate());
    }

    @Test
    @DisplayName("날짜 추출 테스트 - N박M일 형식")
    void testExtractDurationFormat() {
        // Given
        String input = "강릉으로 3박4일 여행 계획중";
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("강릉");
        // Start date will be default (7 days from now)
        assertThat(result.getStartDate()).isNotNull();
        assertThat(result.getEndDate()).isEqualTo(result.getStartDate().plusDays(3));
    }

    @Test
    @DisplayName("예산 추출 테스트 - 원화")
    void testExtractBudgetKRW() {
        // Given
        String input = "예산 100만원으로 경주 여행";
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("경주");
        assertThat(result.getBudgetPerPerson()).isEqualTo(1000000);
    }

    @Test
    @DisplayName("예산 추출 테스트 - 여행 스타일")
    void testExtractTravelStyle() {
        // Given
        String inputLuxury = "럭셔리하게 여행하고 싶어요";
        String inputBudget = "저렴하게 여행하려고 해요";
        
        // When
        TripPlanningRequest resultLuxury = chatInputParser.parseUserInput(inputLuxury);
        TripPlanningRequest resultBudget = chatInputParser.parseUserInput(inputBudget);
        
        // Then
        assertThat(resultLuxury.getTravelStyle()).isEqualTo("luxury");
        assertThat(resultBudget.getTravelStyle()).isEqualTo("budget");
    }

    @Test
    @DisplayName("인원 추출 테스트 - 숫자")
    void testExtractTravelerCountNumber() {
        // Given
        String input = "4명이서 여수 여행 갈 예정";
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("여수");
        assertThat(result.getNumberOfTravelers()).isEqualTo(4);
    }

    @Test
    @DisplayName("인원 추출 테스트 - 설명적 표현")
    void testExtractTravelerCountDescriptive() {
        // Given
        String inputCouple = "커플 여행으로 전주 가려고 해요";
        String inputFamily = "가족여행으로 속초 갈 예정";
        String inputSolo = "혼자 제주도 여행";
        
        // When
        TripPlanningRequest resultCouple = chatInputParser.parseUserInput(inputCouple);
        TripPlanningRequest resultFamily = chatInputParser.parseUserInput(inputFamily);
        TripPlanningRequest resultSolo = chatInputParser.parseUserInput(inputSolo);
        
        // Then
        assertThat(resultCouple.getNumberOfTravelers()).isEqualTo(2);
        assertThat(resultFamily.getNumberOfTravelers()).isEqualTo(4);
        assertThat(resultSolo.getNumberOfTravelers()).isEqualTo(1);
    }

    @Test
    @DisplayName("관심사 추출 테스트")
    void testExtractInterests() {
        // Given
        String input = "맛집 탐방과 쇼핑을 즐기면서 문화 체험도 하고 싶어요";
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getInterests()).contains("food", "shopping", "culture");
    }

    @Test
    @DisplayName("복합적인 입력 파싱 테스트")
    void testComplexInput() {
        // Given
        String input = "다음달 15일부터 3박4일로 부산 여행을 가려고 해요. " +
                      "2명이서 가는데 예산은 1인당 50만원 정도로 생각하고 있어요. " +
                      "맛집 탐방이랑 해변에서 휴양하는 걸 좋아해요.";
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("부산");
        assertThat(result.getNumberOfTravelers()).isEqualTo(2);
        assertThat(result.getBudgetPerPerson()).isEqualTo(500000);
        assertThat(result.getInterests()).contains("food", "nature");
        assertThat(result.getEndDate()).isEqualTo(result.getStartDate().plusDays(3));
    }

    @Test
    @DisplayName("기본값 설정 테스트")
    void testDefaultValues() {
        // Given
        String input = "여행 가고 싶어요";
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getOrigin()).isEqualTo("서울");
        assertThat(result.getNumberOfTravelers()).isEqualTo(1);
        assertThat(result.getTravelStyle()).isEqualTo("moderate");
        assertThat(result.getCurrency()).isEqualTo("KRW");
        assertThat(result.getStartDate()).isNotNull();
        assertThat(result.getEndDate()).isNotNull();
    }

    @Test
    @DisplayName("한국어 월일 형식 파싱 테스트")
    void testKoreanDateFormat() {
        // Given
        String input = "3월 25일부터 3월 28일까지 경주 여행";
        int currentYear = LocalDate.now().getYear();
        
        // When
        TripPlanningRequest result = chatInputParser.parseUserInput(input);
        
        // Then
        assertThat(result.getDestination()).isEqualTo("경주");
        assertThat(result.getStartDate().getMonthValue()).isEqualTo(3);
        assertThat(result.getStartDate().getDayOfMonth()).isEqualTo(25);
        assertThat(result.getEndDate().getMonthValue()).isEqualTo(3);
        assertThat(result.getEndDate().getDayOfMonth()).isEqualTo(28);
    }

    @Test
    @DisplayName("예산 단위 변환 테스트")
    void testBudgetUnitConversion() {
        // Given
        String input1 = "예산 500만원";
        String input2 = "예산 30만원";
        String input3 = "예산 1000달러";
        
        // When
        TripPlanningRequest result1 = chatInputParser.parseUserInput(input1);
        TripPlanningRequest result2 = chatInputParser.parseUserInput(input2);
        TripPlanningRequest result3 = chatInputParser.parseUserInput(input3);
        
        // Then
        assertThat(result1.getBudgetPerPerson()).isEqualTo(5000000);
        assertThat(result2.getBudgetPerPerson()).isEqualTo(300000);
        assertThat(result3.getBudgetPerPerson()).isEqualTo(1300000); // 1000 USD * 1300
    }
}