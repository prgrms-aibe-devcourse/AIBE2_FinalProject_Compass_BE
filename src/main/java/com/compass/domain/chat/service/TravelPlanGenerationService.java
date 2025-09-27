package com.compass.domain.chat.service;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.stage3.service.Stage3IntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanGenerationService {

    private final Stage3IntegrationService stage3IntegrationService;

    /**
     * Phase 3에서 실제 여행 계획을 생성하는 메인 메서드
     */
    public Map<String, Object> generateTravelPlan(TravelContext context) {
        log.info("🚀 여행 계획 생성 시작 - ThreadId: {}, UserId: {}",
            context.getThreadId(), context.getUserId());

        try {
            // 1. Context에서 여행 정보 추출
            Map<String, Object> travelInfo = extractTravelInfoFromContext(context);
            log.info("📍 여행 정보 추출 완료: {}", travelInfo);

            // 2. Stage 3 - TravelContext를 직접 사용하여 여행 계획 생성
            log.info("📍 Stage 3 시작: 여행 계획 생성");

            // Stage3IntegrationService의 processWithTravelContext 메서드 사용
            var stage3Output = stage3IntegrationService.processWithTravelContext(context);
            log.info("✅ Stage 3 완료: 여행 일정 생성 완료");

            // 3. Stage3Output을 Map으로 변환
            Map<String, Object> travelPlan = new HashMap<>();
            travelPlan.put("status", "SUCCESS");
            travelPlan.put("generatedAt", new Date());
            travelPlan.put("travelInfo", travelInfo);

            if (stage3Output != null) {
                travelPlan.put("dailyItineraries", stage3Output.getDailyItineraries());
                travelPlan.put("optimizedRoutes", stage3Output.getOptimizedRoutes());
                travelPlan.put("totalDistance", stage3Output.getTotalDistance());
                travelPlan.put("totalDuration", stage3Output.getTotalDuration());
                travelPlan.put("statistics", stage3Output.getStatistics());
                travelPlan.put("generatedAt", stage3Output.getGeneratedAt());
                travelPlan.put("message", "🎯 여행 계획이 성공적으로 생성되었습니다!");
            } else {
                // Stage3Output이 null인 경우 기본 메시지 추가
                travelPlan.put("message", "여행 계획 생성이 완료되었습니다.");
                travelPlan.put("dailyItineraries", List.of());
            }

            // 4. Context에 여행 계획 저장
            context.setTravelPlan(travelPlan);
            log.info("✅ 여행 계획이 Context에 저장되었습니다.");

            return travelPlan;

        } catch (Exception e) {
            log.error("❌ 여행 계획 생성 중 오류 발생", e);

            // 오류 발생 시 기본 응답 반환
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "여행 계획 생성 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
            errorResponse.put("error", e.getMessage());

            return errorResponse;
        }
    }

    /**
     * Context에서 여행 정보 추출
     */
    private Map<String, Object> extractTravelInfoFromContext(TravelContext context) {
        Map<String, Object> info = new HashMap<>();

        // FormData에서 정보 추출
        Map<String, Object> formData = context.getFormData();
        if (formData != null) {
            info.put("destinations", formData.getOrDefault("destinations", List.of("서울")));
            info.put("departureLocation", formData.getOrDefault("departureLocation", "서울"));

            // 날짜 처리
            Map<String, Object> travelDates = (Map<String, Object>) formData.get("travelDates");
            if (travelDates != null) {
                info.put("startDate", LocalDate.parse((String) travelDates.get("startDate")));
                info.put("endDate", LocalDate.parse((String) travelDates.get("endDate")));
            } else {
                // 기본값 설정
                info.put("startDate", LocalDate.now());
                info.put("endDate", LocalDate.now().plusDays(2));
            }

            // 시간 처리 - 이미 LocalTime 타입으로 저장되어 있을 수 있음
            Object departureTimeObj = formData.get("departureTime");
            Object endTimeObj = formData.get("endTime");

            if (departureTimeObj instanceof LocalTime) {
                info.put("departureTime", departureTimeObj);
            } else if (departureTimeObj instanceof String) {
                info.put("departureTime", LocalTime.parse((String) departureTimeObj));
            } else {
                info.put("departureTime", LocalTime.of(9, 0));
            }

            if (endTimeObj instanceof LocalTime) {
                info.put("endTime", endTimeObj);
            } else if (endTimeObj instanceof String) {
                info.put("endTime", LocalTime.parse((String) endTimeObj));
            } else {
                info.put("endTime", LocalTime.of(18, 0));
            }

            // 기타 정보
            info.put("travelStyle", formData.getOrDefault("travelStyle", "culture"));
            info.put("budget", formData.getOrDefault("budget", 500000));
            info.put("companions", formData.getOrDefault("companionType", "가족"));
            info.put("travelers", formData.getOrDefault("travelers", 2));
        }

        return info;
    }

}