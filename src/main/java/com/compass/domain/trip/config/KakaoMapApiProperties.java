package com.compass.domain.trip.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kakao Map API 설정 Properties
 * REQ-SEARCH-003: Kakao Map API 검색
 */
@Data
@Component
@ConfigurationProperties(prefix = "kakao.map.api")
public class KakaoMapApiProperties {
    
    private String baseUrl;
    private String restApiKey;
    private Search search = new Search();
    private Default defaults = new Default();
    
    @Data
    public static class Search {
        private String keyword;
        private String category;
        private String address;
        private String coord2address;
        private String address2coord;
    }
    
    @Data
    public static class Default {
        private int size = 15;
        private int page = 1;
        private String sort = "accuracy";
    }
}
