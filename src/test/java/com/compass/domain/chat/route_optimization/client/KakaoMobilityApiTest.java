package com.compass.domain.chat.route_optimization.client;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

@DisplayName("카카오 모빌리티 API 테스트")
@Disabled("Spring Context 로드 문제 해결 필요")
public class KakaoMobilityApiTest {

    @Test
    public void testDirectApiCall() {
        String apiKey = System.getenv("KAKAO_REST_KEY");
        String baseUrl = "https://apis-navi.kakaomobility.com";

        RestTemplate restTemplate = new RestTemplate();

        // 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + apiKey);
        headers.set("Content-Type", "application/json");

        // 요청 바디 생성 - 서울시청에서 명동까지
        Map<String, Object> body = new HashMap<>();
        body.put("origin", Map.of("x", 126.9780, "y", 37.5665));  // 서울시청
        body.put("destination", Map.of("x", 126.9869, "y", 37.5636));  // 명동
        body.put("priority", "RECOMMEND");
        body.put("car_type", 1);
        body.put("car_fuel", "GASOLINE");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        System.out.println("=== 카카오 모빌리티 API 직접 호출 테스트 ===");
        System.out.println("URL: " + baseUrl + "/v1/directions");
        System.out.println("Headers: " + headers);
        System.out.println("Body: " + body);

        try {
            // v1/directions (자동차 길찾기) 시도
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/v1/directions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class,
                "origin=" + body.get("origin") + "&destination=" + body.get("destination") + "&priority=RECOMMEND"
            );

            System.out.println("✅ API 호출 성공!");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Response: " + response.getBody());

        } catch (HttpClientErrorException e) {
            System.out.println("❌ HTTP 에러: " + e.getStatusCode());
            System.out.println("에러 메시지: " + e.getResponseBodyAsString());

            // 다른 엔드포인트 시도 - waypoints/directions
            System.out.println("\n다중 경유지 API 시도...");
            try {
                Map<String, Object> waypointsBody = new HashMap<>();
                waypointsBody.put("origin", Map.of("x", 126.9780, "y", 37.5665));
                waypointsBody.put("destination", Map.of("x", 126.9869, "y", 37.5636));
                waypointsBody.put("waypoints", List.of());
                waypointsBody.put("priority", "RECOMMEND");

                HttpEntity<Map<String, Object>> waypointsEntity = new HttpEntity<>(waypointsBody, headers);

                ResponseEntity<Map> waypointsResponse = restTemplate.exchange(
                    baseUrl + "/v1/waypoints/directions",
                    HttpMethod.POST,
                    waypointsEntity,
                    Map.class
                );

                System.out.println("✅ waypoints API 호출 성공!");
                System.out.println("Status: " + waypointsResponse.getStatusCode());
                System.out.println("Response: " + waypointsResponse.getBody());

            } catch (Exception e2) {
                System.out.println("❌ waypoints API도 실패: " + e2.getMessage());
                if (e2 instanceof HttpClientErrorException) {
                    System.out.println("에러 응답: " + ((HttpClientErrorException) e2).getResponseBodyAsString());
                }
            }

        } catch (Exception e) {
            System.out.println("❌ 예상치 못한 에러: " + e.getClass().getName());
            System.out.println("에러 메시지: " + e.getMessage());
            e.printStackTrace();
        }
    }
}