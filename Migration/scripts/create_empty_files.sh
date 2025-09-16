#!/bin/bash

# ============================================
# 스켈레톤 파일 생성 스크립트 (빈 파일)
# ============================================

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 프로젝트 루트
PROJECT_ROOT="/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE"
SRC_ROOT="$PROJECT_ROOT/src/main/java/com/compass"

echo -e "${GREEN}📄 Creating skeleton files for new implementation...${NC}"
echo "=================================="

# ========== ORCHESTRATOR 파일 생성 ==========
echo -e "${YELLOW}🧠 Creating Orchestrator files...${NC}"

# MainLLMOrchestrator.java
cat > $SRC_ROOT/domain/chat/orchestrator/MainLLMOrchestrator.java << 'EOF'
package com.compass.domain.chat.orchestrator;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메인 LLM 오케스트레이터
 *
 * TODO: 구현 필요
 * 담당: [Chat2 개발자]
 *
 * 주요 기능:
 * 1. Intent 분류
 * 2. Phase 관리
 * 3. Function 선택 및 실행
 * 4. LLM 호출 및 응답 처리
 *
 * 참고: documents/CHAT2_TRAVEL_WORKFLOW_DETAILED.md
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MainLLMOrchestrator {

    // private final ChatModel chatModel;
    // private final IntentClassifier intentClassifier;
    // private final PhaseManager phaseManager;

    // TODO: orchestrate() 메서드 구현

}
EOF
echo "  ✓ Created: MainLLMOrchestrator.java"

# IntentClassifier.java
cat > $SRC_ROOT/domain/chat/orchestrator/IntentClassifier.java << 'EOF'
package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.enums.Intent;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 의도 분류기
 *
 * TODO: 구현 필요
 * 담당: [Chat2 개발자]
 *
 * Intent 종류:
 * - TRAVEL_PLANNING: 여행 계획 요청
 * - INFORMATION_COLLECTION: 정보 수집
 * - IMAGE_UPLOAD: 이미지 업로드
 * - GENERAL_QUESTION: 일반 질문
 */
@Slf4j
@Component
public class IntentClassifier {

    // TODO: classifyIntent() 메서드 구현

}
EOF
echo "  ✓ Created: IntentClassifier.java"

# PhaseManager.java
cat > $SRC_ROOT/domain/chat/orchestrator/PhaseManager.java << 'EOF'
package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.enums.TravelPhase;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 계획 Phase 관리자
 *
 * TODO: 구현 필요
 * 담당: [Chat2 개발자]
 *
 * Phase 순서:
 * 1. INITIALIZATION
 * 2. INFORMATION_COLLECTION
 * 3. PLAN_GENERATION
 * 4. FEEDBACK_REFINEMENT
 * 5. COMPLETION
 */
@Slf4j
@Component
public class PhaseManager {

    // TODO: getCurrentPhase() 메서드 구현
    // TODO: transitionPhase() 메서드 구현

}
EOF
echo "  ✓ Created: PhaseManager.java"

# ========== CONTROLLER 파일 생성 ==========
echo -e "${YELLOW}🎮 Creating Controller files...${NC}"

cat > $SRC_ROOT/domain/chat/controller/UnifiedChatController.java << 'EOF'
package com.compass.domain.chat.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 통합 채팅 컨트롤러 (단일 진입점)
 *
 * TODO: 구현 필요
 * 담당: [Chat2 개발자]
 *
 * 엔드포인트:
 * - POST /api/chat/unified : 메인 채팅 처리
 * - GET /api/chat/threads : 스레드 목록 조회
 * - GET /api/chat/threads/{threadId}/messages : 메시지 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class UnifiedChatController {

    // private final MainLLMOrchestrator orchestrator;

    @PostMapping("/unified")
    public ResponseEntity<?> handleChat(@RequestBody Object request) {
        // TODO: 구현
        return ResponseEntity.ok("TODO");
    }

}
EOF
echo "  ✓ Created: UnifiedChatController.java"

# ========== AUTH CONTROLLER 생성 ==========
cat > $SRC_ROOT/domain/auth/controller/AuthController.java << 'EOF'
package com.compass.domain.auth.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증/인가 컨트롤러
 *
 * TODO: 구현 필요
 * 담당: [Chat 개발자]
 *
 * 엔드포인트:
 * - POST /api/auth/login
 * - POST /api/auth/signup
 * - POST /api/auth/refresh
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // TODO: 구현

}
EOF
echo "  ✓ Created: AuthController.java"

# ========== FUNCTION 파일 생성 ==========
echo -e "${YELLOW}⚡ Creating Function files...${NC}"

# FunctionConfiguration.java
cat > $SRC_ROOT/domain/chat/function/config/FunctionConfiguration.java << 'EOF'
package com.compass.domain.chat.function.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.function.Function;

/**
 * Function Bean 설정
 *
 * TODO: 구현 필요
 * 담당: [전체 - 각자 Function Bean 등록]
 *
 * 등록할 Function:
 * - showQuickInputForm
 * - submitTravelForm
 * - processImage
 * - generateTravelPlan
 * - recommendDestinations
 */
@Configuration
public class FunctionConfiguration {

    // @Bean("showQuickInputForm")
    // public Function<?, ?> showQuickInputForm() {
    //     return request -> {
    //         // TODO: 구현
    //     };
    // }

}
EOF
echo "  ✓ Created: FunctionConfiguration.java"

# ShowQuickInputFormFunction.java
cat > $SRC_ROOT/domain/chat/function/collection/ShowQuickInputFormFunction.java << 'EOF'
package com.compass.domain.chat.function.collection;

import java.util.function.Function;

/**
 * 빠른 입력 폼 표시 Function
 *
 * TODO: 구현 필요
 * 담당: [User 개발자]
 *
 * 폼 필드:
 * - destinations (태그 입력)
 * - departureLocation
 * - travelDates
 * - companions
 * - budget
 * - travelStyle
 */
public class ShowQuickInputFormFunction implements Function<Object, Object> {

    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: ShowQuickInputFormFunction.java"

# ProcessImageFunction.java
cat > $SRC_ROOT/domain/chat/function/processing/ProcessImageFunction.java << 'EOF'
package com.compass.domain.chat.function.processing;

import java.util.function.Function;

/**
 * 이미지 처리 Function (OCR)
 *
 * TODO: 구현 필요
 * 담당: [Media 개발자]
 *
 * 기능:
 * - S3 업로드
 * - OCR 텍스트 추출
 * - 여행 정보 파싱
 */
public class ProcessImageFunction implements Function<Object, Object> {

    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: ProcessImageFunction.java"

# GenerateTravelPlanFunction.java
cat > $SRC_ROOT/domain/chat/function/planning/GenerateTravelPlanFunction.java << 'EOF'
package com.compass.domain.chat.function.planning;

import java.util.function.Function;

/**
 * 여행 계획 생성 Function
 *
 * TODO: 구현 필요
 * 담당: [Trip 개발자]
 *
 * 기능:
 * - 단일/복수 도시 계획 생성
 * - 일정 최적화
 * - 장소 추천
 */
public class GenerateTravelPlanFunction implements Function<Object, Object> {

    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: GenerateTravelPlanFunction.java"

# ========== MODEL/ENUM 파일 생성 ==========
echo -e "${YELLOW}📊 Creating Model/Enum files...${NC}"

# TravelContext.java
cat > $SRC_ROOT/domain/chat/model/context/TravelContext.java << 'EOF'
package com.compass.domain.chat.model.context;

import lombok.Data;
import lombok.Builder;
import java.util.Map;
import java.util.HashMap;

/**
 * 여행 대화 컨텍스트
 *
 * TODO: 구현 필요
 * 담당: [Chat2 개발자]
 *
 * 기능: 대화 진행 중 수집된 정보 및 상태 관리
 */
@Data
@Builder
public class TravelContext {
    private String threadId;
    private String userId;
    private String currentPhase;
    private Map<String, Object> collectedInfo;
    private Map<String, Object> travelPlan;

    public TravelContext() {
        this.collectedInfo = new HashMap<>();
        this.travelPlan = new HashMap<>();
    }
}
EOF
echo "  ✓ Created: TravelContext.java"

# Intent.java
cat > $SRC_ROOT/domain/chat/model/enums/Intent.java << 'EOF'
package com.compass.domain.chat.model.enums;

/**
 * 사용자 의도 열거형
 */
public enum Intent {
    TRAVEL_PLANNING("여행 계획"),
    INFORMATION_COLLECTION("정보 수집"),
    IMAGE_UPLOAD("이미지 업로드"),
    GENERAL_QUESTION("일반 질문"),
    QUICK_INPUT("빠른 입력 폼"),
    DESTINATION_SEARCH("목적지 검색"),
    RESERVATION_PROCESSING("예약 정보 처리"),
    API_USAGE_CHECK("API 사용량 조회"),
    UNKNOWN("알 수 없음");

    private final String description;

    Intent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
EOF
echo "  ✓ Created: Intent.java"

# TravelPhase.java
cat > $SRC_ROOT/domain/chat/model/enums/TravelPhase.java << 'EOF'
package com.compass.domain.chat.model.enums;

/**
 * 여행 계획 Phase 열거형
 */
public enum TravelPhase {
    INITIALIZATION("초기화"),
    INFORMATION_COLLECTION("정보 수집"),
    PLAN_GENERATION("계획 생성"),
    FEEDBACK_REFINEMENT("피드백 처리"),
    COMPLETION("완료");

    private final String koreanName;

    TravelPhase(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }
}
EOF
echo "  ✓ Created: TravelPhase.java"

# ========== REQUEST/RESPONSE 모델 생성 ==========
echo -e "${YELLOW}📦 Creating Request/Response models...${NC}"

# ChatRequest.java
cat > $SRC_ROOT/domain/chat/model/request/ChatRequest.java << 'EOF'
package com.compass.domain.chat.model.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 채팅 요청 DTO
 *
 * TODO: 필드 추가 필요
 * 담당: [Chat2 개발자]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private String threadId;
    // TODO: 추가 필드
}
EOF
echo "  ✓ Created: ChatRequest.java"

# ChatResponse.java
cat > $SRC_ROOT/domain/chat/model/response/ChatResponse.java << 'EOF'
package com.compass.domain.chat.model.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 채팅 응답 DTO
 *
 * TODO: 필드 추가 필요
 * 담당: [Chat2 개발자]
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String type;
    // TODO: 추가 필드
}
EOF
echo "  ✓ Created: ChatResponse.java"

# ========== 추가 FUNCTION 파일 생성 ==========
echo -e "${YELLOW}⚡ Creating Additional Function files...${NC}"

# SubmitTravelFormFunction.java
cat > $SRC_ROOT/domain/chat/function/collection/SubmitTravelFormFunction.java << 'EOF'
package com.compass.domain.chat.function.collection;

import java.util.function.Function;

/**
 * 폼 제출 처리 Function
 *
 * TODO: 구현 필요
 * 담당: [User 개발자]
 */
public class SubmitTravelFormFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: SubmitTravelFormFunction.java"

# RecommendDestinationsFunction.java
cat > $SRC_ROOT/domain/chat/function/planning/RecommendDestinationsFunction.java << 'EOF'
package com.compass.domain.chat.function.planning;

import java.util.function.Function;

/**
 * 목적지 추천 Function (목적지 미정인 경우)
 *
 * TODO: 구현 필요
 * 담당: [User 개발자]
 */
public class RecommendDestinationsFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: RecommendDestinationsFunction.java"

# HandleGeneralQuestionFunction.java
cat > $SRC_ROOT/domain/chat/function/processing/HandleGeneralQuestionFunction.java << 'EOF'
package com.compass.domain.chat.function.processing;

import java.util.function.Function;

/**
 * 일반 질문 처리 Function
 *
 * TODO: 구현 필요
 * 담당: [Chat 개발자]
 */
public class HandleGeneralQuestionFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: HandleGeneralQuestionFunction.java"

# ========== 추가 MODEL 파일 생성 ==========
echo -e "${YELLOW}📦 Creating Additional Model files...${NC}"

# TravelPlanRequest.java
cat > $SRC_ROOT/domain/chat/model/request/TravelPlanRequest.java << 'EOF'
package com.compass.domain.chat.model.request;

import java.time.LocalDate;
import java.util.List;

/**
 * Trip 도메인으로 전달될 여행 계획 요청 데이터
 *
 * TODO: 구현 필요
 * 담당: [Trip 개발자]
 *
 * 포함 정보:
 * - destinations: 목적지 리스트
 * - departureLocation: 출발지
 * - startDate/endDate: 여행 날짜
 * - companions: 동행자 수
 * - budget: 예산
 * - travelStyles: 여행 스타일
 * - flightInfo: 항공편 정보 (optional)
 * - hotelInfo: 호텔 정보 (optional)
 */
public record TravelPlanRequest(
    List<String> destinations,
    String departureLocation,
    LocalDate startDate,
    LocalDate endDate,
    String companions,
    String budget,
    List<String> travelStyles,
    String customTravelStyle
    // TODO: OCR 정보 필드 추가 (Media 개발자와 협의)
) {}
EOF
echo "  ✓ Created: TravelPlanRequest.java"

# TravelPlanResponse.java
cat > $SRC_ROOT/domain/chat/model/response/TravelPlanResponse.java << 'EOF'
package com.compass.domain.chat.model.response;

/**
 * 여행 계획 응답 DTO
 *
 * TODO: 구현 필요
 * 담당: [Trip 개발자]
 */
public record TravelPlanResponse(
    String planId,
    Object itinerary  // TODO: 상세 구조 정의
) {}
EOF
echo "  ✓ Created: TravelPlanResponse.java"

# ========== 추가 MODEL 파일 생성 (워크플로우 기반) ==========
echo -e "${YELLOW}📦 Creating Additional Data Models...${NC}"

# QuickInputFormDto.java
cat > $SRC_ROOT/domain/chat/model/response/QuickInputFormDto.java << 'EOF'
package com.compass.domain.chat.model.response;

import java.util.List;

/**
 * 빠른 입력 폼 구조 DTO
 *
 * TODO: 구현 필요
 * 담당: [User 개발자]
 *
 * 워크플로우 문서 참조
 */
public record QuickInputFormDto(
    String formType,
    List<FormField> formFields
) {
    public record FormField(
        String field,
        String label,
        String type,
        String placeholder,
        boolean required,
        List<String> options
    ) {}
}
EOF
echo "  ✓ Created: QuickInputFormDto.java"

# TravelFormSubmitRequest.java
cat > $SRC_ROOT/domain/chat/model/request/TravelFormSubmitRequest.java << 'EOF'
package com.compass.domain.chat.model.request;

import java.time.LocalDate;
import java.util.List;

/**
 * 폼 제출 요청 데이터
 *
 * TODO: 구현 필요
 * 담당: [User 개발자]
 *
 * 워크플로우 섹션 2.2.1 참조
 */
public record TravelFormSubmitRequest(
    List<String> destinations,
    String departureLocation,
    LocalDate startDate,
    LocalDate endDate,
    String companions,
    String budget,
    List<String> travelStyle,
    String customTravelStyle,
    String reservationImage
) {}
EOF
echo "  ✓ Created: TravelFormSubmitRequest.java"

# FlightReservation.java
cat > $SRC_ROOT/domain/chat/model/dto/FlightReservation.java << 'EOF'
package com.compass.domain.chat.model.dto;

import java.time.LocalDateTime;

/**
 * 항공편 예약 정보 (OCR 추출용)
 *
 * TODO: 구현 필요
 * 담당: [Media 개발자]
 */
public record FlightReservation(
    String airline,
    String flightNumber,
    String departure,
    String arrival,
    LocalDateTime departureTime,
    LocalDateTime arrivalTime
) {}
EOF
echo "  ✓ Created: FlightReservation.java"

# HotelReservation.java
cat > $SRC_ROOT/domain/chat/model/dto/HotelReservation.java << 'EOF'
package com.compass.domain.chat.model.dto;

import java.time.LocalDate;

/**
 * 호텔 예약 정보 (OCR 추출용)
 *
 * TODO: 구현 필요
 * 담당: [Media 개발자]
 */
public record HotelReservation(
    String hotelName,
    String address,
    LocalDate checkIn,
    LocalDate checkOut,
    String roomType
) {}
EOF
echo "  ✓ Created: HotelReservation.java"

# ========== 추가 FUNCTION 파일 생성 (워크플로우 기반) ==========
echo -e "${YELLOW}⚡ Creating Additional Workflow Functions...${NC}"

# AnalyzeUserInputFunction.java
cat > $SRC_ROOT/domain/chat/function/collection/AnalyzeUserInputFunction.java << 'EOF'
package com.compass.domain.chat.function.collection;

import java.util.function.Function;

/**
 * 사용자 입력 분석 Function
 *
 * TODO: 구현 필요
 * 담당: [Chat2 개발자]
 *
 * 기능: 사용자 입력에서 여행 정보 추출
 */
public class AnalyzeUserInputFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: AnalyzeUserInputFunction.java"

# StartFollowUpFunction.java
cat > $SRC_ROOT/domain/chat/function/collection/StartFollowUpFunction.java << 'EOF'
package com.compass.domain.chat.function.collection;

import java.util.function.Function;

/**
 * Follow-up 질문 시작 Function
 *
 * TODO: 구현 필요
 * 담당: [User 개발자]
 *
 * 기능: 부족한 정보에 대한 추가 질문 생성
 */
public class StartFollowUpFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: StartFollowUpFunction.java"

# ExtractFlightInfoFunction.java
cat > $SRC_ROOT/domain/chat/function/processing/ExtractFlightInfoFunction.java << 'EOF'
package com.compass.domain.chat.function.processing;

import java.util.function.Function;

/**
 * 항공편 정보 추출 Function
 *
 * TODO: 구현 필요
 * 담당: [Media 개발자]
 *
 * 기능: OCR 결과에서 항공편 정보 파싱
 */
public class ExtractFlightInfoFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: ExtractFlightInfoFunction.java"

# ExtractHotelInfoFunction.java
cat > $SRC_ROOT/domain/chat/function/processing/ExtractHotelInfoFunction.java << 'EOF'
package com.compass.domain.chat.function.processing;

import java.util.function.Function;

/**
 * 호텔 정보 추출 Function
 *
 * TODO: 구현 필요
 * 담당: [Media 개발자]
 *
 * 기능: OCR 결과에서 호텔 예약 정보 파싱
 */
public class ExtractHotelInfoFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: ExtractHotelInfoFunction.java"

# SearchDestinationsFunction.java
cat > $SRC_ROOT/domain/chat/function/external/SearchDestinationsFunction.java << 'EOF'
package com.compass.domain.chat.function.external;

import java.util.function.Function;

/**
 * 목적지 검색 Function
 *
 * TODO: 구현 필요
 * 담당: [Trip 개발자]
 *
 * 기능: 여행지 정보 검색 (Perplexity/Tour API 연동)
 */
public class SearchDestinationsFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: SearchDestinationsFunction.java"

# ContinueFollowUpFunction.java
cat > $SRC_ROOT/domain/chat/function/collection/ContinueFollowUpFunction.java << 'EOF'
package com.compass.domain.chat.function.collection;

import java.util.function.Function;

/**
 * Follow-up 질문 계속 Function
 *
 * TODO: 구현 필요
 * 담당: [User 개발자]
 *
 * 기능: 추가 정보 수집을 위한 연속 질문
 */
public class ContinueFollowUpFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: ContinueFollowUpFunction.java"

# ProcessOCRFunction.java
cat > $SRC_ROOT/domain/chat/function/processing/ProcessOCRFunction.java << 'EOF'
package com.compass.domain.chat.function.processing;

import java.util.function.Function;

/**
 * OCR 메인 처리 Function
 *
 * TODO: 구현 필요
 * 담당: [Media 개발자]
 *
 * 기능: 이미지에서 텍스트 추출 (Google Vision API)
 */
public class ProcessOCRFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: ProcessOCRFunction.java"

# ProvideGeneralInfoFunction.java
cat > $SRC_ROOT/domain/chat/function/processing/ProvideGeneralInfoFunction.java << 'EOF'
package com.compass.domain.chat.function.processing;

import java.util.function.Function;

/**
 * 일반 정보 제공 Function
 *
 * TODO: 구현 필요
 * 담당: [Chat 개발자]
 *
 * 기능: 여행과 무관한 일반 정보 제공
 */
public class ProvideGeneralInfoFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: ProvideGeneralInfoFunction.java"

# SearchWithPerplexityFunction.java
cat > $SRC_ROOT/domain/chat/function/external/SearchWithPerplexityFunction.java << 'EOF'
package com.compass.domain.chat.function.external;

import java.util.function.Function;

/**
 * Perplexity 검색 Function
 *
 * TODO: 구현 필요
 * 담당: [Trip 개발자]
 *
 * 기능: Perplexity API를 통한 실시간 검색
 */
public class SearchWithPerplexityFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: SearchWithPerplexityFunction.java"

# SearchTourAPIFunction.java
cat > $SRC_ROOT/domain/chat/function/external/SearchTourAPIFunction.java << 'EOF'
package com.compass.domain.chat.function.external;

import java.util.function.Function;

/**
 * 한국관광공사 API 검색 Function
 *
 * TODO: 구현 필요
 * 담당: [Trip 개발자]
 *
 * 기능: 관광공사 API를 통한 관광지 정보 검색
 */
public class SearchTourAPIFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: SearchTourAPIFunction.java"

# HandleUnknownFunction.java
cat > $SRC_ROOT/domain/chat/function/processing/HandleUnknownFunction.java << 'EOF'
package com.compass.domain.chat.function.processing;

import java.util.function.Function;

/**
 * 알 수 없는 의도 처리 Function
 *
 * TODO: 구현 필요
 * 담당: [Chat2 개발자]
 *
 * 기능: 분류되지 않은 요청 처리
 */
public class HandleUnknownFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: HandleUnknownFunction.java"

# ModifyTravelPlanFunction.java
cat > $SRC_ROOT/domain/chat/function/refinement/ModifyTravelPlanFunction.java << 'EOF'
package com.compass.domain.chat.function.refinement;

import java.util.function.Function;

/**
 * 여행 계획 수정 Function
 *
 * TODO: 구현 필요
 * 담당: [Trip 개발자]
 *
 * 기능: 생성된 여행 계획 수정 및 피드백 반영
 */
public class ModifyTravelPlanFunction implements Function<Object, Object> {
    @Override
    public Object apply(Object request) {
        // TODO: 구현
        return null;
    }
}
EOF
echo "  ✓ Created: ModifyTravelPlanFunction.java"

# ========== SERVICE 파일 생성 ==========
echo -e "${YELLOW}⚙️ Creating Service files...${NC}"

# ChatThreadService.java
cat > $SRC_ROOT/domain/chat/service/internal/ChatThreadService.java << 'EOF'
package com.compass.domain.chat.service.internal;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ChatThread CRUD 서비스
 *
 * TODO: 구현 필요
 * 담당: [Chat 개발자]
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatThreadService {

    // private final ChatThreadRepository repository;

    // TODO: getOrCreateThread() 구현
    // TODO: getUserThreads() 구현

}
EOF
echo "  ✓ Created: ChatThreadService.java"

# PerplexityClient.java
cat > $SRC_ROOT/domain/chat/service/external/PerplexityClient.java << 'EOF'
package com.compass.domain.chat.service.external;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Perplexity API 클라이언트
 *
 * TODO: 구현 필요
 * 담당: [Trip 개발자]
 *
 * 기능:
 * - 실시간 검색
 * - 목적지 정보 조회
 */
@Slf4j
@Component
public class PerplexityClient {

    // TODO: search() 메서드 구현

}
EOF
echo "  ✓ Created: PerplexityClient.java"

# S3Client.java
cat > $SRC_ROOT/domain/chat/service/external/S3Client.java << 'EOF'
package com.compass.domain.chat.service.external;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * AWS S3 클라이언트
 *
 * TODO: 구현 필요
 * 담당: [Media 개발자]
 */
@Slf4j
@Component
public class S3Client {

    // TODO: upload() 메서드 구현

}
EOF
echo "  ✓ Created: S3Client.java"

# OCRClient.java
cat > $SRC_ROOT/domain/chat/service/external/OCRClient.java << 'EOF'
package com.compass.domain.chat.service.external;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Vision OCR 클라이언트
 *
 * TODO: 구현 필요
 * 담당: [Media 개발자]
 */
@Slf4j
@Component
public class OCRClient {

    // TODO: extractText() 메서드 구현

}
EOF
echo "  ✓ Created: OCRClient.java"

echo "=================================="
echo -e "${GREEN}✅ All skeleton files created successfully!${NC}"
echo ""
echo -e "${BLUE}📊 Summary:${NC}"
echo "  - Orchestrator: 3 files"
echo "  - Controllers: 2 files"
echo "  - Functions: 19 files (모든 워크플로우 Function 포함 + ModifyTravelPlan)"
echo "  - Models: 7 files (Intent, Phase, Request, Response, Context)"
echo "  - Services: 4 files"
echo "  - Total: 39 skeleton files"
echo ""
echo -e "${BLUE}👥 Team Assignment (5명):${NC}"
echo "  - Chat2 개발자: Orchestrator + Controller + Enums + TravelContext + FunctionConfig (8 files)"
echo "  - User 개발자: Input Form + Follow-up (5개) + Recommend + Models (8 files)"
echo "  - Chat 개발자: General Question + ProvideInfo + HandleUnknown + CRUD + Auth (5 files)"
echo "  - Media 개발자: ProcessImage + ProcessOCR + Extract(Flight/Hotel) + S3/OCR Client + Models (8 files)"
echo "  - Trip 개발자: GeneratePlan + Search(3종) + ModifyPlan + Perplexity + Models (10 files)"
echo ""
echo "Next steps:"
echo "1. Review and assign files to team members"
echo "2. Commit skeleton structure"
echo "3. Start implementation!"