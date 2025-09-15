package com.compass.domain.trip.function;

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.TripDetail;
import com.compass.domain.trip.TripStatus;
import com.compass.domain.trip.function.model.TravelPlanModifyRequest;
import com.compass.domain.trip.function.model.TravelPlanResponse;
import com.compass.domain.trip.repository.TripRepository;
import com.compass.domain.trip.repository.TourPlaceRepository;
import com.compass.domain.trip.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 여행 계획 수정 Function
 * REQ-TRIP-001: 여행 계획 생성 Function
 * 
 * MainLLMOrchestrator에서 호출되는 순수 Function입니다.
 * 완성된 수정 정보를 받아서 기존 계획을 업데이트합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TravelPlanModifyFunction {

    private final TripRepository tripRepository;
    private final TourPlaceRepository tourPlaceRepository;
    private final SearchService searchService;

    /**
     * 여행 계획 수정 실행
     * @param request 완성된 여행 계획 수정 요청
     * @return 수정된 여행 계획 응답
     */
    @Transactional
    public TravelPlanResponse execute(TravelPlanModifyRequest request) {
        try {
            log.info("여행 계획 수정 시작: tripId={}, modifyType={}", 
                    request.getTripId(), request.getModifyType());

            // 1. 기존 여행 계획 조회
            Trip trip = tripRepository.findByIdWithDetails(request.getTripId())
                    .orElseThrow(() -> new IllegalArgumentException("여행 계획을 찾을 수 없습니다. ID: " + request.getTripId()));

            // 2. 권한 확인
            if (!trip.getUser().getId().equals(request.getUserId())) {
                throw new IllegalArgumentException("해당 여행 계획을 수정할 권한이 없습니다.");
            }

            // 3. 수정 타입에 따른 처리
            switch (request.getModifyType()) {
                case CHANGE_DESTINATION:
                    modifyDestination(trip, request);
                    break;
                case CHANGE_DATES:
                    modifyDates(trip, request);
                    break;
                case CHANGE_BUDGET:
                    modifyBudget(trip, request);
                    break;
                case CHANGE_STYLE:
                    modifyStyle(trip, request);
                    break;
                case ADD_PLACES:
                    addPlaces(trip, request);
                    break;
                case REMOVE_PLACES:
                    removePlaces(trip, request);
                    break;
                case REORDER_SCHEDULE:
                    reorderSchedule(trip, request);
                    break;
                case CUSTOM:
                    customModify(trip, request);
                    break;
            }

            // 4. 여행 계획 저장
            Trip savedTrip = tripRepository.save(trip);

            // 5. 응답 생성
            return buildSuccessResponse(savedTrip, request);

        } catch (Exception e) {
            log.error("여행 계획 수정 실패: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    /**
     * 목적지 변경
     */
    private void modifyDestination(Trip trip, TravelPlanModifyRequest request) {
        if (request.getNewDestination() == null || request.getNewDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("새로운 목적지를 입력해주세요.");
        }

        String oldDestination = trip.getDestination();
        trip.setDestination(request.getNewDestination());
        
        // 제목 업데이트
        String newTitle = String.format("%s %d박 %d일 여행", 
                request.getNewDestination(), 
                trip.getStartDate().until(trip.getEndDate()).getDays(),
                trip.getStartDate().until(trip.getEndDate()).getDays() + 1);
        trip.setTitle(newTitle);

        // 기존 일정을 새 목적지에 맞게 업데이트
        updateScheduleForNewDestination(trip, request.getNewDestination());

        log.info("목적지 변경: {} -> {}", oldDestination, request.getNewDestination());
    }

    /**
     * 날짜 변경
     */
    private void modifyDates(Trip trip, TravelPlanModifyRequest request) {
        if (request.getNewStartDate() == null || request.getNewEndDate() == null) {
            throw new IllegalArgumentException("새로운 시작일과 종료일을 입력해주세요.");
        }

        if (request.getNewStartDate().isAfter(request.getNewEndDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
        }

        LocalDate oldStartDate = trip.getStartDate();
        LocalDate oldEndDate = trip.getEndDate();
        
        trip.setStartDate(request.getNewStartDate());
        trip.setEndDate(request.getNewEndDate());

        // 제목 업데이트
        String newTitle = String.format("%s %d박 %d일 여행", 
                trip.getDestination(), 
                request.getNewStartDate().until(request.getNewEndDate()).getDays(),
                request.getNewStartDate().until(request.getNewEndDate()).getDays() + 1);
        trip.setTitle(newTitle);

        // 기존 일정을 새 날짜에 맞게 조정
        adjustScheduleForNewDates(trip, oldStartDate, oldEndDate, request.getNewStartDate(), request.getNewEndDate());

        log.info("날짜 변경: {} ~ {} -> {} ~ {}", oldStartDate, oldEndDate, request.getNewStartDate(), request.getNewEndDate());
    }

    /**
     * 예산 변경
     */
    private void modifyBudget(Trip trip, TravelPlanModifyRequest request) {
        if (request.getNewBudget() == null || request.getNewBudget() <= 0) {
            throw new IllegalArgumentException("새로운 예산을 올바르게 입력해주세요.");
        }

        Integer oldBudget = trip.getTotalBudget();
        trip.setTotalBudget(request.getNewBudget());

        log.info("예산 변경: {}원 -> {}원", oldBudget, request.getNewBudget());
    }

    /**
     * 여행 스타일 변경
     */
    private void modifyStyle(Trip trip, TravelPlanModifyRequest request) {
        if (request.getNewTravelStyle() == null || request.getNewTravelStyle().trim().isEmpty()) {
            throw new IllegalArgumentException("새로운 여행 스타일을 입력해주세요.");
        }

        // 여행 스타일 변경에 따른 일정 재구성
        updateScheduleForNewStyle(trip, request.getNewTravelStyle());

        log.info("여행 스타일 변경: {}", request.getNewTravelStyle());
    }

    /**
     * 장소 추가
     */
    private void addPlaces(Trip trip, TravelPlanModifyRequest request) {
        if (request.getPlacesToAdd() == null || request.getPlacesToAdd().isEmpty()) {
            throw new IllegalArgumentException("추가할 장소를 입력해주세요.");
        }

        for (String placeName : request.getPlacesToAdd()) {
            addPlaceToSchedule(trip, placeName);
        }

        log.info("장소 추가: {}", request.getPlacesToAdd());
    }

    /**
     * 장소 제거
     */
    private void removePlaces(Trip trip, TravelPlanModifyRequest request) {
        if (request.getPlacesToRemove() == null || request.getPlacesToRemove().isEmpty()) {
            throw new IllegalArgumentException("제거할 장소를 입력해주세요.");
        }

        for (String placeName : request.getPlacesToRemove()) {
            removePlaceFromSchedule(trip, placeName);
        }

        log.info("장소 제거: {}", request.getPlacesToRemove());
    }

    /**
     * 일정 재정렬
     */
    private void reorderSchedule(Trip trip, TravelPlanModifyRequest request) {
        // 사용자 요청에 따라 일정을 재정렬
        // 실제 구현에서는 request.getModifyContent()를 파싱하여 재정렬 로직 수행
        log.info("일정 재정렬: {}", request.getModifyContent());
    }

    /**
     * 사용자 정의 수정
     */
    private void customModify(Trip trip, TravelPlanModifyRequest request) {
        // 사용자의 자유로운 수정 요청 처리
        // request.getModifyContent()를 파싱하여 다양한 수정 사항 처리
        log.info("사용자 정의 수정: {}", request.getModifyContent());
    }

    /**
     * 새 목적지에 맞게 일정 업데이트
     */
    private void updateScheduleForNewDestination(Trip trip, String newDestination) {
        // 기존 일정을 모두 제거
        trip.getDetails().clear();

        // 새 목적지에 맞는 장소 검색 및 일정 생성
        try {
            var places = searchService.searchByName(newDestination);
            
            LocalDate currentDate = trip.getStartDate();
            int dayNumber = 1;
            
            while (!currentDate.isAfter(trip.getEndDate())) {
                for (int i = 0; i < Math.min(places.size(), 2); i++) { // 하루 최대 2개 장소
                    var place = places.get(i);
                    
                    TripDetail detail = TripDetail.builder()
                            .dayNumber(dayNumber)
                            .activityDate(currentDate)
                            .activityTime(LocalTime.of(9 + i * 4, 0))
                            .placeName(place.getName())
                            .category(place.getCategory())
                            .description(String.format("%s에서 즐길 수 있는 %s 활동", place.getName(), place.getCategory()))
                            .estimatedCost(10000)
                            .address(place.getAddress())
                            .latitude(place.getLatitude())
                            .longitude(place.getLongitude())
                            .displayOrder(i + 1)
                            .build();
                    
                    trip.addDetail(detail);
                }
                
                currentDate = currentDate.plusDays(1);
                dayNumber++;
            }
        } catch (Exception e) {
            log.warn("새 목적지 장소 검색 실패: {}", e.getMessage());
        }
    }

    /**
     * 새 날짜에 맞게 일정 조정
     */
    private void adjustScheduleForNewDates(Trip trip, LocalDate oldStartDate, LocalDate oldEndDate, 
                                         LocalDate newStartDate, LocalDate newEndDate) {
        // 기존 일정을 새 날짜에 맞게 조정
        List<TripDetail> details = new ArrayList<>(trip.getDetails());
        trip.getDetails().clear();

        int dayOffset = (int) oldStartDate.until(newStartDate).getDays();
        
        for (TripDetail detail : details) {
            LocalDate newActivityDate = detail.getActivityDate().plusDays(dayOffset);
            
            // 새 날짜 범위 내에 있는 일정만 유지
            if (!newActivityDate.isBefore(newStartDate) && !newActivityDate.isAfter(newEndDate)) {
                detail.setActivityDate(newActivityDate);
                trip.addDetail(detail);
            }
        }
    }

    /**
     * 새 여행 스타일에 맞게 일정 업데이트
     */
    private void updateScheduleForNewStyle(Trip trip, String newTravelStyle) {
        // 여행 스타일에 따른 장소 카테고리 필터링
        String categoryFilter = getCategoryForTravelStyle(newTravelStyle);
        
        try {
            var places = searchService.getByCategory(categoryFilter);
            
            // 기존 일정을 새 스타일에 맞게 업데이트
            for (TripDetail detail : trip.getDetails()) {
                if (!places.isEmpty()) {
                    var place = places.get(0); // 첫 번째 장소로 교체
                    detail.setPlaceName(place.getName());
                    detail.setCategory(place.getCategory());
                    detail.setDescription(String.format("%s 스타일에 맞는 %s 활동", newTravelStyle, place.getCategory()));
                }
            }
        } catch (Exception e) {
            log.warn("여행 스타일별 장소 검색 실패: {}", e.getMessage());
        }
    }

    /**
     * 일정에 장소 추가
     */
    private void addPlaceToSchedule(Trip trip, String placeName) {
        try {
            var places = searchService.searchByName(placeName);
            if (!places.isEmpty()) {
                var place = places.get(0);
                
                // 가장 적은 일정을 가진 날에 추가
                int targetDay = findDayWithLeastActivities(trip);
                
                TripDetail detail = TripDetail.builder()
                        .dayNumber(targetDay)
                        .activityDate(trip.getStartDate().plusDays(targetDay - 1))
                        .activityTime(LocalTime.of(15, 0))
                        .placeName(place.getName())
                        .category(place.getCategory())
                        .description(String.format("추가된 장소: %s", place.getName()))
                        .estimatedCost(10000)
                        .address(place.getAddress())
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .displayOrder(getNextDisplayOrder(trip, targetDay))
                        .build();
                
                trip.addDetail(detail);
            }
        } catch (Exception e) {
            log.warn("장소 추가 실패: {}", e.getMessage());
        }
    }

    /**
     * 일정에서 장소 제거
     */
    private void removePlaceFromSchedule(Trip trip, String placeName) {
        trip.getDetails().removeIf(detail -> detail.getPlaceName().contains(placeName));
    }

    /**
     * 여행 스타일에 따른 카테고리 매핑
     */
    private String getCategoryForTravelStyle(String travelStyle) {
        return switch (travelStyle.toLowerCase()) {
            case "문화관광" -> "문화시설";
            case "자연여행" -> "관광지";
            case "액티비티" -> "레포츠";
            case "쇼핑" -> "쇼핑";
            case "음식" -> "음식점";
            default -> "관광지";
        };
    }

    /**
     * 가장 적은 일정을 가진 날 찾기
     */
    private int findDayWithLeastActivities(Trip trip) {
        return trip.getDetails().stream()
                .collect(Collectors.groupingBy(TripDetail::getDayNumber, Collectors.counting()))
                .entrySet().stream()
                .min((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
                .map(entry -> entry.getKey())
                .orElse(1);
    }

    /**
     * 다음 표시 순서 계산
     */
    private Integer getNextDisplayOrder(Trip trip, int dayNumber) {
        return trip.getDetails().stream()
                .filter(detail -> detail.getDayNumber().equals(dayNumber))
                .mapToInt(detail -> detail.getDisplayOrder() != null ? detail.getDisplayOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    /**
     * 성공 응답 생성
     */
    private TravelPlanResponse buildSuccessResponse(Trip trip, TravelPlanModifyRequest request) {
        List<TravelPlanResponse.DailyPlan> dailyPlans = buildDailyPlansFromTrip(trip);
        
        int totalEstimatedCost = dailyPlans.stream()
                .mapToInt(TravelPlanResponse.DailyPlan::getDailyCost)
                .sum();

        String summary = String.format("여행 계획이 성공적으로 수정되었습니다. %s에서 %d박 %d일 여행, 총 예상 비용: %d원",
                trip.getDestination(),
                trip.getStartDate().until(trip.getEndDate()).getDays(),
                trip.getStartDate().until(trip.getEndDate()).getDays() + 1,
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
                .dailyPlans(dailyPlans)
                .summary(summary)
                .status("SUCCESS")
                .message("여행 계획이 성공적으로 수정되었습니다")
                .build();
    }

    /**
     * Trip 엔티티에서 DailyPlan 리스트 생성
     */
    private List<TravelPlanResponse.DailyPlan> buildDailyPlansFromTrip(Trip trip) {
        return trip.getDetails().stream()
                .collect(Collectors.groupingBy(TripDetail::getDayNumber))
                .entrySet().stream()
                .map(entry -> {
                    int dayNumber = entry.getKey();
                    List<TripDetail> dayDetails = entry.getValue();
                    
                    List<TravelPlanResponse.Activity> activities = dayDetails.stream()
                            .map(this::convertToActivity)
                            .collect(Collectors.toList());
                    
                    int dailyCost = activities.stream()
                            .mapToInt(activity -> activity.getEstimatedCost() != null ? activity.getEstimatedCost() : 0)
                            .sum();
                    
                    return TravelPlanResponse.DailyPlan.builder()
                            .dayNumber(dayNumber)
                            .date(trip.getStartDate().plusDays(dayNumber - 1))
                            .activities(activities)
                            .dailyCost(dailyCost)
                            .build();
                })
                .sorted((p1, p2) -> Integer.compare(p1.getDayNumber(), p2.getDayNumber()))
                .collect(Collectors.toList());
    }

    /**
     * TripDetail을 Activity로 변환
     */
    private TravelPlanResponse.Activity convertToActivity(TripDetail detail) {
        return TravelPlanResponse.Activity.builder()
                .placeName(detail.getPlaceName())
                .category(detail.getCategory())
                .startTime(detail.getActivityTime())
                .endTime(detail.getActivityTime() != null ? detail.getActivityTime().plusHours(3) : null)
                .description(detail.getDescription())
                .estimatedCost(detail.getEstimatedCost())
                .address(detail.getAddress())
                .latitude(detail.getLatitude())
                .longitude(detail.getLongitude())
                .tips(detail.getTips())
                .build();
    }

    /**
     * 오류 응답 생성
     */
    private TravelPlanResponse buildErrorResponse(String errorMessage) {
        return TravelPlanResponse.builder()
                .status("ERROR")
                .errorCode("TRIP_002")
                .message(errorMessage)
                .build();
    }
}
