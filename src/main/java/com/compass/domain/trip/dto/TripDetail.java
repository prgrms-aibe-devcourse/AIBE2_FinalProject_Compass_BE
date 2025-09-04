package com.compass.domain.trip.dto;

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.TripStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "여행 계획 상세 조회 DTO")
public class TripDetail {

    @Schema(description = "여행 계획 상세 조회 응답")
    public record Response(
            @Schema(description = "여행 계획 ID", example = "1") Long id,
            @Schema(description = "여행 계획 UUID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") UUID tripUuid,
            @Schema(description = "사용자 ID", example = "1") Long userId,
            @Schema(description = "채팅 스레드 ID", example = "101") Long threadId,
            @Schema(description = "여행 제목", example = "서울 3박 4일 여행") String title,
            @Schema(description = "목적지", example = "서울") String destination,
            @Schema(description = "여행 시작일", example = "2024-09-01") LocalDate startDate,
            @Schema(description = "여행 종료일", example = "2024-09-04") LocalDate endDate,
            @Schema(description = "여행 인원", example = "2") Integer numberOfPeople,
            @Schema(description = "총 예산", example = "1000000") Integer totalBudget,
            @Schema(description = "여행 상태", example = "PLANNING") TripStatus status,
            @Schema(description = "버전", example = "1") Integer version,
            @Schema(description = "생성일시", example = "2024-08-01T10:00:00") LocalDateTime createdAt,
            @Schema(description = "수정일시", example = "2024-08-01T10:00:00") LocalDateTime updatedAt,
            @Schema(description = "일자별 계획") List<DailyPlan> dailyPlans
    ) {
        public static Response from(Trip trip) {
            // 일자별로 그룹화 - null 체크 추가
            List<com.compass.domain.trip.TripDetail> details = trip.getDetails() != null ? trip.getDetails() : new ArrayList<>();
            
            Map<Integer, List<com.compass.domain.trip.TripDetail>> groupedByDay = details.stream()
                    .filter(detail -> detail.getDayNumber() != null) // null 체크 추가
                    .collect(Collectors.groupingBy(com.compass.domain.trip.TripDetail::getDayNumber));

            List<DailyPlan> dailyPlans = groupedByDay.entrySet().stream()
                    .map(entry -> {
                        Integer dayNumber = entry.getKey();
                        List<com.compass.domain.trip.TripDetail> dayDetails = entry.getValue();
                        
                        // 해당 일차의 활동 날짜 (첫 번째 활동의 날짜를 사용)
                        LocalDate activityDate = dayDetails.isEmpty() ? null : dayDetails.get(0).getActivityDate();
                        
                        List<Activity> activities = dayDetails.stream()
                                .map(Activity::from)
                                .collect(Collectors.toList());
                        
                        return new DailyPlan(dayNumber, activityDate, activities);
                    })
                    .sorted((a, b) -> a.dayNumber().compareTo(b.dayNumber()))
                    .collect(Collectors.toList());

            return new Response(
                    trip.getId(),
                    trip.getTripUuid(),
                    trip.getUser() != null ? trip.getUser().getId() : null,
                    trip.getThreadId(),
                    trip.getTitle(),
                    trip.getDestination(),
                    trip.getStartDate(),
                    trip.getEndDate(),
                    trip.getNumberOfPeople(),
                    trip.getTotalBudget(),
                    trip.getStatus(),
                    trip.getVersion(),
                    trip.getCreatedAt(),
                    trip.getUpdatedAt(),
                    dailyPlans
            );
        }
    }

    @Schema(description = "일자별 계획")
    public record DailyPlan(
            @Schema(description = "여행 일차", example = "1") Integer dayNumber,
            @Schema(description = "활동 날짜", example = "2024-09-01") LocalDate activityDate,
            @Schema(description = "활동 목록") List<Activity> activities
    ) {}

    @Schema(description = "활동 상세")
    public record Activity(
            @Schema(description = "활동 ID", example = "1") Long id,
            @Schema(description = "활동 시간", example = "09:00", type = "string") @JsonFormat(pattern = "HH:mm") LocalTime activityTime,
            @Schema(description = "장소명", example = "경복궁") String placeName,
            @Schema(description = "카테고리", example = "관광지") String category,
            @Schema(description = "활동 설명", example = "조선 왕조의 법궁") String description,
            @Schema(description = "예상 비용", example = "3000") Integer estimatedCost,
            @Schema(description = "주소", example = "서울특별시 종로구 사직로 161") String address,
            @Schema(description = "위도", example = "37.579617") Double latitude,
            @Schema(description = "경도", example = "126.977041") Double longitude,
            @Schema(description = "팁", example = "한복을 입으면 무료 입장") String tips,
            @Schema(description = "표시 순서", example = "1") Integer displayOrder
    ) {
        public static Activity from(com.compass.domain.trip.TripDetail tripDetail) {
            return new Activity(
                    tripDetail.getId(),
                    tripDetail.getActivityTime(),
                    tripDetail.getPlaceName(),
                    tripDetail.getCategory(),
                    tripDetail.getDescription(),
                    tripDetail.getEstimatedCost(),
                    tripDetail.getAddress(),
                    tripDetail.getLatitude(),
                    tripDetail.getLongitude(),
                    tripDetail.getTips(),
                    tripDetail.getDisplayOrder()
            );
        }
    }
}
