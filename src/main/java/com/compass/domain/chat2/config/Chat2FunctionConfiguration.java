package com.compass.domain.chat2.config;

import com.compass.domain.chat2.model.Intent;
import com.compass.domain.chat2.service.IntentClassificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.ai.function.FunctionCallback; // Spring AI Function Calling은 나중에 구현
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Description; // Spring AI Function Calling은 나중에 구현

import java.util.function.Function;

/**
 * CHAT2 도메인 Function Configuration
 * REQ-CHAT2-005: 부족 정보 확인 Function (checkMissingInfo)
 * REQ-CHAT2-006: 부족 정보 채우기 Function (fillMissingInfo)
 * 
 * CHAT2 도메인에서 제공하는 Function들을 정의합니다.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class Chat2FunctionConfiguration {
    
    private final IntentClassificationService intentClassificationService;
    
    /**
     * 부족 정보 확인 Function
     * REQ-CHAT2-005: 부족 정보 확인 Function (checkMissingInfo)
     */
    @Bean("checkMissingInfo")
    // @Description("여행 계획에 필요한 정보가 부족한지 확인하고 부족한 정보를 알려줍니다") // Spring AI Function Calling은 나중에 구현
    public Function<MissingInfoRequest, MissingInfoResponse> checkMissingInfo() {
        return request -> {
            log.info("부족 정보 확인 Function 실행: {}", request);
            
            try {
                // 필수 정보 체크
                boolean hasDestination = request.getDestination() != null && !request.getDestination().trim().isEmpty();
                boolean hasStartDate = request.getStartDate() != null;
                boolean hasEndDate = request.getEndDate() != null;
                boolean hasNumberOfPeople = request.getNumberOfPeople() != null && request.getNumberOfPeople() > 0;
                boolean hasBudget = request.getBudget() != null && request.getBudget() > 0;
                boolean hasTravelStyle = request.getTravelStyle() != null && !request.getTravelStyle().trim().isEmpty();
                
                // 부족한 정보 수집
                StringBuilder missingInfo = new StringBuilder();
                if (!hasDestination) missingInfo.append("목적지, ");
                if (!hasStartDate) missingInfo.append("출발일, ");
                if (!hasEndDate) missingInfo.append("도착일, ");
                if (!hasNumberOfPeople) missingInfo.append("인원수, ");
                if (!hasBudget) missingInfo.append("예산, ");
                if (!hasTravelStyle) missingInfo.append("여행 스타일, ");
                
                boolean isComplete = hasDestination && hasStartDate && hasEndDate && 
                                   hasNumberOfPeople && hasBudget && hasTravelStyle;
                
                String message;
                if (isComplete) {
                    message = "모든 필수 정보가 완성되었습니다. 여행 계획을 생성하겠습니다.";
                } else {
                    message = "다음 정보가 부족합니다: " + missingInfo.toString().replaceAll(", $", "");
                }
                
                return MissingInfoResponse.builder()
                        .status("SUCCESS")
                        .isComplete(isComplete)
                        .missingInfo(missingInfo.toString().replaceAll(", $", ""))
                        .message(message)
                        .build();
                        
            } catch (Exception e) {
                log.error("부족 정보 확인 Function 오류: {}", e.getMessage(), e);
                return MissingInfoResponse.builder()
                        .status("ERROR")
                        .errorCode("CHAT2_001")
                        .message("정보 확인 중 오류가 발생했습니다.")
                        .build();
            }
        };
    }
    
    /**
     * 부족 정보 채우기 Function
     * REQ-CHAT2-006: 부족 정보 채우기 Function (fillMissingInfo)
     */
    @Bean("fillMissingInfo")
    // @Description("부족한 정보를 사용자로부터 수집하기 위한 질문을 생성합니다") // Spring AI Function Calling은 나중에 구현
    public Function<FillInfoRequest, FillInfoResponse> fillMissingInfo() {
        return request -> {
            log.info("부족 정보 채우기 Function 실행: {}", request);
            
            try {
                String question = generateFollowUpQuestion(request.getMissingInfo());
                
                return FillInfoResponse.builder()
                        .status("SUCCESS")
                        .question(question)
                        .message("추가 정보를 수집하겠습니다.")
                        .build();
                        
            } catch (Exception e) {
                log.error("부족 정보 채우기 Function 오류: {}", e.getMessage(), e);
                return FillInfoResponse.builder()
                        .status("ERROR")
                        .errorCode("CHAT2_002")
                        .message("정보 수집 중 오류가 발생했습니다.")
                        .build();
            }
        };
    }
    
    /**
     * Follow-up 질문 생성
     */
    private String generateFollowUpQuestion(String missingInfo) {
        if (missingInfo.contains("목적지")) {
            return "어느 도시로 여행을 가시나요?";
        } else if (missingInfo.contains("출발일")) {
            return "언제 출발하시나요? (YYYY-MM-DD 형식으로 알려주세요)";
        } else if (missingInfo.contains("도착일")) {
            return "언제 돌아오시나요? (YYYY-MM-DD 형식으로 알려주세요)";
        } else if (missingInfo.contains("인원수")) {
            return "몇 명이 함께 가시나요?";
        } else if (missingInfo.contains("예산")) {
            return "예산은 얼마 정도로 생각하고 계신가요? (원 단위)";
        } else if (missingInfo.contains("여행 스타일")) {
            return "어떤 스타일의 여행을 원하시나요? (예: 문화탐방, 자연여행, 쇼핑, 휴양 등)";
        } else {
            return "추가 정보가 필요합니다. 구체적으로 알려주세요.";
        }
    }
    
    /**
     * 부족 정보 확인 요청 DTO
     */
    public static class MissingInfoRequest {
        private String destination;
        private String startDate;
        private String endDate;
        private Integer numberOfPeople;
        private Integer budget;
        private String travelStyle;
        
        // Getters and Setters
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        
        public Integer getNumberOfPeople() { return numberOfPeople; }
        public void setNumberOfPeople(Integer numberOfPeople) { this.numberOfPeople = numberOfPeople; }
        
        public Integer getBudget() { return budget; }
        public void setBudget(Integer budget) { this.budget = budget; }
        
        public String getTravelStyle() { return travelStyle; }
        public void setTravelStyle(String travelStyle) { this.travelStyle = travelStyle; }
    }
    
    /**
     * 부족 정보 확인 응답 DTO
     */
    public static class MissingInfoResponse {
        private String status;
        private String errorCode;
        private String message;
        private boolean isComplete;
        private String missingInfo;
        
        public static MissingInfoResponseBuilder builder() {
            return new MissingInfoResponseBuilder();
        }
        
        public static class MissingInfoResponseBuilder {
            private String status;
            private String errorCode;
            private String message;
            private boolean isComplete;
            private String missingInfo;
            
            public MissingInfoResponseBuilder status(String status) { this.status = status; return this; }
            public MissingInfoResponseBuilder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
            public MissingInfoResponseBuilder message(String message) { this.message = message; return this; }
            public MissingInfoResponseBuilder isComplete(boolean isComplete) { this.isComplete = isComplete; return this; }
            public MissingInfoResponseBuilder missingInfo(String missingInfo) { this.missingInfo = missingInfo; return this; }
            
            public MissingInfoResponse build() {
                MissingInfoResponse response = new MissingInfoResponse();
                response.status = this.status;
                response.errorCode = this.errorCode;
                response.message = this.message;
                response.isComplete = this.isComplete;
                response.missingInfo = this.missingInfo;
                return response;
            }
        }
        
        // Getters
        public String getStatus() { return status; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public boolean isComplete() { return isComplete; }
        public String getMissingInfo() { return missingInfo; }
    }
    
    /**
     * 부족 정보 채우기 요청 DTO
     */
    public static class FillInfoRequest {
        private String missingInfo;
        
        public String getMissingInfo() { return missingInfo; }
        public void setMissingInfo(String missingInfo) { this.missingInfo = missingInfo; }
    }
    
    /**
     * 부족 정보 채우기 응답 DTO
     */
    public static class FillInfoResponse {
        private String status;
        private String errorCode;
        private String message;
        private String question;
        
        public static FillInfoResponseBuilder builder() {
            return new FillInfoResponseBuilder();
        }
        
        public static class FillInfoResponseBuilder {
            private String status;
            private String errorCode;
            private String message;
            private String question;
            
            public FillInfoResponseBuilder status(String status) { this.status = status; return this; }
            public FillInfoResponseBuilder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
            public FillInfoResponseBuilder message(String message) { this.message = message; return this; }
            public FillInfoResponseBuilder question(String question) { this.question = question; return this; }
            
            public FillInfoResponse build() {
                FillInfoResponse response = new FillInfoResponse();
                response.status = this.status;
                response.errorCode = this.errorCode;
                response.message = this.message;
                response.question = this.question;
                return response;
            }
        }
        
        // Getters
        public String getStatus() { return status; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public String getQuestion() { return question; }
    }
}
