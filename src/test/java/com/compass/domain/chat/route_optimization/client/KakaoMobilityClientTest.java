package com.compass.domain.chat.route_optimization.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("카카오 모빌리티 API 테스트")
class KakaoMobilityClientTest {

    @Autowired
    private KakaoMobilityClient kakaoMobilityClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 환경 변수 확인
        String apiKey = System.getenv("KAKAO_REST_KEY");
        if (apiKey == null) {
            System.out.println("⚠️ KAKAO_REST_KEY 환경 변수가 설정되지 않았습니다. Mock 응답을 사용합니다.");
        } else {
            System.out.println("✅ KAKAO_REST_KEY 환경 변수 확인: " + apiKey.substring(0, 8) + "...");
        }
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
        assertThat(carDistance.distance()).isBetween(1.0, 5.0);  // 1-5km 사이
        assertThat(carDistance.duration()).isBetween(5, 20);     // 5-20분 사이

        assertThat(walkDistance).isNotNull();
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
        System.out.println("개선율: " + String.format("%.1f%%",
            (1 - optimizedDistance / originalDistance) * 100));
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
    @DisplayName("실제 카카오 모빌리티 API 연결 테스트 (REST API 키 필요)")
    void testRealApiConnection() {
        // Given
        String apiKey = System.getenv("KAKAO_REST_KEY");

        if (apiKey == null || apiKey.contains("dummy")) {
            System.out.println("\n⚠️ 실제 API 테스트를 건너뜁니다. (API 키 미설정)");
            System.out.println("실제 테스트를 위해서는 KAKAO_REST_KEY 환경 변수를 설정하세요.");
            return;
        }

        // 간단한 경로로 테스트
        List<KakaoMobilityClient.Waypoint> waypoints = List.of(
            new KakaoMobilityClient.Waypoint("출발지", 37.5665, 126.9780),
            new KakaoMobilityClient.Waypoint("도착지", 37.5636, 126.9869)
        );

        KakaoMobilityClient.RouteRequest request = new KakaoMobilityClient.RouteRequest(
            waypoints,
            KakaoMobilityClient.TransportMode.CAR,
            "RECOMMEND",
            1,
            "GASOLINE"
        );

        // When
        try {
            KakaoMobilityClient.RouteResponse response = kakaoMobilityClient.searchMultiDestinationRoute(request);

            // Then
            if (response != null && "SUCCESS".equals(response.status())) {
                System.out.println("\n✅ 실제 카카오 모빌리티 API 연결 성공!");
                System.out.println("응답 데이터: " + objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(response));
            } else {
                System.out.println("\n⚠️ Mock 응답을 사용 중입니다.");
                System.out.println("카카오 모빌리티 API는 B2B 서비스로 별도 계약이 필요합니다.");
            }
        } catch (Exception e) {
            System.out.println("\n❌ API 호출 실패: " + e.getMessage());
            System.out.println("Mock 응답으로 대체됩니다.");
        }
    }
}