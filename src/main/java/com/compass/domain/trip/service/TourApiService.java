package com.compass.domain.trip.service;

import com.compass.domain.trip.client.TourApiClient;
import com.compass.domain.trip.config.TourApiProperties;
import com.compass.domain.trip.dto.TourApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tour API 서비스
 * Seoul JSON 데이터를 보완하고 실시간 관광 정보를 제공
 */
@Service
public class TourApiService {
    
    private static final Logger log = LoggerFactory.getLogger(TourApiService.class);
    
    private final TourApiClient tourApiClient;
    
    public TourApiService(TourApiClient tourApiClient, TourApiProperties properties) {
        this.tourApiClient = tourApiClient;
        // properties는 현재 사용하지 않지만 향후 확장을 위해 파라미터로 유지
    }
    
    /**
     * Seoul JSON 카테고리를 Tour API ContentTypeId로 매핑
     */
    public String mapCategoryToContentTypeId(String seoulCategory) {
        return switch (seoulCategory.toLowerCase()) {
            case "palace", "historic gate", "unesco site", "historic trail", "fortress" -> 
                TourApiProperties.TOURIST_SPOT; // 12: 관광지
            case "museum", "theater", "arts complex" -> 
                TourApiProperties.CULTURAL_FACILITY; // 14: 문화시설
            case "shopping mall", "shopping district", "shopping street", "shopping", "traditional market", "market" -> 
                TourApiProperties.SHOPPING; // 38: 쇼핑
            case "food alley", "food street" -> 
                TourApiProperties.RESTAURANT; // 39: 음식점
            case "sports venue", "sports", "theme park", "attraction" -> 
                TourApiProperties.LEISURE_SPORTS; // 28: 레포츠
            default -> TourApiProperties.TOURIST_SPOT; // 기본값: 관광지
        };
    }
    
    /**
     * 서울 지역 관광지 정보 조회 (Seoul JSON 보완용)
     */
    public List<TourApiResponse.TourItem> getSeoulTouristSpots(int pageNo, int numOfRows) {
        String seoulAreaCode = "1"; // 서울 지역코드
        String touristSpotTypeId = TourApiProperties.TOURIST_SPOT; // 관광지
        
        Optional<TourApiResponse> response = tourApiClient.getAreaBasedList(
                seoulAreaCode, touristSpotTypeId, pageNo, numOfRows);
        
        return extractTourItems(response);
    }
    
    /**
     * 서울 지역 음식점 정보 조회 (Seoul JSON 보완용)
     */
    public List<TourApiResponse.TourItem> getSeoulRestaurants(int pageNo, int numOfRows) {
        String seoulAreaCode = "1"; // 서울 지역코드
        String restaurantTypeId = TourApiProperties.RESTAURANT; // 음식점
        
        Optional<TourApiResponse> response = tourApiClient.getAreaBasedList(
                seoulAreaCode, restaurantTypeId, pageNo, numOfRows);
        
        return extractTourItems(response);
    }
    
    /**
     * 서울 지역 쇼핑 정보 조회 (Seoul JSON 보완용)
     */
    public List<TourApiResponse.TourItem> getSeoulShopping(int pageNo, int numOfRows) {
        String seoulAreaCode = "1"; // 서울 지역코드
        String shoppingTypeId = TourApiProperties.SHOPPING; // 쇼핑
        
        Optional<TourApiResponse> response = tourApiClient.getAreaBasedList(
                seoulAreaCode, shoppingTypeId, pageNo, numOfRows);
        
        return extractTourItems(response);
    }
    
    /**
     * 카테고리별 서울 관광지 조회
     */
    public List<TourApiResponse.TourItem> getSeoulByCategory(String seoulCategory, int pageNo, int numOfRows) {
        String contentTypeId = mapCategoryToContentTypeId(seoulCategory);
        String seoulAreaCode = "1";
        
        Optional<TourApiResponse> response = tourApiClient.getAreaBasedList(
                seoulAreaCode, contentTypeId, pageNo, numOfRows);
        
        return extractTourItems(response);
    }
    
    /**
     * 관광지 상세 정보 조회 (Seoul JSON에 없는 정보 보완)
     */
    public Optional<TourApiResponse.TourItem> getPlaceDetail(String contentId, String contentTypeId) {
        Optional<TourApiResponse> response = tourApiClient.getDetailCommon(contentId, contentTypeId);
        
        if (response.isPresent()) {
            List<TourApiResponse.TourItem> items = extractTourItems(response);
            return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
        }
        
        return Optional.empty();
    }
    
    /**
     * 위치 기반 근처 관광지 조회 (Seoul JSON 근거리 검색 보완)
     */
    public List<TourApiResponse.TourItem> getNearbyPlaces(double latitude, double longitude, 
                                                         int radiusMeters, String contentTypeId) {
        // Tour API는 경도(mapX), 위도(mapY) 순서
        String mapX = String.valueOf(longitude); // 경도
        String mapY = String.valueOf(latitude);  // 위도
        
        Optional<TourApiResponse> response = tourApiClient.getLocationBasedList(
                mapX, mapY, radiusMeters, contentTypeId);
        
        return extractTourItems(response);
    }
    
    /**
     * 키워드 기반 관광지 검색 (Seoul JSON tags 보완)
     */
    public List<TourApiResponse.TourItem> searchByKeyword(String keyword, String contentTypeId) {
        String seoulAreaCode = "1"; // 서울로 제한
        
        Optional<TourApiResponse> response = tourApiClient.searchKeyword(
                keyword, seoulAreaCode, contentTypeId);
        
        return extractTourItems(response);
    }
    
    /**
     * 전체 서울 관광 정보 수집 (Phase별 크롤링용)
     */
    public List<TourApiResponse.TourItem> collectAllSeoulData() {
        List<TourApiResponse.TourItem> allItems = new ArrayList<>();
        
        // 관광지
        allItems.addAll(getSeoulTouristSpots(1, 100));
        log.info("관광지 수집 완료: {}개", allItems.size());
        
        // 문화시설
        allItems.addAll(getSeoulByCategory("museum", 1, 100));
        log.info("문화시설 추가, 총 {}개", allItems.size());
        
        // 음식점
        allItems.addAll(getSeoulRestaurants(1, 100));
        log.info("음식점 추가, 총 {}개", allItems.size());
        
        // 쇼핑
        allItems.addAll(getSeoulShopping(1, 100));
        log.info("쇼핑 추가, 총 {}개", allItems.size());
        
        log.info("서울 전체 데이터 수집 완료: 총 {}개 관광지", allItems.size());
        
        return allItems;
    }
    
    /**
     * Tour API 응답에서 TourItem 리스트 추출
     */
    private List<TourApiResponse.TourItem> extractTourItems(Optional<TourApiResponse> response) {
        if (response.isEmpty()) {
            return new ArrayList<>();
        }
        
        TourApiResponse apiResponse = response.get();
        if (apiResponse.getResponse() == null || 
            apiResponse.getResponse().getBody() == null ||
            apiResponse.getResponse().getBody().getItems() == null ||
            apiResponse.getResponse().getBody().getItems().getItem() == null) {
            return new ArrayList<>();
        }
        
        return apiResponse.getResponse().getBody().getItems().getItem();
    }
    
    /**
     * Seoul JSON 좌표를 기반으로 Tour API에서 추가 정보 검색
     */
    public List<TourApiResponse.TourItem> enrichSeoulJsonData(double seoulLat, double seoulLng, String seoulCategory) {
        String contentTypeId = mapCategoryToContentTypeId(seoulCategory);
        int radiusMeters = 500; // 500m 반경
        
        return getNearbyPlaces(seoulLat, seoulLng, radiusMeters, contentTypeId);
    }
}
