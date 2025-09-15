package com.compass.domain.trip.config;

import com.compass.domain.trip.function.TravelPlanFunction;
import com.compass.domain.trip.function.TravelPlanModifyFunction;
// import com.compass.domain.trip.function.TravelPlanSearchFunction; // 아직 구현되지 않음
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TRIP 도메인 Function Configuration
 * REQ-TRIP-001: 여행 계획 생성 Function
 * 
 * MainLLMOrchestrator와 연동되는 Function들을 정의합니다.
 * Spring AI Function Calling 표준을 따릅니다.
 */
@Configuration
@RequiredArgsConstructor
public class TripFunctionConfiguration {

    private final TravelPlanFunction travelPlanFunction;
    private final TravelPlanModifyFunction travelPlanModifyFunction;
    // private final TravelPlanSearchFunction travelPlanSearchFunction; // 아직 구현되지 않음

    /**
     * 여행 계획 생성 Function
     * MainLLMOrchestrator에서 여행 계획 생성 요청 시 호출됩니다.
     * 완성된 여행 정보를 받아서 상세한 일정을 생성합니다.
     */
    @Bean("generateTravelPlan")
    // @org.springframework.ai.function.FunctionDescription("사용자의 완성된 여행 정보(목적지, 기간, 예산, 여행 스타일)를 기반으로 상세한 여행 계획을 생성합니다. DB에서 장소를 검색하고, 필요시 외부 API를 활용하여 최적화된 일정을 만들어줍니다.") // Spring AI Function Calling은 나중에 구현
    public java.util.function.Function<com.compass.domain.trip.function.model.TravelPlanRequest, com.compass.domain.trip.function.model.TravelPlanResponse> generateTravelPlan() {
        return travelPlanFunction::execute;
    }

    /**
     * 여행 계획 수정 Function
     * MainLLMOrchestrator에서 기존 여행 계획 수정 요청 시 호출됩니다.
     * 완성된 수정 정보를 받아서 기존 계획을 업데이트합니다.
     */
    @Bean("modifyTravelPlan")
    // @org.springframework.ai.function.FunctionDescription("기존 여행 계획을 수정합니다. 사용자의 완성된 수정 요청(목적지 변경, 날짜 변경, 예산 조정, 장소 추가/제거 등)을 받아서 기존 계획을 업데이트하고 최적화된 일정을 제공합니다.") // Spring AI Function Calling은 나중에 구현
    public java.util.function.Function<com.compass.domain.trip.function.model.TravelPlanModifyRequest, com.compass.domain.trip.function.model.TravelPlanResponse> modifyTravelPlan() {
        return travelPlanModifyFunction::execute;
    }

    /**
     * 여행 계획 검색 Function
     * MainLLMOrchestrator에서 여행 계획 검색 요청 시 호출됩니다.
     * 완성된 검색 조건을 받아서 해당하는 여행 계획들을 반환합니다.
     */
    // @Bean("searchTravelPlans") // 아직 구현되지 않음
    // @org.springframework.ai.function.FunctionDescription("사용자의 여행 계획을 검색합니다. 완성된 검색 조건(목적지, 날짜 범위, 키워드, 여행 스타일 등)을 받아서 해당하는 여행 계획 목록을 반환합니다.") // Spring AI Function Calling은 나중에 구현
    // public java.util.function.Function<com.compass.domain.trip.function.model.TravelPlanSearchRequest, java.util.List<com.compass.domain.trip.function.model.TravelPlanSummary>> searchTravelPlans() {
    //     return travelPlanSearchFunction::execute;
    // }
}
