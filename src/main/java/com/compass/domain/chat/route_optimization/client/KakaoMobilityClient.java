package com.compass.domain.chat.route_optimization.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoMobilityClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kakao.api.rest-key:dummy-rest-key}")
    private String restApiKey;

    @Value("${kakao.api.mobility.base-url:https://apis-navi.kakaomobility.com}")
    private String baseUrl;

    @Value("${kakao.api.mobility.rate-limit:10}")
    private int rateLimit;

    private static final String DIRECTIONS_PATH = "/v1/waypoints/directions";
    private static final String DUMMY_API_KEY = "dummy-key";

    // 다중 목적지 경로 탐색
    public RouteResponse searchMultiDestinationRoute(RouteRequest request) {
        log.info("카카오 모빌리티 경로 탐색: {} 지점", request.waypoints().size());
        log.info("API Key 상태: {}", restApiKey != null ? restApiKey.substring(0, 8) + "..." : "null");

        // API 키가 설정되지 않았거나 dummy인 경우 Mock 반환
        if (restApiKey == null || restApiKey.isBlank() || restApiKey.contains("dummy")) {
            log.info("Kakao REST API key not configured. Using mock response.");
            return createMockResponse(request);
        }

        try {
            // 실제 API 호출
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + restApiKey);
            headers.set("Content-Type", "application/json");

            Map<String, Object> body = buildRequestBody(request);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Kakao API 요청: URL={}, Body={}", baseUrl + DIRECTIONS_PATH, body);

            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + DIRECTIONS_PATH,
                HttpMethod.POST,
                entity,
                Map.class
            );

            log.info("Kakao API 응답 상태: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Kakao API 호출 성공! 응답: {}", response.getBody());
                return parseResponse(response.getBody());
            } else {
                log.warn("Kakao API 응답 오류: Status={}, Body={}", response.getStatusCode(), response.getBody());
                return createMockResponse(request);
            }

        } catch (Exception e) {
            log.error("카카오 모빌리티 API 호출 실패: URL={}, 에러={}", baseUrl + DIRECTIONS_PATH, e.getMessage());
            log.error("상세 에러: ", e);
            return createMockResponse(request);
        }
    }

    // 두 지점 간 거리와 시간 계산
    public DistanceInfo calculateDistance(
        double originLat, double originLng,
        double destLat, double destLng,
        TransportMode mode
    ) {
        log.debug("거리 계산: ({},{}) -> ({},{}), mode={}",
            originLat, originLng, destLat, destLng, mode);

        // Haversine 공식으로 직선 거리 계산
        double distance = calculateHaversineDistance(originLat, originLng, destLat, destLng);

        // 도로 거리 보정 (직선 거리의 1.3배)
        double roadDistance = distance * 1.3;

        // 이동 수단별 시간 계산
        int duration = calculateDuration(roadDistance, mode);

        return new DistanceInfo(roadDistance, duration, mode);
    }

    // 다중 경유지 최적화
    public List<Waypoint> optimizeWaypoints(List<Waypoint> waypoints, TransportMode mode) {
        log.info("경유지 최적화: {} 지점, mode={}", waypoints.size(), mode);

        if (waypoints.size() <= 2) {
            return waypoints;
        }

        // TSP 알고리즘으로 최적 순서 계산
        List<Waypoint> optimized = new ArrayList<>();
        Set<Waypoint> unvisited = new HashSet<>(waypoints);

        // 시작점
        Waypoint current = waypoints.get(0);
        optimized.add(current);
        unvisited.remove(current);

        // 가장 가까운 이웃 선택
        while (!unvisited.isEmpty()) {
            Waypoint nearest = findNearestWaypoint(current, unvisited);
            optimized.add(nearest);
            unvisited.remove(nearest);
            current = nearest;
        }

        return optimized;
    }

    // 요청 본문 생성
    private Map<String, Object> buildRequestBody(RouteRequest request) {
        Map<String, Object> body = new HashMap<>();

        // 출발지
        body.put("origin", Map.of(
            "x", request.waypoints().get(0).longitude(),
            "y", request.waypoints().get(0).latitude()
        ));

        // 목적지
        Waypoint destination = request.waypoints().get(request.waypoints().size() - 1);
        body.put("destination", Map.of(
            "x", destination.longitude(),
            "y", destination.latitude()
        ));

        // 경유지
        if (request.waypoints().size() > 2) {
            List<Map<String, Object>> viaPoints = new ArrayList<>();
            for (int i = 1; i < request.waypoints().size() - 1; i++) {
                Waypoint wp = request.waypoints().get(i);
                viaPoints.add(Map.of(
                    "x", wp.longitude(),
                    "y", wp.latitude()
                ));
            }
            body.put("waypoints", viaPoints);
        }

        // 옵션
        body.put("priority", request.priority());  // RECOMMEND, DISTANCE, TIME
        body.put("car_type", request.carType());    // 1: 소형차, 2: 중형차
        body.put("car_fuel", request.carFuel());    // GASOLINE, DIESEL

        return body;
    }

    // API 응답 파싱
    @SuppressWarnings("unchecked")
    private RouteResponse parseResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes == null || routes.isEmpty()) {
                log.warn("Kakao API 응답에 경로 정보 없음");
                return null;
            }

            Map<String, Object> route = routes.get(0);  // 첫 번째 추천 경로 사용
            Map<String, Object> summary = (Map<String, Object>) route.get("summary");

            double totalDistance = ((Number) summary.get("distance")).doubleValue() / 1000.0;  // m -> km
            int totalDuration = ((Number) summary.get("duration")).intValue() / 60;  // 초 -> 분

            // 섹션 정보 파싱
            List<Map<String, Object>> sections = (List<Map<String, Object>>) route.get("sections");
            List<RouteSection> routeSections = new ArrayList<>();

            if (sections != null) {
                for (Map<String, Object> section : sections) {
                    double distance = ((Number) section.get("distance")).doubleValue() / 1000.0;
                    int duration = ((Number) section.get("duration")).intValue() / 60;

                    // 경로 좌표 파싱 (생략 가능)
                    List<double[]> path = parseCoordinates(section);

                    routeSections.add(new RouteSection(
                        "", // from name은 클라이언트에서 관리
                        "", // to name은 클라이언트에서 관리
                        distance,
                        duration,
                        path
                    ));
                }
            }

            return new RouteResponse(
                "SUCCESS",
                totalDistance,
                totalDuration,
                routeSections,
                null  // optimized waypoints는 별도 처리
            );

        } catch (Exception e) {
            log.error("Kakao API 응답 파싱 실패", e);
            return null;
        }
    }

    // 좌표 파싱 헬퍼 메서드
    @SuppressWarnings("unchecked")
    private List<double[]> parseCoordinates(Map<String, Object> section) {
        List<double[]> coordinates = new ArrayList<>();
        try {
            List<Map<String, Object>> roads = (List<Map<String, Object>>) section.get("roads");
            if (roads != null) {
                for (Map<String, Object> road : roads) {
                    List<Double> vertexes = (List<Double>) road.get("vertexes");
                    if (vertexes != null) {
                        // vertexes는 [x1, y1, x2, y2, ...] 형식
                        for (int i = 0; i < vertexes.size(); i += 2) {
                            coordinates.add(new double[]{
                                vertexes.get(i),     // longitude
                                vertexes.get(i + 1)  // latitude
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("좌표 파싱 스킵: {}", e.getMessage());
        }
        return coordinates;
    }

    // Mock 응답 생성
    private RouteResponse createMockResponse(RouteRequest request) {
        List<RouteSection> sections = new ArrayList<>();
        double totalDistance = 0;
        int totalDuration = 0;

        for (int i = 0; i < request.waypoints().size() - 1; i++) {
            Waypoint from = request.waypoints().get(i);
            Waypoint to = request.waypoints().get(i + 1);

            double distance = calculateHaversineDistance(
                from.latitude(), from.longitude(),
                to.latitude(), to.longitude()
            ) * 1.3;  // 도로 거리 보정

            int duration = calculateDuration(distance, request.transportMode());

            sections.add(new RouteSection(
                from.name(),
                to.name(),
                distance,
                duration,
                List.of()  // 상세 경로 (생략)
            ));

            totalDistance += distance;
            totalDuration += duration;
        }

        return new RouteResponse(
            "SUCCESS",
            totalDistance,
            totalDuration,
            sections,
            request.waypoints()
        );
    }

    // Haversine 공식으로 거리 계산
    private double calculateHaversineDistance(
        double lat1, double lon1, double lat2, double lon2
    ) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // 이동 수단별 소요 시간 계산
    private int calculateDuration(double distance, TransportMode mode) {
        double speedKmh = switch (mode) {
            case CAR -> 50;           // 도심 평균 속도
            case PUBLIC_TRANSPORT -> 30;  // 대중교통 평균
            case WALKING -> 4;        // 도보 속도
            case BICYCLE -> 15;       // 자전거 속도
        };

        return (int)(distance / speedKmh * 60);  // 분 단위
    }

    // 가장 가까운 경유지 찾기
    private Waypoint findNearestWaypoint(Waypoint current, Set<Waypoint> unvisited) {
        return unvisited.stream()
            .min(Comparator.comparing(wp -> calculateHaversineDistance(
                current.latitude(), current.longitude(),
                wp.latitude(), wp.longitude()
            )))
            .orElse(unvisited.iterator().next());
    }

    // 요청/응답 모델
    public record RouteRequest(
        List<Waypoint> waypoints,
        TransportMode transportMode,
        String priority,  // RECOMMEND, DISTANCE, TIME
        int carType,      // 1: 소형차, 2: 중형차
        String carFuel    // GASOLINE, DIESEL
    ) {
        public static RouteRequest of(List<Waypoint> waypoints, TransportMode mode) {
            return new RouteRequest(waypoints, mode, "RECOMMEND", 1, "GASOLINE");
        }
    }

    public record Waypoint(
        String name,
        double latitude,
        double longitude
    ) {}

    public record RouteResponse(
        String status,
        double totalDistance,  // km
        int totalDuration,     // 분
        List<RouteSection> sections,
        List<Waypoint> optimizedWaypoints
    ) {}

    public record RouteSection(
        String from,
        String to,
        double distance,
        int duration,
        List<double[]> path  // 상세 경로 좌표
    ) {}

    public record DistanceInfo(
        double distance,  // km
        int duration,     // 분
        TransportMode mode
    ) {}

    public enum TransportMode {
        CAR,
        PUBLIC_TRANSPORT,
        WALKING,
        BICYCLE
    }
}