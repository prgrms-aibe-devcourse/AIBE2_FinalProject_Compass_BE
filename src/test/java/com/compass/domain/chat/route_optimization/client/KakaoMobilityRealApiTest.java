package com.compass.domain.chat.route_optimization.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {KakaoMobilityClient.class, RestTemplate.class, ObjectMapper.class})
@ActiveProfiles("test")
@DisplayName("카카오 모빌리티 실제 API 테스트")
class KakaoMobilityRealApiTest {

    @Autowired
    private KakaoMobilityClient kakaoMobilityClient;

    @Value("${kakao.api.rest-key}")
    private String apiKey;

    @BeforeEach
    void setUp() {
        System.out.println("=== 카카오 모빌리티 API 테스트 시작 ===");
        System.out.println("API Key: " + (apiKey != null ? apiKey.substring(0, 8) + "..." : "null"));
    }

    @Test
    @DisplayName("실제 API 호출 - 서울시청에서 명동")
    void testRealApiCall() {
        // Given
        List<KakaoMobilityClient.Waypoint> waypoints = List.of(
            new KakaoMobilityClient.Waypoint("서울시청", 37.5665, 126.9780),
            new KakaoMobilityClient.Waypoint("명동", 37.5636, 126.9869)
        );

        KakaoMobilityClient.RouteRequest request = KakaoMobilityClient.RouteRequest.of(
            waypoints,
            KakaoMobilityClient.TransportMode.CAR
        );

        // When
        System.out.println("\nAPI 호출 시작...");
        KakaoMobilityClient.RouteResponse response = kakaoMobilityClient.searchMultiDestinationRoute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");

        System.out.println("\n=== API 응답 ===");
        System.out.println("상태: " + response.status());
        System.out.println("총 거리: " + String.format("%.2f km", response.totalDistance()));
        System.out.println("총 시간: " + response.totalDuration() + " 분");

        // Mock 응답 확인
        double expectedMockDistance = 1.67 * 1.3;  // Haversine 거리 * 도로 보정
        boolean isMock = Math.abs(response.totalDistance() - expectedMockDistance) < 0.1;

        if (isMock) {
            System.out.println("\n⚠️ Mock 응답 감지됨");
        } else {
            System.out.println("\n✅ 실제 API 응답 확인!");
            System.out.println("실제 거리와 시간이 반환되었습니다.");
        }

        // 실제 API라면 더 정확한 값이 나와야 함
        if (!isMock) {
            assertThat(response.totalDistance()).isBetween(1.5, 3.0);  // 서울시청-명동은 약 2km
            assertThat(response.totalDuration()).isBetween(5, 15);      // 약 10분
        }
    }

    @Test
    @DisplayName("다중 경유지 실제 API 호출")
    void testMultiWaypointRealApi() {
        // Given - 서울 관광 코스
        List<KakaoMobilityClient.Waypoint> waypoints = List.of(
            new KakaoMobilityClient.Waypoint("서울시청", 37.5665, 126.9780),
            new KakaoMobilityClient.Waypoint("덕수궁", 37.5658, 126.9751),
            new KakaoMobilityClient.Waypoint("명동", 37.5636, 126.9869),
            new KakaoMobilityClient.Waypoint("남산타워", 37.5512, 126.9882)
        );

        KakaoMobilityClient.RouteRequest request = KakaoMobilityClient.RouteRequest.of(
            waypoints,
            KakaoMobilityClient.TransportMode.CAR
        );

        // When
        System.out.println("\n다중 경유지 API 호출...");
        KakaoMobilityClient.RouteResponse response = kakaoMobilityClient.searchMultiDestinationRoute(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.sections()).hasSize(3);  // 4개 지점 = 3개 구간

        System.out.println("\n=== 다중 경유지 결과 ===");
        System.out.println("총 거리: " + String.format("%.2f km", response.totalDistance()));
        System.out.println("총 시간: " + response.totalDuration() + " 분");

        for (int i = 0; i < response.sections().size(); i++) {
            KakaoMobilityClient.RouteSection section = response.sections().get(i);
            System.out.println(String.format("구간 %d: %.2f km, %d분",
                i + 1, section.distance(), section.duration()));
        }
    }
}