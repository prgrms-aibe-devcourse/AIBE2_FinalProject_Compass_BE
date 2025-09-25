package com.compass.domain.chat.route_optimization.controller;

import com.compass.domain.chat.route_optimization.model.RouteOptimizationRequest;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationResponse;
import com.compass.domain.chat.route_optimization.service.RouteOptimizationOrchestrationService;
import com.compass.domain.chat.route_optimization.service.RouteOptimizationOrchestrationService.CustomizationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/travel/route-optimization")
@RequiredArgsConstructor
@Tag(name = "Route Optimization", description = "여행 일정 최적화 API")
public class RouteOptimizationApiController {

    private final RouteOptimizationOrchestrationService orchestrationService;

    @PostMapping("/optimize/{sessionId}")
    @Operation(summary = "AI 추천 최적 일정 생성")
    public ResponseEntity<RouteOptimizationResponse> optimizeItinerary(
        @PathVariable Long sessionId,
        @RequestBody RouteOptimizationRequest request
    ) {
        log.info("Optimize request: sessionId={}", sessionId);
        RouteOptimizationResponse response = orchestrationService.processRouteOptimization(sessionId, request);
        return response.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/customize/{sessionId}/{itineraryId}")
    @Operation(summary = "사용자 맞춤 일정 수정")
    public ResponseEntity<RouteOptimizationResponse> customizeItinerary(
        @PathVariable Long sessionId,
        @PathVariable Long itineraryId,
        @RequestBody CustomizationRequest request
    ) {
        log.info("Customize: sessionId={}, itineraryId={}", sessionId, itineraryId);
        RouteOptimizationResponse response = orchestrationService.customizeItinerary(sessionId, itineraryId, request);
        return response.success() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}