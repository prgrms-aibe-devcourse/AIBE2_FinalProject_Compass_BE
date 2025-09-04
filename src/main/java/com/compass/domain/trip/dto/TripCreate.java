package com.compass.domain.trip.dto;

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.TripDetail;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "여행 계획 생성 DTO")
public class TripCreate {

    @Schema(description = "여행 계획 생성 요청")
    public record Request(
            @Schema(description = "사용자 ID", example = "1") @NotNull Long userId,
            @Schema(description = "채팅 스레드 ID", example = "101") Long threadId,
            @Schema(description = "여행 제목", example = "서울 3박 4일 여행") @NotBlank String title,
            @Schema(description = "목적지", example = "서울") @NotBlank String destination,
            @Schema(description = "여행 시작일", example = "2024-09-01") @NotNull @FutureOrPresent LocalDate startDate,
            @Schema(description = "여행 종료일", example = "2024-09-04") @NotNull @FutureOrPresent LocalDate endDate,
            @Schema(description = "여행 인원", example = "2") @Positive Integer numberOfPeople,
            @Schema(description = "총 예산", example = "1000000") Integer totalBudget,
            @Valid List<DailyPlan> dailyPlans
    ) {
        public Trip toTripEntity() {
            Trip trip = new Trip(threadId, title, destination, startDate, endDate, numberOfPeople, totalBudget, "PLANNING", null);

            if (this.dailyPlans != null) {
                List<TripDetail> tripDetails = this.dailyPlans.stream()
                        .flatMap(dailyPlan -> dailyPlan.activities().stream()
                                .map(activity -> new TripDetail(
                                        dailyPlan.dayNumber(),
                                        dailyPlan.activityDate(),
                                        activity.activityTime(),
                                        activity.placeName(),
                                        activity.category(),
                                        activity.description(),
                                        activity.estimatedCost(),
                                        activity.address(),
                                        activity.latitude(),
                                        activity.longitude(),
                                        activity.tips(),
                                        "",  // additionalInfo
                                        activity.displayOrder()
                                )))
                        .collect(Collectors.toList());
                tripDetails.forEach(trip::addDetail);
            }

            return trip;
        }
    }

    @Schema(description = "일자별 계획")
    public record DailyPlan(
            @Schema(description = "여행 일차", example = "1") @NotNull Integer dayNumber,
            @Schema(description = "활동 날짜", example = "2024-09-01") LocalDate activityDate,
            @Valid List<Activity> activities
    ) {}

    @Schema(description = "활동 계획")
    public record Activity(
            @Schema(description = "활동 시간", example = "09:00", type = "string") @JsonFormat(pattern = "HH:mm") LocalTime activityTime,
            @Schema(description = "장소명", example = "경복궁") @NotBlank String placeName,
            @Schema(description = "카테고리", example = "관광지") String category,
            @Schema(description = "활동 설명", example = "조선 왕조의 법궁") String description,
            @Schema(description = "예상 비용", example = "3000") Integer estimatedCost,
            @Schema(description = "주소", example = "서울특별시 종로구 사직로 161") String address,
            @Schema(description = "위도", example = "37.579617") Double latitude,
            @Schema(description = "경도", example = "126.977041") Double longitude,
            @Schema(description = "팁", example = "한복을 입으면 무료 입장") String tips,
            @Schema(description = "표시 순서", example = "1") Integer displayOrder
    ) {}

    @Schema(description = "여행 계획 생성 응답")
    public record Response(
            @Schema(description = "생성된 여행 계획 ID", example = "1") Long id,
            @Schema(description = "생성된 여행 계획 UUID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") UUID tripUuid
    ) {
        public static Response from(Trip trip) {
            return new Response(trip.getId(), trip.getTripUuid());
        }
    }
}
