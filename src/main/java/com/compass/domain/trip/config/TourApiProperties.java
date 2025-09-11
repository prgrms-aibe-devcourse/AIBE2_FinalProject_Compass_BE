package com.compass.domain.trip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tour.api")
public class TourApiProperties {
    
    private String baseUrl = "http://apis.data.go.kr/B551011/KorService1";
    private String serviceKey;
    private String responseType = "json";
    private int numOfRows = 100;
    private int pageNo = 1;
    private String arrange = "A"; // A=제목순, B=조회순, C=수정일순, D=생성일순, E=거리순
    
    // Seoul JSON과 매핑되는 지역 코드 (서울: 1)
    private String defaultAreaCode = "1"; // 서울
    
    // Seoul JSON 카테고리와 매핑되는 컨텐츠 타입 ID
    public static final String TOURIST_SPOT = "12"; // 관광지
    public static final String CULTURAL_FACILITY = "14"; // 문화시설
    public static final String FESTIVAL = "15"; // 축제공연행사
    public static final String TRAVEL_COURSE = "25"; // 여행코스
    public static final String LEISURE_SPORTS = "28"; // 레포츠
    public static final String ACCOMMODATION = "32"; // 숙박
    public static final String SHOPPING = "38"; // 쇼핑
    public static final String RESTAURANT = "39"; // 음식점
    
    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getServiceKey() {
        return serviceKey;
    }
    
    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }
    
    public String getResponseType() {
        return responseType;
    }
    
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
    
    public int getNumOfRows() {
        return numOfRows;
    }
    
    public void setNumOfRows(int numOfRows) {
        this.numOfRows = numOfRows;
    }
    
    public int getPageNo() {
        return pageNo;
    }
    
    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }
    
    public String getArrange() {
        return arrange;
    }
    
    public void setArrange(String arrange) {
        this.arrange = arrange;
    }
    
    public String getDefaultAreaCode() {
        return defaultAreaCode;
    }
    
    public void setDefaultAreaCode(String defaultAreaCode) {
        this.defaultAreaCode = defaultAreaCode;
    }
}
