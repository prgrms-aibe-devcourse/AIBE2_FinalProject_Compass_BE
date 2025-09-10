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
     * Seoul JSON 177개를 1000개 이상으로 확장
     */
    public List<TourApiResponse.TourItem> collectAllSeoulData() {
        List<TourApiResponse.TourItem> allItems = new ArrayList<>();
        
        log.info("=== 서울 대용량 데이터 수집 시작 ===");
        
        // 1. 관광지 (여러 페이지 수집)
        allItems.addAll(collectMultiplePages("12", "관광지", 5)); // 5페이지 = 500개
        
        // 2. 문화시설 
        allItems.addAll(collectMultiplePages("14", "문화시설", 3)); // 3페이지 = 300개
        
        // 3. 음식점
        allItems.addAll(collectMultiplePages("39", "음식점", 3)); // 3페이지 = 300개
        
        // 4. 쇼핑
        allItems.addAll(collectMultiplePages("38", "쇼핑", 2)); // 2페이지 = 200개
        
        // 5. 레포츠/액티비티
        allItems.addAll(collectMultiplePages("28", "레포츠", 2)); // 2페이지 = 200개
        
        // 6. 숙박시설
        allItems.addAll(collectMultiplePages("32", "숙박", 1)); // 1페이지 = 100개
        
        // 중복 제거 (contentId 기준)
        List<TourApiResponse.TourItem> uniqueItems = removeDuplicates(allItems);
        
        log.info("=== 서울 데이터 수집 완료 ===");
        log.info("전체 수집: {}개 → 중복 제거 후: {}개", allItems.size(), uniqueItems.size());
        
        return uniqueItems;
    }
    
    /**
     * 여러 페이지에 걸쳐 데이터 수집
     */
    private List<TourApiResponse.TourItem> collectMultiplePages(String contentTypeId, String categoryName, int maxPages) {
        List<TourApiResponse.TourItem> items = new ArrayList<>();
        String seoulAreaCode = "1";
        
        for (int page = 1; page <= maxPages; page++) {
            Optional<TourApiResponse> response = tourApiClient.getAreaBasedList(
                    seoulAreaCode, contentTypeId, page, 100);
            
            List<TourApiResponse.TourItem> pageItems = extractTourItems(response);
            items.addAll(pageItems);
            
            log.info("{} {}페이지: {}개 수집, 누적 {}개", categoryName, page, pageItems.size(), items.size());
            
            // API 호출 간격 (Rate Limiting 방지)
            try {
                Thread.sleep(100); // 100ms 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            
            // 빈 페이지면 더 이상 수집할 데이터 없음
            if (pageItems.isEmpty()) {
                log.info("{} 데이터 수집 완료 ({}페이지에서 종료)", categoryName, page);
                break;
            }
        }
        
        log.info("=== {} 카테고리 수집 완료: 총 {}개 ===", categoryName, items.size());
        return items;
    }
    
    /**
     * contentId 기준으로 중복 제거
     */
    private List<TourApiResponse.TourItem> removeDuplicates(List<TourApiResponse.TourItem> items) {
        return items.stream()
                .filter(item -> item.getContentId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        TourApiResponse.TourItem::getContentId,
                        item -> item,
                        (existing, replacement) -> existing // 중복 시 첫 번째 유지
                ))
                .values()
                .stream()
                .collect(java.util.stream.Collectors.toList());
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
