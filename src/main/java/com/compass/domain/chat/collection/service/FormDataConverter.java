package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

// 프론트엔드 폼 데이터를 TravelFormSubmitRequest로 변환하는 컨버터
@Slf4j
@Component
@RequiredArgsConstructor
public class FormDataConverter {

    private final ObjectMapper objectMapper;

    /**
     * 프론트엔드 폼 데이터를 TravelFormSubmitRequest로 변환
     *
     * 프론트엔드 폼 데이터 구조:
     * {
     *   "destinations": ["도쿄"],
     *   "departureLocation": "서울",
     *   "travelDates": {
     *     "startDate": "2024-02-01",
     *     "endDate": "2024-02-05"
     *   },
     *   "travelStyle": "휴양",
     *   "budget": "100-200만원",
     *   "travelers": 2,
     *   "companionType": "커플",
     *   "specialRequests": "특별 요청사항"
     *   t
     * }
     */
    @SuppressWarnings("unchecked")
    public TravelFormSubmitRequest convertFromFrontend(String userId, Map<String, Object> formData) {
        try {
            List<String> destinations = null;
            if (formData.containsKey("destinations")) {
                var destValue = formData.get("destinations");
                if (destValue instanceof List) {
                    destinations = (List<String>) destValue;
                } else if (destValue != null) {
                    destinations = List.of(destValue.toString());
                }
            }

            TravelFormSubmitRequest.DateRange dateRange = null;
            if (formData.containsKey("travelDates")) {
                var dates = (Map<String, Object>) formData.get("travelDates");
                if (dates != null && dates.containsKey("startDate") && dates.containsKey("endDate")) {
                    try {
                        var startDate = LocalDate.parse(dates.get("startDate").toString());
                        var endDate = LocalDate.parse(dates.get("endDate").toString());
                        dateRange = new TravelFormSubmitRequest.DateRange(startDate, endDate);
                    } catch (Exception e) {
                        log.error("날짜 파싱 실패: {}", e.getMessage());
                    }
                }
            }

            // [수정] travelDates 객체 바깥에서 departureTime과 endTime을 직접 읽어옵니다.
            LocalTime departureTime = null;
            if (formData.containsKey("departureTime") && formData.get("departureTime") != null) {
                try {
                    departureTime = LocalTime.parse(formData.get("departureTime").toString());
                } catch (Exception e) {
                    log.error("출발 시간 파싱 실패: {}", e.getMessage());
                }
            }

            LocalTime endTime = null;
            if (formData.containsKey("endTime") && formData.get("endTime") != null) {
                try {
                    endTime = LocalTime.parse(formData.get("endTime").toString());
                } catch (Exception e) {
                    log.error("종료 시간 파싱 실패: {}", e.getMessage());
                }
            }

            Long budget = null;
            if (formData.containsKey("budget") && formData.get("budget") != null) {
                String budgetStr = formData.get("budget").toString();
                budget = parseBudgetString(budgetStr);
            }

            List<String> travelStyle = null;
            if (formData.containsKey("travelStyle") && formData.get("travelStyle") != null) {
                var styleValue = formData.get("travelStyle");
                if (styleValue instanceof List) {
                    travelStyle = (List<String>) styleValue;
                } else {
                    travelStyle = List.of(styleValue.toString());
                }
            }

            String companions = formData.containsKey("companions") ?
                    formData.get("companions").toString() : null;
            String departureLocation = formData.containsKey("departureLocation") ?
                    formData.get("departureLocation").toString() : null;
            String reservationDocument = formData.containsKey("reservationDocument") ?
                    (String) formData.get("reservationDocument") : null;

            return new TravelFormSubmitRequest(
                    userId,
                    destinations,
                    departureLocation,
                    dateRange,
                    departureTime,
                    endTime,
                    companions,
                    budget,
                    travelStyle,
                    reservationDocument
            );

        } catch (Exception e) {
            log.error("폼 데이터 변환 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("폼 데이터 변환 실패", e);
        }
    }

    private Long parseBudgetString(String budgetStr) {
        if (budgetStr == null || budgetStr.isEmpty() || "무관".equals(budgetStr)) {
            return null;
        }
        if (budgetStr.contains("50만원 이하")) return 500000L;
        if (budgetStr.contains("50-100만원")) return 750000L;
        if (budgetStr.contains("100-200만원")) return 1500000L;
        if (budgetStr.contains("200만원 이상")) return 3000000L;
        try {
            return Long.parseLong(budgetStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.warn("예산 파싱 실패: {}", budgetStr);
            return null;
        }
    }
}