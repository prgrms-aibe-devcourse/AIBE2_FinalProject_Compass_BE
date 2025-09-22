package com.compass.domain.chat.function.planning;

import com.compass.domain.chat.model.request.DestinationSearchRequest;
import com.compass.domain.chat.model.response.OptimizedRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;

// 목적지 검색 및 경로 최적화 Function
@Slf4j
@Component
public class SearchDestinationsFunction implements Function<DestinationSearchRequest, OptimizedRoute> {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${GOOGLE_MAPS_API_KEY:}")
    private String googleMapsApiKey;

    @Value("${OPENROUTE_API_KEY:}")
    private String openRouteApiKey;

    @Value("${KAKAO_MOBILITY_API_KEY:}")
    private String kakaoMobilityApiKey;

    @Override
    public OptimizedRoute apply(DestinationSearchRequest request) {
        log.info("목적지 경로 최적화 시작: {} 개 도시", request.destinations().size());

        try {
            return processRouteOptimization(request);
        } catch (Exception e) {
            log.error("경로 최적화 실패", e);
            return OptimizedRoute.error("경로 최적화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 경로 최적화 처리 (메서드 분할)
    private OptimizedRoute processRouteOptimization(DestinationSearchRequest request) {
        // 1. 입력 검증
        if (request.destinations().size() == 1) {
            return handleSingleDestination(request);
        }

        // 2. 거리 행렬 계산
        var distanceMatrix = calculateDistanceMatrix(request.destinations(), request.startLocation());

        // 3. 경로 계산 및 결과 생성
        return buildOptimizedRoute(request, distanceMatrix);
    }

    // 최적화된 경로 구성 (메서드 분할)
    private OptimizedRoute buildOptimizedRoute(DestinationSearchRequest request, 
                                             Map<String, Map<String, DistanceInfo>> distanceMatrix) {
        var optimizedOrder = optimizeRoute(
            request.destinations(), 
            distanceMatrix, 
            request.optimizationStrategy(),
            request.startLocation()
        );

        var segments = createRouteSegments(optimizedOrder, distanceMatrix);
        var totalDistance = calculateTotalDistance(segments);
        var totalTime = calculateTotalTime(segments);

        log.info("경로 최적화 완료: 총 거리 {}km, 총 시간 {}분", totalDistance, totalTime);

        return OptimizedRoute.success(
            optimizedOrder,
            totalDistance,
            totalTime,
            request.optimizationStrategy(),
            segments
        );
    }

    // 단일 목적지 처리
    private OptimizedRoute handleSingleDestination(DestinationSearchRequest request) {
        var destination = request.destinations().get(0);
        var segments = request.startLocation() != null ? 
            List.of(new OptimizedRoute.RouteSegment(request.startLocation(), destination, 0.0, 0, "자동차")) :
            List.<OptimizedRoute.RouteSegment>of();

        return OptimizedRoute.success(
            List.of(destination),
            0.0,
            0,
            request.optimizationStrategy(),
            segments
        );
    }

    // Google Maps Distance Matrix API를 사용한 실제 거리 계산
    private Map<String, Map<String, DistanceInfo>> calculateDistanceMatrix(List<String> destinations, String startLocation) {
        var matrix = new HashMap<String, Map<String, DistanceInfo>>();
        var allLocations = new ArrayList<>(destinations);
        if (startLocation != null && !startLocation.isEmpty()) {
            allLocations.add(0, startLocation);
        }

        // 1. 카카오 모빌리티 API 시도 (한국 특화, 추천)
        if (kakaoMobilityApiKey != null && !kakaoMobilityApiKey.isEmpty()) {
            try {
                return callKakaoMobilityDistanceMatrix(allLocations);
            } catch (Exception e) {
                log.warn("카카오 모빌리티 API 호출 실패, 대안 API 시도", e);
            }
        }

        // 2. Google Maps API 시도
        if (googleMapsApiKey != null && !googleMapsApiKey.isEmpty()) {
            try {
                return callGoogleMapsDistanceMatrix(allLocations);
            } catch (Exception e) {
                log.warn("Google Maps API 호출 실패, 대안 API 시도", e);
            }
        }

        // 3. OpenRouteService API 시도 (무료)
        if (openRouteApiKey != null && !openRouteApiKey.isEmpty()) {
            try {
                return callOpenRouteDistanceMatrix(allLocations);
            } catch (Exception e) {
                log.warn("OpenRoute API 호출 실패, Mock 데이터 사용", e);
            }
        }

        // 4. Mock 데이터 사용 (실제 거리 기반)
        log.info("외부 API 없음, 실제 거리 기반 Mock 데이터를 사용합니다.");
        return calculateMockDistanceMatrix(allLocations);
    }

    // 카카오 모빌리티 길찾기 API 호출 (한국 특화)
    private Map<String, Map<String, DistanceInfo>> callKakaoMobilityDistanceMatrix(List<String> locations) {
        var matrix = new HashMap<String, Map<String, DistanceInfo>>();
        
        log.info("카카오 모빌리티 길찾기 API 호출: {} 개 위치", locations.size());
        
        // 각 도시 쌍별로 카카오 모빌리티 API 호출
        for (String from : locations) {
            matrix.put(from, new HashMap<>());
            for (String to : locations) {
                if (!from.equals(to)) {
                    try {
                        var distanceInfo = callKakaoMobilityDirections(from, to);
                        matrix.get(from).put(to, distanceInfo);
                    } catch (Exception e) {
                        log.warn("카카오 모빌리티 API 호출 실패: {} → {}, Mock 데이터 사용", from, to, e);
                        var mockDistance = calculateMockDistance(from, to);
                        var mockTime = (int) (mockDistance * 2);
                        matrix.get(from).put(to, new DistanceInfo(mockDistance, mockTime));
                    }
                }
            }
        }
        
        log.info("카카오 모빌리티 API 매트릭스 구성 완료: {}x{}", locations.size(), locations.size());
        return matrix;
    }
    
    // 카카오 모빌리티 단일 경로 조회
    private DistanceInfo callKakaoMobilityDirections(String from, String to) {
        // 도시명을 좌표로 변환 (실제로는 Geocoding API 사용해야 함)
        var fromCoord = getCityCoordinates(from);
        var toCoord = getCityCoordinates(to);
        
        var url = String.format(
            "https://apis-navi.kakaomobility.com/v1/directions?origin=%f,%f&destination=%f,%f&priority=RECOMMEND&summary=true",
            fromCoord[0], fromCoord[1], toCoord[0], toCoord[1]
        );
        
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoMobilityApiKey);
        headers.set("Content-Type", "application/json");
        
        var entity = new org.springframework.http.HttpEntity<>(headers);
        
        try {
            var response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            return parseKakaoMobilityResponse(response.getBody());
        } catch (Exception e) {
            log.error("카카오 모빌리티 API 호출 실패: {} → {}", from, to, e);
            throw new RuntimeException("카카오 모빌리티 API 호출 실패", e);
        }
    }
    
    // 도시명을 좌표로 변환 (Mock 데이터)
    private double[] getCityCoordinates(String city) {
        var coordMap = new HashMap<String, double[]>();
        coordMap.put("서울", new double[]{126.9780, 37.5665});
        coordMap.put("부산", new double[]{129.0756, 35.1796});
        coordMap.put("대구", new double[]{128.6014, 35.8714});
        coordMap.put("인천", new double[]{126.7052, 37.4563});
        coordMap.put("광주", new double[]{126.8526, 35.1595});
        coordMap.put("대전", new double[]{127.3845, 36.3504});
        coordMap.put("제주", new double[]{126.5312, 33.4996});
        coordMap.put("울산", new double[]{129.3114, 35.5384});
        coordMap.put("세종", new double[]{127.2890, 36.4800});
        coordMap.put("경주", new double[]{129.2247, 35.8562});
        
        return coordMap.getOrDefault(city, new double[]{127.0, 37.0}); // 기본값: 한국 중심
    }
    
    // 카카오 모빌리티 API 응답 파싱
    private DistanceInfo parseKakaoMobilityResponse(String response) {
        try {
            var rootNode = objectMapper.readTree(response);
            var routes = rootNode.path("routes");
            
            if (routes.isArray() && routes.size() > 0) {
                var firstRoute = routes.get(0);
                var summary = firstRoute.path("summary");
                
                // 거리(미터 → 킬로미터), 시간(초 → 분)
                var distanceKm = summary.path("distance").asDouble() / 1000.0;
                var durationMin = summary.path("duration").asInt() / 60;
                
                log.debug("카카오 모빌리티 API 응답 파싱: {}km, {}분", distanceKm, durationMin);
                return new DistanceInfo(distanceKm, durationMin);
            }
            
            throw new RuntimeException("카카오 모빌리티 API 응답에서 경로를 찾을 수 없음");
            
        } catch (Exception e) {
            log.error("카카오 모빌리티 API 응답 파싱 실패", e);
            throw new RuntimeException("카카오 모빌리티 API 응답 파싱 실패", e);
        }
    }

    // Google Maps Distance Matrix API 호출
    private Map<String, Map<String, DistanceInfo>> callGoogleMapsDistanceMatrix(List<String> locations) {
        var matrix = new HashMap<String, Map<String, DistanceInfo>>();
        
        // 위치들을 URL 인코딩
        var origins = String.join("|", locations);
        var destinations = String.join("|", locations);
        
        var url = String.format(
            "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%s&destinations=%s&key=%s&language=ko&units=metric",
            origins.replace(" ", "+"), destinations.replace(" ", "+"), googleMapsApiKey
        );

        log.info("Google Maps Distance Matrix API 호출: {} 개 위치", locations.size());

        try {
            var response = restTemplate.getForObject(url, String.class);
            return parseGoogleMapsResponse(response, locations);
        } catch (Exception e) {
            log.error("Google Maps API 호출 실패", e);
            throw new RuntimeException("거리 계산 API 호출 실패", e);
        }
    }

    // OpenRouteService API 호출 (무료 대안)
    private Map<String, Map<String, DistanceInfo>> callOpenRouteDistanceMatrix(List<String> locations) {
        var matrix = new HashMap<String, Map<String, DistanceInfo>>();
        
        log.info("OpenRouteService API 호출: {} 개 위치", locations.size());
        
        // OpenRouteService Matrix API 사용
        var url = "https://api.openrouteservice.org/v2/matrix/driving-car";
        
        // 좌표 변환 (실제로는 Geocoding 필요, 여기서는 Mock)
        var coordinates = locations.stream()
            .map(this::getMockCoordinates)
            .toList();
            
        try {
            // POST 요청 구성
            var requestBody = Map.of(
                "locations", coordinates,
                "metrics", List.of("distance", "duration")
            );
            
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", openRouteApiKey);
            headers.set("Content-Type", "application/json");
            
            var entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            var response = restTemplate.postForObject(url, entity, String.class);
            
            return parseOpenRouteResponse(response, locations);
            
        } catch (Exception e) {
            log.error("OpenRoute API 호출 실패", e);
            throw new RuntimeException("OpenRoute API 호출 실패", e);
        }
    }
    
    // Mock 좌표 반환 (실제로는 Geocoding API 사용)
    private double[] getMockCoordinates(String city) {
        var coordMap = Map.of(
            "서울", new double[]{126.9780, 37.5665},
            "부산", new double[]{129.0756, 35.1796},
            "대구", new double[]{128.6014, 35.8714},
            "인천", new double[]{126.7052, 37.4563},
            "광주", new double[]{126.8526, 35.1595},
            "대전", new double[]{127.3845, 36.3504},
            "제주", new double[]{126.5312, 33.4996}
        );
        
        return coordMap.getOrDefault(city, new double[]{127.0, 37.0}); // 기본값: 한국 중심
    }
    
    // OpenRoute 응답 파싱
    private Map<String, Map<String, DistanceInfo>> parseOpenRouteResponse(String response, List<String> locations) {
        try {
            var matrix = new HashMap<String, Map<String, DistanceInfo>>();
            var rootNode = objectMapper.readTree(response);
            var distances = rootNode.path("distances");
            var durations = rootNode.path("durations");
            
            for (int i = 0; i < locations.size(); i++) {
                var from = locations.get(i);
                matrix.put(from, new HashMap<>());
                
                for (int j = 0; j < locations.size(); j++) {
                    if (i != j) {
                        var to = locations.get(j);
                        var distanceKm = distances.get(i).get(j).asDouble() / 1000.0;
                        var durationMin = durations.get(i).get(j).asInt() / 60;
                        matrix.get(from).put(to, new DistanceInfo(distanceKm, durationMin));
                    }
                }
            }
            
            log.info("OpenRoute API 응답 파싱 완료: {}x{} 매트릭스", locations.size(), locations.size());
            return matrix;
            
        } catch (Exception e) {
            log.error("OpenRoute API 응답 파싱 실패", e);
            throw new RuntimeException("OpenRoute API 응답 파싱 실패", e);
        }
    }

    // Google Maps API 응답 파싱
    private Map<String, Map<String, DistanceInfo>> parseGoogleMapsResponse(String response, List<String> locations) {
        try {
            var matrix = new HashMap<String, Map<String, DistanceInfo>>();
            var rootNode = objectMapper.readTree(response);
            var rows = rootNode.path("rows");

            for (int i = 0; i < locations.size(); i++) {
                var from = locations.get(i);
                matrix.put(from, new HashMap<>());
                
                var elements = rows.get(i).path("elements");
                for (int j = 0; j < locations.size(); j++) {
                    if (i != j) {
                        var to = locations.get(j);
                        var element = elements.get(j);
                        
                        if ("OK".equals(element.path("status").asText())) {
                            var distanceKm = element.path("distance").path("value").asDouble() / 1000.0;
                            var durationMin = element.path("duration").path("value").asInt() / 60;
                            matrix.get(from).put(to, new DistanceInfo(distanceKm, durationMin));
                        } else {
                            // API 오류시 기본값 사용
                            matrix.get(from).put(to, new DistanceInfo(100.0, 120));
                        }
                    }
                }
            }
            
            log.info("Google Maps API 응답 파싱 완료: {}x{} 매트릭스", locations.size(), locations.size());
            return matrix;
            
        } catch (Exception e) {
            log.error("Google Maps API 응답 파싱 실패", e);
            throw new RuntimeException("거리 계산 응답 파싱 실패", e);
        }
    }

    // Mock 거리 행렬 계산 (API 실패시 대체)
    private Map<String, Map<String, DistanceInfo>> calculateMockDistanceMatrix(List<String> allLocations) {
        var matrix = new HashMap<String, Map<String, DistanceInfo>>();
        
        for (String from : allLocations) {
            matrix.put(from, new HashMap<>());
            for (String to : allLocations) {
                if (!from.equals(to)) {
                    var distance = calculateMockDistance(from, to);
                    var time = (int) (distance * 2); // 대략 30km/h 속도로 계산
                    matrix.get(from).put(to, new DistanceInfo(distance, time));
                }
            }
        }
        
        return matrix;
    }

    // Mock 거리 계산 (실제 한국 도시 거리 기반)
    private double calculateMockDistance(String from, String to) {
        // 한국 주요 도시간 실제 거리 데이터
        var distanceMap = new HashMap<String, Double>();
        distanceMap.put("서울-부산", 325.0); distanceMap.put("부산-서울", 325.0);
        distanceMap.put("서울-대구", 237.0); distanceMap.put("대구-서울", 237.0);
        distanceMap.put("서울-광주", 267.0); distanceMap.put("광주-서울", 267.0);
        distanceMap.put("서울-대전", 140.0); distanceMap.put("대전-서울", 140.0);
        distanceMap.put("서울-인천", 37.0); distanceMap.put("인천-서울", 37.0);
        distanceMap.put("서울-제주", 452.0); distanceMap.put("제주-서울", 452.0);
        distanceMap.put("부산-대구", 108.0); distanceMap.put("대구-부산", 108.0);
        distanceMap.put("부산-광주", 144.0); distanceMap.put("광주-부산", 144.0);
        distanceMap.put("대구-광주", 134.0); distanceMap.put("광주-대구", 134.0);
        distanceMap.put("대전-대구", 113.0); distanceMap.put("대구-대전", 113.0);
        distanceMap.put("대전-광주", 129.0); distanceMap.put("광주-대전", 129.0);
        distanceMap.put("인천-부산", 347.0); distanceMap.put("부산-인천", 347.0);
        
        var key = from + "-" + to;
        if (distanceMap.containsKey(key)) {
            return distanceMap.get(key);
        }
        
        // 매핑되지 않은 도시는 해시 기반으로 일관성 있게 계산
        var hash = Math.abs((from + to).hashCode());
        return 50.0 + (hash % 200); // 50-250km 사이의 거리
    }

    // 경로 최적화 (TSP 문제의 간단한 근사 해법)
    private List<String> optimizeRoute(List<String> destinations, 
                                     Map<String, Map<String, DistanceInfo>> distanceMatrix,
                                     String strategy,
                                     String startLocation) {
        
        return switch (strategy) {
            case "ONE_DIRECTION" -> optimizeOneDirection(destinations, distanceMatrix, startLocation);
            case "SHORTEST" -> optimizeShortest(destinations, distanceMatrix, startLocation);
            case "CIRCULAR" -> optimizeCircular(destinations, distanceMatrix, startLocation);
            default -> new ArrayList<>(destinations); // 기본: 입력 순서 유지
        };
    }

    // 일방향 최적화 (가장 가까운 다음 도시 선택)
    private List<String> optimizeOneDirection(List<String> destinations, 
                                            Map<String, Map<String, DistanceInfo>> distanceMatrix,
                                            String startLocation) {
        var result = new ArrayList<String>();
        var remaining = new HashSet<>(destinations);
        var currentLocation = startLocation != null ? startLocation : destinations.get(0);

        if (startLocation == null || destinations.contains(startLocation)) {
            remaining.remove(currentLocation);
            if (destinations.contains(currentLocation)) {
                result.add(currentLocation);
            }
        }

        // 가장 가까운 도시를 순차적으로 선택
        while (!remaining.isEmpty()) {
            String nextCity = findNearestCity(currentLocation, remaining, distanceMatrix);
            result.add(nextCity);
            remaining.remove(nextCity);
            currentLocation = nextCity;
        }

        return result;
    }

    // 최단 거리 최적화 (Nearest Neighbor 알고리즘)
    private List<String> optimizeShortest(List<String> destinations,
                                        Map<String, Map<String, DistanceInfo>> distanceMatrix,
                                        String startLocation) {
        // ONE_DIRECTION과 동일한 로직 (간단한 구현)
        return optimizeOneDirection(destinations, distanceMatrix, startLocation);
    }

    // 순환 경로 최적화
    private List<String> optimizeCircular(List<String> destinations,
                                        Map<String, Map<String, DistanceInfo>> distanceMatrix,
                                        String startLocation) {
        var optimized = optimizeOneDirection(destinations, distanceMatrix, startLocation);
        // 순환을 위해 시작점을 마지막에 추가 (필요시)
        if (startLocation != null && !startLocation.isEmpty() && !optimized.get(optimized.size() - 1).equals(startLocation)) {
            optimized.add(startLocation);
        }
        return optimized;
    }

    // 가장 가까운 도시 찾기
    private String findNearestCity(String currentLocation, Set<String> candidates, 
                                 Map<String, Map<String, DistanceInfo>> distanceMatrix) {
        return candidates.stream()
            .min((city1, city2) -> {
                var distance1 = getDistance(currentLocation, city1, distanceMatrix);
                var distance2 = getDistance(currentLocation, city2, distanceMatrix);
                return Double.compare(distance1, distance2);
            })
            .orElse(candidates.iterator().next());
    }

    // 두 지점 간 거리 조회
    private double getDistance(String from, String to, Map<String, Map<String, DistanceInfo>> distanceMatrix) {
        return distanceMatrix.getOrDefault(from, Map.of())
                           .getOrDefault(to, new DistanceInfo(100.0, 120))
                           .distance();
    }

    // 경로 세그먼트 생성
    private List<OptimizedRoute.RouteSegment> createRouteSegments(List<String> optimizedOrder,
                                                                Map<String, Map<String, DistanceInfo>> distanceMatrix) {
        var segments = new ArrayList<OptimizedRoute.RouteSegment>();

        for (int i = 0; i < optimizedOrder.size() - 1; i++) {
            var from = optimizedOrder.get(i);
            var to = optimizedOrder.get(i + 1);
            var distanceInfo = distanceMatrix.getOrDefault(from, Map.of())
                                           .getOrDefault(to, new DistanceInfo(0.0, 0));

            segments.add(new OptimizedRoute.RouteSegment(
                from, to, distanceInfo.distance(), distanceInfo.time(), "자동차"
            ));
        }

        return segments;
    }

    // 총 거리 계산
    private double calculateTotalDistance(List<OptimizedRoute.RouteSegment> segments) {
        return segments.stream()
                      .mapToDouble(segment -> segment.distance() != null ? segment.distance() : 0.0)
                      .sum();
    }

    // 총 시간 계산
    private int calculateTotalTime(List<OptimizedRoute.RouteSegment> segments) {
        return segments.stream()
                      .mapToInt(segment -> segment.travelTime() != null ? segment.travelTime() : 0)
                      .sum();
    }

    // 거리 정보 내부 클래스
    private record DistanceInfo(double distance, int time) {}
}
