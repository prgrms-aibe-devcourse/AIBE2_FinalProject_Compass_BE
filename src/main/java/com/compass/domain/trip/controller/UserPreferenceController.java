package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.TravelStylePreferenceRequest;
import com.compass.domain.trip.dto.TravelStylePreferenceResponse;
import com.compass.domain.trip.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 선호도 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/users/{userId}/preferences")
@RequiredArgsConstructor
@Tag(name = "사용자 선호도", description = "사용자 여행 선호도 관리 API")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @PostMapping("/travel-style")
    @Operation(
        summary = "여행 스타일 선호도 설정",
        description = "사용자의 여행 스타일 선호도(휴양/관광/액티비티)를 설정합니다. 가중치의 합계는 1.0이어야 합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "성공적으로 여행 스타일 선호도가 설정됨",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TravelStylePreferenceResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 파라미터 (가중치 합계 오류, 중복 스타일, 범위 오류 등)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음"
        )
    })
    public ResponseEntity<TravelStylePreferenceResponse> setTravelStylePreferences(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @PathVariable Long userId,
            @Parameter(description = "여행 스타일 선호도 설정 요청", required = true)
            @RequestBody @Valid TravelStylePreferenceRequest request) {
        
        log.info("POST /api/users/{}/preferences/travel-style - Setting travel style preferences", userId);
        
        TravelStylePreferenceResponse response = userPreferenceService
                .setTravelStylePreferences(userId, request);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/travel-style")
    @Operation(
        summary = "여행 스타일 선호도 조회",
        description = "사용자의 설정된 여행 스타일 선호도를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "여행 스타일 선호도 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TravelStylePreferenceResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음"
        )
    })
    public ResponseEntity<TravelStylePreferenceResponse> getTravelStylePreferences(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @PathVariable Long userId) {
        
        log.info("GET /api/users/{}/preferences/travel-style - Getting travel style preferences", userId);
        
        TravelStylePreferenceResponse response = userPreferenceService
                .getTravelStylePreferences(userId);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/travel-style")
    @Operation(
        summary = "여행 스타일 선호도 수정",
        description = "사용자의 여행 스타일 선호도를 수정합니다. 기존 설정을 완전히 교체합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "성공적으로 여행 스타일 선호도가 수정됨",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TravelStylePreferenceResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 파라미터 (가중치 합계 오류, 중복 스타일, 범위 오류 등)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음"
        )
    })
    public ResponseEntity<TravelStylePreferenceResponse> updateTravelStylePreferences(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @PathVariable Long userId,
            @Parameter(description = "여행 스타일 선호도 수정 요청", required = true)
            @RequestBody @Valid TravelStylePreferenceRequest request) {
        
        log.info("PUT /api/users/{}/preferences/travel-style - Updating travel style preferences", userId);
        
        TravelStylePreferenceResponse response = userPreferenceService
                .updateTravelStylePreferences(userId, request);
        
        return ResponseEntity.ok(response);
    }
}
