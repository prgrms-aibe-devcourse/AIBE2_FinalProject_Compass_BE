package com.compass.domain.trip.controller;

import com.compass.domain.trip.dto.BudgetRequest;
import com.compass.domain.trip.dto.BudgetResponse;
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
@Tag(name = "선호도", description = "사용자 선호도 관리")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @PostMapping("/travel-style")
    @Operation(summary = "여행 스타일 선호도 설정", description = "사용자의 여행 스타일 선호도를 설정합니다. 각 스타일의 가중치 합은 1.0이어야 합니다.")
    public ResponseEntity<TravelStylePreferenceResponse> setTravelStylePreferences(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Valid @RequestBody TravelStylePreferenceRequest request) {
        return ResponseEntity.ok(userPreferenceService.setTravelStylePreferences(userId, request));
    }

    @GetMapping("/travel-style")
    @Operation(summary = "여행 스타일 선호도 조회", description = "사용자의 현재 여행 스타일 선호도를 조회합니다.")
    public ResponseEntity<TravelStylePreferenceResponse> getTravelStylePreferences(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId) {
        return ResponseEntity.ok(userPreferenceService.getTravelStylePreferences(userId));
    }

    @PutMapping("/travel-style")
    @Operation(summary = "여행 스타일 선호도 수정", description = "사용자의 여행 스타일 선호도를 수정합니다. 기존 설정이 없으면 새로 생성됩니다.")
    public ResponseEntity<TravelStylePreferenceResponse> updateTravelStylePreferences(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Valid @RequestBody TravelStylePreferenceRequest request) {
        return ResponseEntity.ok(userPreferenceService.updateTravelStylePreferences(userId, request));
    }
    
    @PostMapping("/budget-level")
    @Operation(summary = "예산 수준 선호도 설정", description = "사용자의 예산 수준 선호도를 설정합니다.")
    public ResponseEntity<BudgetResponse> setBudgetLevel(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(userPreferenceService.setOrUpdateBudgetLevel(userId, request));
    }

    @GetMapping("/budget-level")
    @Operation(summary = "예산 수준 선호도 조회", description = "사용자의 현재 예산 수준 선호도를 조회합니다.")
    public ResponseEntity<BudgetResponse> getBudgetLevel(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId) {
        return ResponseEntity.ok(userPreferenceService.getBudgetLevel(userId));
    }

    @PutMapping("/budget-level")
    @Operation(summary = "예산 수준 선호도 수정", description = "사용자의 예산 수준 선호도를 수정합니다. 기존 설정이 없으면 새로 생성됩니다.")
    public ResponseEntity<BudgetResponse> updateBudgetLevel(
            @Parameter(description = "사용자 ID", required = true) @PathVariable Long userId,
            @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(userPreferenceService.setOrUpdateBudgetLevel(userId, request));
    }
}
