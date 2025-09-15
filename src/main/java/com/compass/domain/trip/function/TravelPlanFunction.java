package com.compass.domain.trip.function;

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.TripDetail;
import com.compass.domain.trip.function.model.TravelPlanRequest;
import com.compass.domain.trip.function.model.TravelPlanResponse;
import com.compass.domain.trip.repository.TripRepository;
import com.compass.domain.trip.repository.TourPlaceRepository;
import com.compass.domain.trip.service.SearchService;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 여행 계획 생성 Function
 * REQ-TRIP-001: 여행 계획 생성 Function
 * 
 * MainLLMOrchestrator에서 호출되는 순수 Function입니다.
 * 완성된 여행 정보를 받아서 상세한 일정을 생성합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TravelPlanFunction {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TourPlaceRepository tourPlaceRepository;
    private final SearchService searchService;

    /**
     * 여행 계획 생성 실행
     * @param request 완성된 여행 계획 생성 요청
     * @return 생성된 여행 계획 응답
     */
    @Transactional
    public TravelPlanResponse execute(TravelPlanRequest request) {
        try {
            log.info("여행 계획 생성 시작: destination={}, startDate={}, endDate={}", 
                    request.getDestination(), request.getStartDate(), request.getEndDate());

            // 1. 사용자 조회
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + request.getUserId()));

            // 2. 여행 계획 엔티티 생성
            Trip trip = createTripEntity(request, user);

            // 3. 장소 검색 및 일정 생성
            List<TravelPlanResponse.DailyPlan> dailyPlans = createDailyPlans(request, trip);

            // 4. 여행 계획 저장
            Trip savedTrip = tripRepository.save(trip);

            // 5. 응답 생성
            return buildSuccessResponse(savedTrip, dailyPlans, request);

        } catch (Exception e) {
            log.error("여행 계획 생성 실패: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    /**
     * 여행 계획 엔티티 생성
     */
    private Trip createTripEntity(TravelPlanRequest request, User user) {
        String title = String.format("%s %d박 %d일 여행", 
                request.getDestination(), 
                request.getStartDate().until(request.getEndDate()).getDays(),
                request.getStartDate().until(request.getEndDate()).getDays() + 1);

        return Trip.builder()
                .user(user)
                .threadId(request.getThreadId())
                .title(title)
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .numberOfPeople(request.getNumberOfPeople())
                .totalBudget(request.getTotalBudget())
                .status(com.compass.domain.trip.TripStatus.PLANNING)
                .tripMetadata(buildTripMetadata(request))
                .build();
    }

    /**
     * 일별 상세 일정 생성
     */
    private List<TravelPlanResponse.DailyPlan> createDailyPlans(TravelPlanRequest request, Trip trip) {
        List<TravelPlanResponse.DailyPlan> dailyPlans = new ArrayList<>();
        
        LocalDate currentDate = request.getStartDate();
        int dayNumber = 1;
        
        while (!currentDate.isAfter(request.getEndDate())) {
            // 해당 날짜의 장소 검색
            List<TravelPlanResponse.Activity> activities = searchPlacesForDay(request, currentDate);
            
            // 일일 계획 생성
            TravelPlanResponse.DailyPlan dailyPlan = TravelPlanResponse.DailyPlan.builder()
                    .dayNumber(dayNumber)
                    .date(currentDate)
                    .activities(activities)
                    .dailyCost(calculateDailyCost(activities))
                    .build();
            
            dailyPlans.add(dailyPlan);
            
            // TripDetail 엔티티 생성 및 저장
            createTripDetails(trip, dailyPlan);
            
            currentDate = currentDate.plusDays(1);
            dayNumber++;
        }
        
        return dailyPlans;
    }

    /**
     * 해당 날짜의 장소 검색
     */
    private List<TravelPlanResponse.Activity> searchPlacesForDay(TravelPlanRequest request, LocalDate date) {
        List<TravelPlanResponse.Activity> activities = new ArrayList<>();
        
        try {
            // DB에서 장소 검색 (우선순위: DB 우선)
            var places = searchService.searchByName(request.getDestination());
            
            // 검색된 장소를 활동으로 변환
            for (int i = 0; i < Math.min(places.size(), 3); i++) { // 하루 최대 3개 장소
                var place = places.get(i);
                
                TravelPlanResponse.Activity activity = TravelPlanResponse.Activity.builder()
                        .placeName(place.getName())
                        .category(place.getCategory())
                        .startTime(LocalTime.of(9 + i * 3, 0)) // 9시, 12시, 15시
                        .endTime(LocalTime.of(12 + i * 3, 0))   // 12시, 15시, 18시
                        .description(buildActivityDescription(place))
                        .estimatedCost(estimateActivityCost(place))
                        .address(place.getAddress())
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .tips(buildActivityTips(place))
                        .build();
                
                activities.add(activity);
            }
            
        } catch (Exception e) {
            log.warn("장소 검색 실패, 기본 활동 생성: {}", e.getMessage());
            // 검색 실패 시 기본 활동 생성
            activities.add(createDefaultActivity(date));
        }
        
        return activities;
    }

    /**
     * TripDetail 엔티티 생성
     */
    private void createTripDetails(Trip trip, TravelPlanResponse.DailyPlan dailyPlan) {
        for (TravelPlanResponse.Activity activity : dailyPlan.getActivities()) {
            TripDetail detail = TripDetail.builder()
                    .dayNumber(dailyPlan.getDayNumber())
                    .activityDate(dailyPlan.getDate())
                    .activityTime(activity.getStartTime())
                    .placeName(activity.getPlaceName())
                    .category(activity.getCategory())
                    .description(activity.getDescription())
                    .estimatedCost(activity.getEstimatedCost())
                    .address(activity.getAddress())
                    .latitude(activity.getLatitude())
                    .longitude(activity.getLongitude())
                    .tips(activity.getTips())
                    .displayOrder(dailyPlan.getActivities().indexOf(activity) + 1)
                    .build();
            
            trip.addDetail(detail);
        }
    }

    /**
     * 성공 응답 생성
     */
    private TravelPlanResponse buildSuccessResponse(Trip trip, List<TravelPlanResponse.DailyPlan> dailyPlans, TravelPlanRequest request) {
        int totalEstimatedCost = dailyPlans.stream()
                .mapToInt(TravelPlanResponse.DailyPlan::getDailyCost)
                .sum();

        String summary = String.format("%s에서 %d박 %d일 여행 계획이 생성되었습니다. 총 %d개의 장소를 방문하며, 예상 비용은 %d원입니다.",
                request.getDestination(),
                request.getStartDate().until(request.getEndDate()).getDays(),
                request.getStartDate().until(request.getEndDate()).getDays() + 1,
                dailyPlans.stream().mapToInt(plan -> plan.getActivities().size()).sum(),
                totalEstimatedCost);

        return TravelPlanResponse.builder()
                .tripId(trip.getId())
                .tripUuid(trip.getTripUuid().toString())
                .title(trip.getTitle())
                .destination(trip.getDestination())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .numberOfPeople(trip.getNumberOfPeople())
                .totalBudget(trip.getTotalBudget())
                .estimatedCost(totalEstimatedCost)
                .travelStyle(request.getTravelStyle())
                .dailyPlans(dailyPlans)
                .summary(summary)
                .status("SUCCESS")
                .message("여행 계획이 성공적으로 생성되었습니다")
                .build();
    }

    /**
     * 오류 응답 생성
     */
    private TravelPlanResponse buildErrorResponse(String errorMessage) {
        return TravelPlanResponse.builder()
                .status("ERROR")
                .errorCode("TRIP_001")
                .message(errorMessage)
                .build();
    }

    // 헬퍼 메서드들
    private String buildTripMetadata(TravelPlanRequest request) {
        return String.format("{\"travelStyle\":\"%s\",\"preferredCategories\":%s,\"specialRequests\":\"%s\"}",
                request.getTravelStyle(),
                request.getPreferredCategories() != null ? request.getPreferredCategories().toString() : "[]",
                request.getSpecialRequests() != null ? request.getSpecialRequests() : "");
    }

    private String buildActivityDescription(com.compass.domain.trip.entity.TourPlace place) {
        return String.format("%s에서 즐길 수 있는 %s 활동입니다.", place.getName(), place.getCategory());
    }

    private Integer estimateActivityCost(com.compass.domain.trip.entity.TourPlace place) {
        // 카테고리별 기본 비용 추정
        return switch (place.getCategory()) {
            case "관광지" -> 10000;
            case "음식점" -> 15000;
            case "쇼핑" -> 50000;
            case "문화시설" -> 8000;
            default -> 10000;
        };
    }

    private String buildActivityTips(com.compass.domain.trip.entity.TourPlace place) {
        return String.format("%s 방문 시 참고사항: 주소 - %s", place.getName(), place.getAddress());
    }

    private Integer calculateDailyCost(List<TravelPlanResponse.Activity> activities) {
        return activities.stream()
                .mapToInt(activity -> activity.getEstimatedCost() != null ? activity.getEstimatedCost() : 0)
                .sum();
    }

    private TravelPlanResponse.Activity createDefaultActivity(LocalDate date) {
        return TravelPlanResponse.Activity.builder()
                .placeName("자유 시간")
                .category("휴식")
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .description("자유롭게 휴식하거나 주변을 둘러보는 시간입니다.")
                .estimatedCost(0)
                .tips("여유롭게 즐기세요!")
                .build();
    }
}
