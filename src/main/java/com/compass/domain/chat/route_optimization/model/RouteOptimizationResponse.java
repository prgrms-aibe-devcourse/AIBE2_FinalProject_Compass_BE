package com.compass.domain.chat.route_optimization.model;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record RouteOptimizationResponse(
    Map<Integer, List<TourPlace>> aiRecommendedItinerary,
    Map<Integer, List<TourPlace>> allCandidatePlaces,
    Map<Integer, RouteInfo> dailyRoutes,
    TravelStatistics statistics,
    boolean success,
    String message
) {
    public static RouteOptimizationResponse success(
        Map<Integer, List<TourPlace>> aiRecommended,
        Map<Integer, List<TourPlace>> allCandidates,
        Map<Integer, RouteInfo> routes
    ) {
        TravelStatistics stats = TravelStatistics.calculate(aiRecommended, allCandidates, routes);
        return new RouteOptimizationResponse(aiRecommended, allCandidates, routes, stats, true, "처리 완료");
    }

    public static RouteOptimizationResponse error(String message) {
        return new RouteOptimizationResponse(Map.of(), Map.of(), Map.of(), null, false, message);
    }

    public record RouteInfo(
        List<String> orderedPlaces,
        double totalDistance,
        int totalDuration,
        String transportMode,
        List<Segment> segments
    ) {
        public record Segment(
            String from,
            String to,
            double distance,
            int duration,
            String transport
        ) {}
    }

    public record TravelStatistics(
        int totalDays,
        int recommendedPlaces,
        int candidatePlaces,
        double totalDistance,
        int totalDuration,
        double averagePlacesPerDay,
        double optimizationRate
    ) {
        public static TravelStatistics calculate(
            Map<Integer, List<TourPlace>> recommended,
            Map<Integer, List<TourPlace>> candidates,
            Map<Integer, RouteInfo> routes
        ) {
            int days = recommended.size();
            int recPlaces = recommended.values().stream().mapToInt(List::size).sum();
            int candPlaces = candidates.values().stream().mapToInt(List::size).sum();
            double dist = routes.values().stream().mapToDouble(RouteInfo::totalDistance).sum();
            int time = routes.values().stream().mapToInt(RouteInfo::totalDuration).sum();
            double avg = days > 0 ? (double) recPlaces / days : 0;
            double rate = candPlaces > 0 ? (double) recPlaces / candPlaces * 100 : 0;

            return new TravelStatistics(days, recPlaces, candPlaces, dist, time, avg, rate);
        }
    }
}