package com.compass.domain.chat.function.external;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Kakao Places API를 사용한 장소 검색
 * 무료 사용량: 일 300,000회
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchKakaoPlacesFunction {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${KAKAO_API_KEY:f576ff3ce0244e21af2931bda0f7233a}")
    private String kakaoApiKey;
    
    private static final String KAKAO_PLACES_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    
    /**
     * Kakao Places API로 장소 검색
     */
    public List<KakaoPlace> searchPlaces(String query, String category, int size) {
        try {
            log.info("Kakao Places API 검색 시작: query={}, category={}, size={}", query, category, size);
            
            // 요청 헤더 설정 (curl과 동일)
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            
            // 요청 URL 구성 (curl과 동일)
            String url = String.format("%s?query=%s&size=%d", 
                KAKAO_PLACES_URL, query, size);
            
            log.info("Kakao API 호출 URL: {}", url);
            log.info("Kakao API 키: {}", kakaoApiKey.substring(0, 8) + "...");
            log.info("요청 헤더: Authorization=KakaoAK {}", kakaoApiKey.substring(0, 8) + "...");
            
            // API 호출
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("Kakao API 응답 상태: {}", response.getStatusCode());
            log.info("Kakao API 응답 본문: {}", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseKakaoResponse(response.getBody());
            } else {
                log.error("Kakao API 호출 실패: {}", response.getStatusCode());
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            log.error("Kakao Places API 검색 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Kakao API 응답 파싱
     */
    private List<KakaoPlace> parseKakaoResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode documents = root.path("documents");
            
            List<KakaoPlace> places = new ArrayList<>();
            
            for (JsonNode doc : documents) {
                KakaoPlace place = KakaoPlace.builder()
                    .name(doc.path("place_name").asText())
                    .address(doc.path("address_name").asText())
                    .roadAddress(doc.path("road_address_name").asText())
                    .category(doc.path("category_name").asText())
                    .phone(doc.path("phone").asText())
                    .url(doc.path("place_url").asText())
                    .x(doc.path("x").asText()) // 경도
                    .y(doc.path("y").asText()) // 위도
                    .build();
                
                places.add(place);
            }
            
            log.info("Kakao API 검색 결과: {}개 장소", places.size());
            return places;
            
        } catch (Exception e) {
            log.error("Kakao API 응답 파싱 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 클러스터별 장소 검색
     */
    public List<KakaoPlace> searchPlacesByCluster(String clusterName, int size) {
        // 클러스터별 쿼리 생성 (영어로 변환)
        String query = getClusterQuery(clusterName);
        String category = "";
        
        log.info("클러스터 검색: clusterName={}, query={}, size={}", clusterName, query, size);
        
        return searchPlaces(query, category, size);
    }
    
    /**
     * 클러스터별 검색 쿼리 생성
     */
    private String getClusterQuery(String clusterName) {
        return switch (clusterName) {
            case "hongdae" -> "홍대 맛집";
            case "gangnam" -> "gangnam";
            case "sungsu" -> "sungsu";
            case "jongno" -> "jongno";
            case "itaewon" -> "itaewon";
            default -> clusterName;
        };
    }
    
    /**
     * 클러스터별 카테고리 설정
     */
    private String getClusterCategory(String clusterName) {
        return ""; // 카테고리 없이 검색
    }
    
    /**
     * Kakao Place 데이터 클래스
     */
    public static class KakaoPlace {
        private String name;
        private String address;
        private String roadAddress;
        private String category;
        private String phone;
        private String url;
        private String x; // 경도
        private String y; // 위도
        
        // Builder pattern
        public static KakaoPlaceBuilder builder() {
            return new KakaoPlaceBuilder();
        }
        
        public static class KakaoPlaceBuilder {
            private String name;
            private String address;
            private String roadAddress;
            private String category;
            private String phone;
            private String url;
            private String x;
            private String y;
            
            public KakaoPlaceBuilder name(String name) { this.name = name; return this; }
            public KakaoPlaceBuilder address(String address) { this.address = address; return this; }
            public KakaoPlaceBuilder roadAddress(String roadAddress) { this.roadAddress = roadAddress; return this; }
            public KakaoPlaceBuilder category(String category) { this.category = category; return this; }
            public KakaoPlaceBuilder phone(String phone) { this.phone = phone; return this; }
            public KakaoPlaceBuilder url(String url) { this.url = url; return this; }
            public KakaoPlaceBuilder x(String x) { this.x = x; return this; }
            public KakaoPlaceBuilder y(String y) { this.y = y; return this; }
            
            public KakaoPlace build() {
                KakaoPlace place = new KakaoPlace();
                place.name = this.name;
                place.address = this.address;
                place.roadAddress = this.roadAddress;
                place.category = this.category;
                place.phone = this.phone;
                place.url = this.url;
                place.x = this.x;
                place.y = this.y;
                return place;
            }
        }
        
        // Getters
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getRoadAddress() { return roadAddress; }
        public String getCategory() { return category; }
        public String getPhone() { return phone; }
        public String getUrl() { return url; }
        public String getX() { return x; }
        public String getY() { return y; }
    }
}