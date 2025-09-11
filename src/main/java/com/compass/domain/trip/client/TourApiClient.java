package com.compass.domain.trip.client;

import com.compass.domain.trip.dto.TourApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * 한국관광공사 Tour API 클라이언트
 * REQ-CRAWL-001: Tour API 클라이언트 구현
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TourApiClient {

    private final RestTemplate restTemplate;
    
    @Value("${tour.api.base-url:https://apis.data.go.kr/B551011/KorService2}")
    private String baseUrl;
    
    @Value("${tour.api.service-key:a1276cb5e93f8b11431d3d2fbfe5e843c3da51d2cd537c50170ea15e87f343ff}")
    private String serviceKey;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 지역 기반 관광지 목록 조회
     */
    public Optional<TourApiResponse> getAreaBasedList(String areaCode, String contentTypeId, int pageNo, int numOfRows) {
        try {
            String url = String.format("%s/areaBasedList2?serviceKey=%s&numOfRows=%d&pageNo=%d&MobileOS=ETC&MobileApp=Compass&_type=json&areaCode=%s&contentTypeId=%s",
                baseUrl, serviceKey, numOfRows, pageNo, areaCode, contentTypeId);
            
            log.debug("Tour API 호출: {}", url);
            
            ResponseEntity<TourApiResponse> response = restTemplate.getForEntity(url, TourApiResponse.class);
            TourApiResponse apiResponse = response.getBody();
            
            if (apiResponse != null && apiResponse.getResponse() != null) {
                return Optional.of(apiResponse);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Tour API 호출 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 관광지 상세 정보 조회
     */
    public Optional<TourApiResponse> getDetailCommon(String contentId) {
        try {
            String url = String.format("%s/detailCommon?serviceKey=%s&contentId=%s&defaultYN=Y&firstImageYN=Y&areacodeYN=Y&catcodeYN=Y&addrinfoYN=Y&mapinfoYN=Y&overviewYN=Y&MobileOS=ETC&MobileApp=Compass&_type=json",
                baseUrl, serviceKey, contentId);
            
            log.debug("Tour API 상세 조회: {}", url);
            
            ResponseEntity<TourApiResponse> response = restTemplate.getForEntity(url, TourApiResponse.class);
            TourApiResponse apiResponse = response.getBody();
            
            if (apiResponse != null && apiResponse.getResponse() != null) {
                return Optional.of(apiResponse);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Tour API 상세 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 위치 기반 관광지 목록 조회
     */
    public Optional<TourApiResponse> getLocationBasedList(String mapX, String mapY, String radius, String contentTypeId, int pageNo, int numOfRows) {
        try {
            String url = String.format("%s/locationBasedList?serviceKey=%s&numOfRows=%d&pageNo=%d&MobileOS=ETC&MobileApp=Compass&_type=json&mapX=%s&mapY=%s&radius=%s&contentTypeId=%s",
                baseUrl, serviceKey, numOfRows, pageNo, mapX, mapY, radius, contentTypeId);
            
            log.debug("Tour API 위치 기반 조회: {}", url);
            
            ResponseEntity<TourApiResponse> response = restTemplate.getForEntity(url, TourApiResponse.class);
            TourApiResponse apiResponse = response.getBody();
            
            if (apiResponse != null && apiResponse.getResponse() != null) {
                return Optional.of(apiResponse);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Tour API 위치 기반 조회 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 키워드 검색
     */
    public Optional<TourApiResponse> searchKeyword(String keyword, String contentTypeId, int pageNo, int numOfRows) {
        try {
            String url = String.format("%s/searchKeyword?serviceKey=%s&numOfRows=%d&pageNo=%d&MobileOS=ETC&MobileApp=Compass&_type=json&keyword=%s&contentTypeId=%s",
                baseUrl, serviceKey, numOfRows, pageNo, keyword, contentTypeId);
            
            log.debug("Tour API 키워드 검색: {}", url);
            
            ResponseEntity<TourApiResponse> response = restTemplate.getForEntity(url, TourApiResponse.class);
            TourApiResponse apiResponse = response.getBody();
            
            if (apiResponse != null && apiResponse.getResponse() != null) {
                return Optional.of(apiResponse);
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Tour API 키워드 검색 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
