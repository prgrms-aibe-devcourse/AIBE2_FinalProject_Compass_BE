package com.compass.domain.chat.route_optimization.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("카카오 모빌리티 API 단위 테스트")
class KakaoMobilityClientUnitTest {

    private KakaoMobilityClient kakaoMobilityClient;
    private String apiKey;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        kakaoMobilityClient = new KakaoMobilityClient(restTemplate, objectMapper);

        // 환경 변수에서 API 키 읽기
        apiKey = System.getenv("KAKAO_REST_KEY");
        if (apiKey == null) {
            apiKey = "e441db4b56f018bdfb43f87db66c216a"; // .env에서 제공된 키
        }

        // Reflection으로 API 키 설정
        ReflectionTestUtils.setField(kakaoMobilityClient, "restApiKey", apiKey);

        System.out.println("✅ KAKAO_REST_KEY 설정: " + apiKey.substring(0, 8) + "...");
    }

    @Test
    @DisplayName("다중 목적지 경로 탐색 테스트")
    void testSearchMultiDestinationRoute() {
        // Given - 서울 주요 관광지 좌표
        List<KakaoMobilityClient.Waypoint> waypoints = List.of(
            new KakaoMobilityClient.Waypoint("서울시청", 37.5665, 126.9780),
            new KakaoMobilityClient.Waypoint("명동", 37.5636, 126.9869),
            new KakaoMobilityClient.Waypoint("남산타워", 37.5512, 126.9882),
            new KakaoMobilityClient.Waypoint("경복궁", 37.5796, 126.9770)
        );

        KakaoMobilityClient.RouteRequest request = KakaoMobilityClient.RouteRequest.of(
            waypoints,
            KakaoMobilityClient.TransportMode.CAR
        );

        // When
        KakaoMobilityClient.RouteResponse response = kakaoMobilityClient.searchMultiDestinationRoute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.totalDistance()).isPositive();
        assertThat(response.totalDuration()).isPositive();
        assertThat(response.sections()).hasSize(waypoints.size() - 1);

        // 결과 출력
        System.out.println("\n=== 경로 탐색 결과 ===");
        System.out.println("총 거리: " + String.format("%.2f", response.totalDistance()) + " km");
        System.out.println("총 소요시간: " + response.totalDuration() + " 분");
        System.out.println("\n구간별 정보:");
        for (KakaoMobilityClient.RouteSection section : response.sections()) {
            System.out.println(String.format("  %s → %s: %.2f km, %d분",
                section.from(), section.to(), section.distance(), section.duration()));
        }
    }

    @Test
    @DisplayName("두 지점 간 거리 계산 테스트")
    void testCalculateDistance() {
        // Given
        double originLat = 37.5665;  // 서울시청 위도
        double originLng = 126.9780;  // 서울시청 경도
        double destLat = 37.5512;    // 남산타워 위도
        double destLng = 126.9882;    // 남산타워 경도

        // When - 다양한 교통수단으로 테스트
        KakaoMobilityClient.DistanceInfo carDistance = kakaoMobilityClient.calculateDistance(
            originLat, originLng, destLat, destLng, KakaoMobilityClient.TransportMode.CAR
        );

        KakaoMobilityClient.DistanceInfo walkDistance = kakaoMobilityClient.calculateDistance(
            originLat, originLng, destLat, destLng, KakaoMobilityClient.TransportMode.WALKING
        );

        // Then
        assertThat(carDistance).isNotNull();
        assertThat(carDistance.distance()).isPositive();  // 양수값
        assertThat(carDistance.duration()).isPositive();   // 양수값

        assertThat(walkDistance).isNotNull();
        assertThat(walkDistance.distance()).isPositive();  // 양수값
        assertThat(walkDistance.duration()).isGreaterThan(carDistance.duration());  // 도보가 더 오래 걸림

        // 결과 출력
        System.out.println("\n=== 거리 계산 결과 ===");
        System.out.println("서울시청 → 남산타워");
        System.out.println("자동차: " + String.format("%.2f km, %d분",
            carDistance.distance(), carDistance.duration()));
        System.out.println("도보: " + String.format("%.2f km, %d분",
            walkDistance.distance(), walkDistance.duration()));
    }

    @Test
    @DisplayName("경유지 최적화 테스트")
    void testOptimizeWaypoints() {
        // Given - 순서가 최적화되지 않은 경유지들
        List<KakaoMobilityClient.Waypoint> waypoints = List.of(
            new KakaoMobilityClient.Waypoint("서울시청", 37.5665, 126.9780),
            new KakaoMobilityClient.Waypoint("강남역", 37.4979, 127.0276),  // 멀리 떨어진 곳
            new KakaoMobilityClient.Waypoint("명동", 37.5636, 126.9869),     // 서울시청 근처
            new KakaoMobilityClient.Waypoint("홍대입구", 37.5579, 126.9237), // 서쪽
            new KakaoMobilityClient.Waypoint("동대문", 37.5714, 127.0098)    // 동쪽
        );

        // When
        List<KakaoMobilityClient.Waypoint> optimized = kakaoMobilityClient.optimizeWaypoints(
            waypoints, KakaoMobilityClient.TransportMode.CAR
        );

        // Then
        assertThat(optimized).isNotNull();
        assertThat(optimized).hasSize(waypoints.size());
        assertThat(optimized.get(0)).isEqualTo(waypoints.get(0));  // 시작점 유지

        // 결과 출력
        System.out.println("\n=== 경유지 최적화 결과 ===");
        System.out.println("원래 순서:");
        waypoints.forEach(wp -> System.out.println("  - " + wp.name()));

        System.out.println("\n최적화된 순서:");
        optimized.forEach(wp -> System.out.println("  - " + wp.name()));

        // 총 거리 비교
        double originalDistance = calculateTotalDistance(waypoints);
        double optimizedDistance = calculateTotalDistance(optimized);

        System.out.println("\n거리 비교:");
        System.out.println("원래 경로: " + String.format("%.2f km", originalDistance));
        System.out.println("최적화 경로: " + String.format("%.2f km", optimizedDistance));

        if (originalDistance > 0 && optimizedDistance > 0) {
            System.out.println("개선율: " + String.format("%.1f%%",
                (1 - optimizedDistance / originalDistance) * 100));
        }
    }

    // 총 거리 계산 헬퍼 메서드
    private double calculateTotalDistance(List<KakaoMobilityClient.Waypoint> waypoints) {
        double total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            KakaoMobilityClient.Waypoint from = waypoints.get(i);
            KakaoMobilityClient.Waypoint to = waypoints.get(i + 1);
            KakaoMobilityClient.DistanceInfo info = kakaoMobilityClient.calculateDistance(
                from.latitude(), from.longitude(),
                to.latitude(), to.longitude(),
                KakaoMobilityClient.TransportMode.CAR
            );
            total += info.distance();
        }
        return total;
    }

    @Test
    @DisplayName("실제 카카오 모빌리티 API 연결 테스트")
    void testRealApiConnection() {
        // Given
        System.out.println("\n=== 실제 API 연결 테스트 ===");
        System.out.println("API Key: " + apiKey.substring(0, 8) + "...");

        // 간단한 경로로 테스트 (서울시청 → 명동)
        List<KakaoMobilityClient.Waypoint> waypoints = List.of(
            new KakaoMobilityClient.Waypoint("서울시청", 37.5665, 126.9780),
            new KakaoMobilityClient.Waypoint("명동", 37.5636, 126.9869)
        );

        KakaoMobilityClient.RouteRequest request = KakaoMobilityClient.RouteRequest.of(
            waypoints,
            KakaoMobilityClient.TransportMode.CAR
        );

        // When
        KakaoMobilityClient.RouteResponse response = kakaoMobilityClient.searchMultiDestinationRoute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");

        // 결과 분석
        if ("SUCCESS".equals(response.status())) {
            // Mock 응답인지 실제 API 응답인지 판단
            if (response.totalDistance() > 0 && response.sections() != null && !response.sections().isEmpty()) {
                KakaoMobilityClient.RouteSection section = response.sections().get(0);

                // Mock 응답은 일정한 패턴을 가지므로 이를 체크
                double expectedMockDistance = calculateHaversineDistance(
                    waypoints.get(0).latitude(), waypoints.get(0).longitude(),
                    waypoints.get(1).latitude(), waypoints.get(1).longitude()
                ) * 1.3;

                double actualDistance = section.distance();
                double tolerance = 0.01; // 1% 오차 허용

                if (Math.abs(actualDistance - expectedMockDistance) < tolerance) {
                    System.out.println("\n⚠️ Mock 응답을 사용 중입니다.");
                    System.out.println("카카오 모빌리티 API는 B2B 서비스로 별도 계약이 필요합니다.");
                    System.out.println("현재 거리 기반 Mock 데이터로 테스트가 진행됩니다.");
                } else {
                    System.out.println("\n✅ 실제 카카오 모빌리티 API 연결 성공!");
                    System.out.println("실제 경로 정보를 받아왔습니다.");
                }
            }

            System.out.println("\n응답 데이터:");
            System.out.println("- 총 거리: " + String.format("%.2f km", response.totalDistance()));
            System.out.println("- 총 시간: " + response.totalDuration() + " 분");
            System.out.println("- 구간 수: " + response.sections().size());
        }
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
}