package com.compass.domain.trip.dto;

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.TripStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "여행 계획 목록 조회 DTO")
public class TripList {

    @Schema(description = "여행 계획 목록 조회 응답")
    public record Response(
            @Schema(description = "여행 계획 ID", example = "1") Long id,
            @Schema(description = "여행 계획 UUID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef") UUID tripUuid,
            @Schema(description = "여행 제목", example = "서울 3박 4일 여행") String title,
            @Schema(description = "목적지", example = "서울") String destination,
            @Schema(description = "여행 시작일", example = "2024-09-01") LocalDate startDate,
            @Schema(description = "여행 종료일", example = "2024-09-04") LocalDate endDate,
            @Schema(description = "여행 인원", example = "2") Integer numberOfPeople,
            @Schema(description = "총 예산", example = "1000000") Integer totalBudget,
            @Schema(description = "여행 상태", example = "PLANNING") TripStatus status,
            @Schema(description = "생성일시", example = "2024-08-01T10:00:00") LocalDateTime createdAt
    ) {
        public static Response from(Trip trip) {
            return new Response(
                    trip.getId(),
                    trip.getTripUuid(),
                    trip.getTitle(),
                    trip.getDestination(),
                    trip.getStartDate(),
                    trip.getEndDate(),
                    trip.getNumberOfPeople(),
                    trip.getTotalBudget(),
                    trip.getStatus(),
                    trip.getCreatedAt()
            );
        }
    }

    @Schema(description = "페이징된 여행 계획 목록 응답")
    public record PageResponse(
            @Schema(description = "여행 계획 목록") Page<Response> trips
    ) {
        public static PageResponse from(Page<Trip> tripPage) {
            Page<Response> responsePage = tripPage.map(Response::from);
            return new PageResponse(responsePage);
        }
    }
}
