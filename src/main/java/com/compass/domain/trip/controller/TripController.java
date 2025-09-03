package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.TripCreate;
import com.compass.domain.trip.dto.TripDetail;
import com.compass.domain.trip.dto.TripList;
import com.compass.domain.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Sort;

@Tag(name = "여행 계획", description = "여행 계획 CRUD")
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(summary = "여행 계획 생성", description = "새로운 여행 계획을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "여행 계획 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
    })
    @PostMapping
    public ResponseEntity<TripCreate.Response> createTrip(
            @Valid @RequestBody TripCreate.Request request) {
        TripCreate.Response response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "여행 계획 상세 조회", description = "특정 여행 계획의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "여행 계획 조회 성공"),
            @ApiResponse(responseCode = "404", description = "여행 계획을 찾을 수 없음"),
    })
    @GetMapping("/{tripId}")
    public ResponseEntity<TripDetail.Response> getTripById(
            @Parameter(description = "조회할 여행 계획 ID", example = "1")
            @PathVariable Long tripId) {
        TripDetail.Response response = tripService.getTripById(tripId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "여행 계획 목록 조회", description = "사용자의 여행 계획 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "여행 계획 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
    })
    @GetMapping
    public ResponseEntity<Page<TripList.Response>> getTripsByUserId(
            @Parameter(description = "조회할 사용자 ID", example = "1", required = true)
            @RequestParam Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        Page<TripList.Response> response = tripService.getTripsByUserId(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
