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

import java.net.URI;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoMapClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kakao.api.rest-key:dummy-rest-key}")
    private String restApiKey;

    private static final String BASE_URL = "https://dapi.kakao.com";
    private static final String SEARCH_KEYWORD_PATH = "/v2/local/search/keyword.json";
    private static final String SEARCH_CATEGORY_PATH = "/v2/local/search/category.json";
    private static final String COORD_TO_ADDRESS_PATH = "/v2/local/geo/coord2address.json";

    // 키워드로 장소 검색
    public PlaceSearchResponse searchPlacesByKeyword(String query, double x, double y, int radius) {
        log.info("카카오맵 키워드 검색: query={}, 중심좌표=({},{}), 반경={}m", query, x, y, radius);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + restApiKey);

            URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + SEARCH_KEYWORD_PATH)
                .queryParam("query", query)
                .queryParam("x", x)  // 경도
                .queryParam("y", y)  // 위도
                .queryParam("radius", radius)
                .queryParam("size", 15)
                .queryParam("sort", "distance")
                .build()
                .encode()
                .toUri();

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                return parseSearchResponse(response.getBody());
            }
        } catch (Exception e) {
            log.error("카카오맵 API 호출 실패", e);
        }

        return new PlaceSearchResponse(Collections.emptyList(), 0);
    }

    // 카테고리로 장소 검색
    public PlaceSearchResponse searchPlacesByCategory(String categoryCode, double x, double y, int radius) {
        log.info("카카오맵 카테고리 검색: category={}, 중심좌표=({},{})", categoryCode, x, y);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + restApiKey);

            URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + SEARCH_CATEGORY_PATH)
                .queryParam("category_group_code", categoryCode)
                .queryParam("x", x)
                .queryParam("y", y)
                .queryParam("radius", radius)
                .queryParam("size", 15)
                .queryParam("sort", "distance")
                .build()
                .encode()
                .toUri();

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                return parseSearchResponse(response.getBody());
            }
        } catch (Exception e) {
            log.error("카카오맵 카테고리 검색 실패", e);
        }

        return new PlaceSearchResponse(Collections.emptyList(), 0);
    }

    // 좌표로 주소 변환
    public String getAddressFromCoordinate(double x, double y) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + restApiKey);

            URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL + COORD_TO_ADDRESS_PATH)
                .queryParam("x", x)
                .queryParam("y", y)
                .build()
                .encode()
                .toUri();

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                return parseAddressResponse(response.getBody());
            }
        } catch (Exception e) {
            log.error("주소 변환 실패", e);
        }

        return null;
    }

    // 검색 응답 파싱
    @SuppressWarnings("unchecked")
    private PlaceSearchResponse parseSearchResponse(Map<String, Object> response) {
        List<Place> places = new ArrayList<>();

        List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
        if (documents != null) {
            for (Map<String, Object> doc : documents) {
                Place place = Place.builder()
                    .id((String) doc.get("id"))
                    .name((String) doc.get("place_name"))
                    .category((String) doc.get("category_name"))
                    .address((String) doc.get("address_name"))
                    .roadAddress((String) doc.get("road_address_name"))
                    .phone((String) doc.get("phone"))
                    .x(Double.parseDouble((String) doc.get("x")))  // 경도
                    .y(Double.parseDouble((String) doc.get("y")))  // 위도
                    .distance(doc.get("distance") != null ?
                        Integer.parseInt((String) doc.get("distance")) : null)
                    .placeUrl((String) doc.get("place_url"))
                    .build();
                places.add(place);
            }
        }

        Map<String, Object> meta = (Map<String, Object>) response.get("meta");
        int totalCount = meta != null ? (int) meta.get("total_count") : 0;

        return new PlaceSearchResponse(places, totalCount);
    }

    // 주소 응답 파싱
    @SuppressWarnings("unchecked")
    private String parseAddressResponse(Map<String, Object> response) {
        List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
        if (documents != null && !documents.isEmpty()) {
            Map<String, Object> address = (Map<String, Object>) documents.get(0).get("address");
            if (address != null) {
                return (String) address.get("address_name");
            }
        }
        return null;
    }

    // 카테고리 코드 상수
    public static class CategoryCode {
        public static final String TOURIST_ATTRACTION = "AT4";  // 관광명소
        public static final String RESTAURANT = "FD6";         // 음식점
        public static final String CAFE = "CE7";               // 카페
        public static final String ACCOMMODATION = "AD5";       // 숙박
        public static final String CULTURE = "CT1";            // 문화시설
        public static final String PARKING = "PK6";            // 주차장
        public static final String SUBWAY = "SW8";             // 지하철역
    }

    // 응답 모델
    public record PlaceSearchResponse(
        List<Place> places,
        int totalCount
    ) {}

    public record Place(
        String id,
        String name,
        String category,
        String address,
        String roadAddress,
        String phone,
        double x,  // 경도
        double y,  // 위도
        Integer distance,  // 중심점으로부터 거리(미터)
        String placeUrl
    ) {
        public static PlaceBuilder builder() {
            return new PlaceBuilder();
        }

        public static class PlaceBuilder {
            private String id;
            private String name;
            private String category;
            private String address;
            private String roadAddress;
            private String phone;
            private double x;
            private double y;
            private Integer distance;
            private String placeUrl;

            public PlaceBuilder id(String id) {
                this.id = id;
                return this;
            }

            public PlaceBuilder name(String name) {
                this.name = name;
                return this;
            }

            public PlaceBuilder category(String category) {
                this.category = category;
                return this;
            }

            public PlaceBuilder address(String address) {
                this.address = address;
                return this;
            }

            public PlaceBuilder roadAddress(String roadAddress) {
                this.roadAddress = roadAddress;
                return this;
            }

            public PlaceBuilder phone(String phone) {
                this.phone = phone;
                return this;
            }

            public PlaceBuilder x(double x) {
                this.x = x;
                return this;
            }

            public PlaceBuilder y(double y) {
                this.y = y;
                return this;
            }

            public PlaceBuilder distance(Integer distance) {
                this.distance = distance;
                return this;
            }

            public PlaceBuilder placeUrl(String placeUrl) {
                this.placeUrl = placeUrl;
                return this;
            }

            public Place build() {
                return new Place(id, name, category, address, roadAddress,
                    phone, x, y, distance, placeUrl);
            }
        }
    }
}