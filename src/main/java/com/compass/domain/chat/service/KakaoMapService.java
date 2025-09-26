package com.compass.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoMapService {

    private static final String KAKAO_LOCAL_API_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    @Value("${kakao.rest.key:}")
    private String kakaoRestKey;

    private final WebClient webClient;

    // 장소명으로 주소 검색
    public String searchAddressByPlaceName(String placeName, String region) {
        try {
            // 검색 쿼리 구성 (지역 정보 포함)
            String query = region != null && !region.isEmpty()
                ? region + " " + placeName
                : placeName;

            log.info("카카오맵 API로 주소 검색: {}", query);

            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(KAKAO_LOCAL_API_URL)
                    .queryParam("query", query)
                    .queryParam("size", 1)  // 가장 정확한 1개만
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("documents")) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");

                if (!documents.isEmpty()) {
                    Map<String, Object> firstResult = documents.get(0);

                    // 도로명 주소 우선, 없으면 지번 주소
                    String roadAddress = (String) firstResult.get("road_address_name");
                    String address = roadAddress != null && !roadAddress.isEmpty()
                        ? roadAddress
                        : (String) firstResult.get("address_name");

                    log.info("검색된 주소: {}", address);
                    return address;
                }
            }

            log.warn("카카오맵 API에서 주소를 찾을 수 없음: {}", query);
            return null;

        } catch (Exception e) {
            log.error("카카오맵 API 호출 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    // 좌표로 주소 검색 (역지오코딩)
    public String searchAddressByCoordinates(Double latitude, Double longitude) {
        try {
            log.info("좌표로 주소 검색: lat={}, lng={}", latitude, longitude);

            String url = "https://dapi.kakao.com/v2/local/geo/coord2address.json";

            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(url)
                    .queryParam("x", longitude)  // 카카오는 x가 경도
                    .queryParam("y", latitude)   // y가 위도
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("documents")) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");

                if (!documents.isEmpty()) {
                    Map<String, Object> firstResult = documents.get(0);

                    // 도로명 주소 정보
                    Map<String, Object> roadAddress = (Map<String, Object>) firstResult.get("road_address");
                    if (roadAddress != null) {
                        String address = (String) roadAddress.get("address_name");
                        log.info("좌표에서 변환된 도로명 주소: {}", address);
                        return address;
                    }

                    // 도로명 주소가 없으면 지번 주소
                    Map<String, Object> jibunAddress = (Map<String, Object>) firstResult.get("address");
                    if (jibunAddress != null) {
                        String address = (String) jibunAddress.get("address_name");
                        log.info("좌표에서 변환된 지번 주소: {}", address);
                        return address;
                    }
                }
            }

            log.warn("좌표에서 주소를 찾을 수 없음: lat={}, lng={}", latitude, longitude);
            return null;

        } catch (Exception e) {
            log.error("카카오맵 역지오코딩 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }

    // 장소 상세 정보 검색
    public Map<String, Object> searchPlaceDetail(String placeName, String region) {
        try {
            String query = region != null && !region.isEmpty()
                ? region + " " + placeName
                : placeName;

            log.info("카카오맵 API로 장소 상세 정보 검색: {}", query);

            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path(KAKAO_LOCAL_API_URL)
                    .queryParam("query", query)
                    .queryParam("size", 1)
                    .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoRestKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("documents")) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");

                if (!documents.isEmpty()) {
                    Map<String, Object> place = documents.get(0);

                    // 필요한 정보만 추출
                    Map<String, Object> placeInfo = Map.of(
                        "place_name", place.getOrDefault("place_name", ""),
                        "address_name", place.getOrDefault("address_name", ""),
                        "road_address_name", place.getOrDefault("road_address_name", ""),
                        "phone", place.getOrDefault("phone", ""),
                        "place_url", place.getOrDefault("place_url", ""),
                        "category_name", place.getOrDefault("category_name", ""),
                        "x", place.getOrDefault("x", ""),  // 경도
                        "y", place.getOrDefault("y", "")   // 위도
                    );

                    log.info("검색된 장소 정보: {}", placeInfo);
                    return placeInfo;
                }
            }

            log.warn("카카오맵 API에서 장소 정보를 찾을 수 없음: {}", query);
            return null;

        } catch (Exception e) {
            log.error("카카오맵 API 장소 상세 검색 중 오류: {}", e.getMessage(), e);
            return null;
        }
    }
}