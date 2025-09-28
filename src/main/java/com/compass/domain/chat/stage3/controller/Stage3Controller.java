package com.compass.domain.chat.stage3.controller;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.stage3.dto.*;
import com.compass.domain.chat.stage3.service.Stage3IntegrationService;
import com.compass.domain.chat.stage3.service.Stage3KMeansClusteringService;
import com.compass.domain.chat.stage3.service.Stage3RouteOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat/stage3")
@RequiredArgsConstructor
@Slf4j
public class Stage3Controller {

    private final Stage3IntegrationService stage3IntegrationService;
    private final Stage3KMeansClusteringService stage3KMeansClusteringService;
    private final Stage3RouteOptimizationService stage3RouteOptimizationService;

    // Process Stage3 with user selected places
    @PostMapping("/process")
    public ResponseEntity<Stage3Response> processStage3(
            @RequestBody Stage3Request request,
            Authentication authentication) {

        log.info("Processing Stage3 request for destination: {}, dates: {} to {}",
            request.getDestination(), request.getStartDate(), request.getEndDate());

        try {
            // Convert request to TravelContext
            TravelContext context = buildTravelContext(request, authentication);

            // Process Stage3
            Stage3Output output = stage3IntegrationService.processWithTravelContext(context);
            Stage3Response response = Stage3Response.from(output);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Stage3 request", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Process Stage3 with TravelContext
    @PostMapping("/process-context")
    public ResponseEntity<Stage3Response> processWithContext(
            @RequestBody TravelContext context) {

        log.info("Processing Stage3 with TravelContext for userId: {}, threadId: {}",
            context.getUserId(), context.getThreadId());

        try {
            Stage3Output output = stage3IntegrationService.processWithTravelContext(context);
            Stage3Response response = Stage3Response.from(output);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Stage3 with context", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Test K-means clustering
    @PostMapping("/test-clustering")
    public ResponseEntity<Map<String, Object>> testClustering(
            @RequestBody Map<String, Object> request) {

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> placesData = (List<Map<String, Object>>) request.get("places");
            Integer k = (Integer) request.get("k");

            // Convert to TravelPlace objects
            List<TravelPlace> places = placesData.stream()
                .map(this::mapToTravelPlace)
                .collect(Collectors.toList());

            // Perform clustering
            Map<Integer, List<TravelPlace>> clusters = stage3KMeansClusteringService.clusterPlaces(places, k);

            // Get cluster centers
            List<Stage3KMeansClusteringService.ClusterCenter> centers =
                stage3KMeansClusteringService.getClusterCenters(clusters);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("clusters", clusters);
            response.put("clusterCenters", centers);
            response.put("totalClusters", clusters.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error testing clustering", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Test time block arrangement
    @GetMapping("/test-timeblocks")
    public ResponseEntity<Map<String, Object>> testTimeBlocks(
            @RequestParam String date) {

        try {
            LocalDate targetDate = LocalDate.parse(date);

            // Create sample time blocks
            Map<String, Object> timeBlocks = new HashMap<>();
            timeBlocks.put("date", targetDate);
            timeBlocks.put("timeSlots", Arrays.asList(
                Map.of("block", "MORNING", "time", "09:00-12:00", "activities", "관광"),
                Map.of("block", "LUNCH", "time", "12:00-14:00", "activities", "식사"),
                Map.of("block", "AFTERNOON", "time", "14:00-18:00", "activities", "관광"),
                Map.of("block", "EVENING", "time", "18:00-21:00", "activities", "저녁 및 휴식")
            ));

            return ResponseEntity.ok(timeBlocks);
        } catch (Exception e) {
            log.error("Error testing time blocks", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Test route optimization
    @PostMapping("/optimize-route")
    public ResponseEntity<Map<String, Object>> optimizeRoute(
            @RequestBody Map<String, Object> request) {

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> placesData = (List<Map<String, Object>>) request.get("places");
            String transportMode = (String) request.get("transportMode");
            String departureLocation = (String) request.get("departureLocation");

            // Convert to TravelPlace objects
            List<TravelPlace> places = placesData.stream()
                .map(this::mapToTravelPlace)
                .collect(Collectors.toList());

            // Optimize route
            OptimizedRoute optimizedRoute = stage3RouteOptimizationService.optimize(
                places, transportMode, departureLocation);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("originalOrder", places);
            response.put("optimizedOrder", optimizedRoute.getPlaces());
            response.put("totalDistance", optimizedRoute.getTotalDistance());
            response.put("totalDuration", optimizedRoute.getTotalDuration());
            response.put("transportMode", transportMode);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error optimizing route", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper method to build TravelContext from Stage3Request
    private TravelContext buildTravelContext(Stage3Request request, Authentication authentication) {
        TravelContext context = new TravelContext();

        // Set user info
        if (authentication != null) {
            context.setUserId(authentication.getName());
        }
        context.setThreadId(UUID.randomUUID().toString());

        // Set collected info
        Map<String, Object> collectedInfo = new HashMap<>();
        collectedInfo.put("destinations", Arrays.asList(request.getDestination()));
        collectedInfo.put("startDate", request.getStartDate());
        collectedInfo.put("endDate", request.getEndDate());
        collectedInfo.put("travelStyle", Arrays.asList(request.getTravelStyle()));
        collectedInfo.put("companions", request.getTravelCompanion());
        collectedInfo.put("transportationType", request.getTransportMode());

        // 사용자 선택 장소 처리
        if (request.getUserSelectedPlaces() != null && !request.getUserSelectedPlaces().isEmpty()) {
            log.info("Adding {} user selected places to context", request.getUserSelectedPlaces().size());
            collectedInfo.put("userSelectedPlaces", request.getUserSelectedPlaces());
        }
        context.setCollectedInfo(collectedInfo);

        return context;
    }


    // Helper method to convert map to TravelPlace
    private TravelPlace mapToTravelPlace(Map<String, Object> placeData) {
        TravelPlace place = new TravelPlace();
        place.setPlaceId((String) placeData.get("placeId"));
        place.setName((String) placeData.getOrDefault("placeName", placeData.get("name")));
        place.setCategory((String) placeData.get("category"));

        // Handle numeric values
        if (placeData.get("latitude") instanceof Number) {
            place.setLatitude(((Number) placeData.get("latitude")).doubleValue());
        }
        if (placeData.get("longitude") instanceof Number) {
            place.setLongitude(((Number) placeData.get("longitude")).doubleValue());
        }
        if (placeData.get("rating") instanceof Number) {
            place.setRating(((Number) placeData.get("rating")).doubleValue());
        }

        place.setAddress((String) placeData.get("address"));

        return place;
    }


}