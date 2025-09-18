package com.compass.domain.chat.function.config;

import com.compass.domain.chat.function.adjustment.ModifyItineraryFunction;
import com.compass.domain.chat.function.adjustment.RegenerateItineraryFunction;
import com.compass.domain.chat.function.collection.*;
import com.compass.domain.chat.function.external.SearchTourAPIFunction;
import com.compass.domain.chat.function.external.SearchWithPerplexityFunction;
import com.compass.domain.chat.function.finalization.ExportItineraryFunction;
import com.compass.domain.chat.function.finalization.SaveFinalItineraryFunction;
import com.compass.domain.chat.function.general.HandleGeneralQueryFunction;
import com.compass.domain.chat.function.general.RestartPlanningFunction;
import com.compass.domain.chat.function.general.ShowProgressFunction;
import com.compass.domain.chat.function.itinerary.GenerateItineraryFunction;
import com.compass.domain.chat.function.itinerary.SearchPlacesFunction;
import com.compass.domain.chat.function.itinerary.ShowItineraryFunction;
import com.compass.domain.chat.function.planning.DestinationSearchFunction;
import com.compass.domain.chat.function.planning.GenerateTravelPlanFunction;
import com.compass.domain.chat.function.processing.ExtractFlightInfoFunction;
import com.compass.domain.chat.function.processing.ProcessImageFunction;
import com.compass.domain.chat.function.processing.ProcessOCRFunction;
import com.compass.domain.chat.function.utility.SearchWebFunction;
import com.compass.domain.chat.function.utility.SendNotificationFunction;

import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Spring AI Function 설정 - 모든 Function을 Bean으로 등록
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FunctionConfiguration {

    // ========== 정보 수집 Functions (Collection) ==========

    private final SubmitTravelFormFunction submitTravelFormFunction;
    private final ShowQuickInputFormFunction showQuickInputFormFunction;
    private final AskFollowUpQuestionFunction askFollowUpQuestionFunction;
    private final StartFollowUpFunction startFollowUpFunction;
    private final ValidateAndStoreInfoFunction validateAndStoreInfoFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> submitTravelFormWrapper() {
        return FunctionCallbackWrapper.builder(submitTravelFormFunction)
                .withName("submit_travel_form")
                .withDescription("여행 정보 폼 제출 처리")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> showQuickInputFormWrapper() {
        return FunctionCallbackWrapper.builder(showQuickInputFormFunction)
                .withName("show_quick_input_form")
                .withDescription("빠른 입력 폼 표시")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> askFollowUpQuestionWrapper() {
        return FunctionCallbackWrapper.builder(askFollowUpQuestionFunction)
                .withName("ask_follow_up_question")
                .withDescription("부족한 정보에 대한 추가 질문 생성")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> startFollowUpWrapper() {
        return FunctionCallbackWrapper.builder(startFollowUpFunction)
                .withName("start_follow_up")
                .withDescription("Follow-up 질문 시작")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> validateAndStoreInfoWrapper() {
        return FunctionCallbackWrapper.builder(validateAndStoreInfoFunction)
                .withName("validate_and_store_info")
                .withDescription("수집된 정보 검증 및 저장")
                .build();
    }

    // ========== 여행 계획 Functions (Planning) ==========

    private final GenerateTravelPlanFunction generateTravelPlanFunction;
    private final DestinationSearchFunction destinationSearchFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> generateTravelPlanWrapper() {
        return FunctionCallbackWrapper.builder(generateTravelPlanFunction)
                .withName("generate_travel_plan")
                .withDescription("여행 계획 생성")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> destinationSearchWrapper() {
        return FunctionCallbackWrapper.builder(destinationSearchFunction)
                .withName("destination_search")
                .withDescription("목적지 검색 및 추천")
                .build();
    }

    // ========== 일정 관리 Functions (Itinerary) ==========

    private final GenerateItineraryFunction generateItineraryFunction;
    private final ShowItineraryFunction showItineraryFunction;
    private final SearchPlacesFunction searchPlacesFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> generateItineraryWrapper() {
        return FunctionCallbackWrapper.builder(generateItineraryFunction)
                .withName("generate_itinerary")
                .withDescription("상세 여행 일정 생성")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> showItineraryWrapper() {
        return FunctionCallbackWrapper.builder(showItineraryFunction)
                .withName("show_itinerary")
                .withDescription("생성된 일정 표시")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> searchPlacesWrapper() {
        return FunctionCallbackWrapper.builder(searchPlacesFunction)
                .withName("search_places")
                .withDescription("관광지 및 장소 검색")
                .build();
    }

    // ========== 일정 수정 Functions (Adjustment) ==========

    private final ModifyItineraryFunction modifyItineraryFunction;
    private final RegenerateItineraryFunction regenerateItineraryFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> modifyItineraryWrapper() {
        return FunctionCallbackWrapper.builder(modifyItineraryFunction)
                .withName("modify_itinerary")
                .withDescription("일정 수정")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> regenerateItineraryWrapper() {
        return FunctionCallbackWrapper.builder(regenerateItineraryFunction)
                .withName("regenerate_itinerary")
                .withDescription("일정 재생성")
                .build();
    }

    // ========== 최종화 Functions (Finalization) ==========

    private final SaveFinalItineraryFunction saveFinalItineraryFunction;
    private final ExportItineraryFunction exportItineraryFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> saveFinalItineraryWrapper() {
        return FunctionCallbackWrapper.builder(saveFinalItineraryFunction)
                .withName("save_final_itinerary")
                .withDescription("최종 일정 저장")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> exportItineraryWrapper() {
        return FunctionCallbackWrapper.builder(exportItineraryFunction)
                .withName("export_itinerary")
                .withDescription("일정 내보내기 (PDF, 이미지 등)")
                .build();
    }

    // ========== 일반 Functions (General) ==========

    private final HandleGeneralQueryFunction handleGeneralQueryFunction;
    private final ShowProgressFunction showProgressFunction;
    private final RestartPlanningFunction restartPlanningFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> handleGeneralQueryWrapper() {
        return FunctionCallbackWrapper.builder(handleGeneralQueryFunction)
                .withName("handle_general_query")
                .withDescription("일반 질문 처리")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> showProgressWrapper() {
        return FunctionCallbackWrapper.builder(showProgressFunction)
                .withName("show_progress")
                .withDescription("진행 상황 표시")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> restartPlanningWrapper() {
        return FunctionCallbackWrapper.builder(restartPlanningFunction)
                .withName("restart_planning")
                .withDescription("계획 다시 시작")
                .build();
    }

    // ========== 처리 Functions (Processing) ==========

    private final ExtractFlightInfoFunction extractFlightInfoFunction;
    private final ProcessImageFunction processImageFunction;
    private final ProcessOCRFunction processOCRFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> extractFlightInfoWrapper() {
        return FunctionCallbackWrapper.builder(extractFlightInfoFunction)
                .withName("extract_flight_info")
                .withDescription("항공권 정보 추출")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> processImageWrapper() {
        return FunctionCallbackWrapper.builder(processImageFunction)
                .withName("process_image")
                .withDescription("이미지 처리 및 분석")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> processOCRWrapper() {
        return FunctionCallbackWrapper.builder(processOCRFunction)
                .withName("process_ocr")
                .withDescription("OCR을 통한 텍스트 추출")
                .build();
    }

    // ========== 외부 API Functions (External) ==========

    private final SearchTourAPIFunction searchTourAPIFunction;
    private final SearchWithPerplexityFunction searchWithPerplexityFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> searchTourAPIWrapper() {
        return FunctionCallbackWrapper.builder(searchTourAPIFunction)
                .withName("search_tour_api")
                .withDescription("한국관광공사 API 검색")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> searchWithPerplexityWrapper() {
        return FunctionCallbackWrapper.builder(searchWithPerplexityFunction)
                .withName("search_with_perplexity")
                .withDescription("Perplexity AI를 통한 실시간 웹 검색")
                .build();
    }

    // ========== 유틸리티 Functions (Utility) ==========

    private final SearchWebFunction searchWebFunction;
    private final SendNotificationFunction sendNotificationFunction;

    @Bean
    public FunctionCallbackWrapper<?, ?> searchWebWrapper() {
        return FunctionCallbackWrapper.builder(searchWebFunction)
                .withName("search_web")
                .withDescription("웹 검색")
                .build();
    }

    @Bean
    public FunctionCallbackWrapper<?, ?> sendNotificationWrapper() {
        return FunctionCallbackWrapper.builder(sendNotificationFunction)
                .withName("send_notification")
                .withDescription("알림 전송")
                .build();
    }

    // Bean 등록 후 로깅
    @Bean
    public String functionRegistrationLogger() {
        log.info("========== Spring AI Function 등록 완료 ==========");
        log.info("총 25개 Function이 FunctionCallbackWrapper로 등록되었습니다.");
        log.info("- Collection Functions: 5개");
        log.info("- Planning Functions: 2개");
        log.info("- Itinerary Functions: 3개");
        log.info("- Adjustment Functions: 2개");
        log.info("- Finalization Functions: 2개");
        log.info("- General Functions: 3개");
        log.info("- Processing Functions: 3개");
        log.info("- External API Functions: 2개");
        log.info("- Utility Functions: 2개");
        log.info("=================================================");
        return "Function Registration Complete";
    }
}