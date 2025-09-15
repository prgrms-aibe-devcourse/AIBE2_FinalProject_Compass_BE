package com.compass.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Kakao Map API 응답 DTO
 * REQ-SEARCH-003: Kakao Map API 검색
 */
@Data
public class KakaoMapApiResponse {
    
    private Meta meta;
    private List<Document> documents;
    
    @Data
    public static class Meta {
        @JsonProperty("total_count")
        private int totalCount;
        
        @JsonProperty("pageable_count")
        private int pageableCount;
        
        @JsonProperty("is_end")
        private boolean isEnd;
    }
    
    @Data
    public static class Document {
        private String id;
        private String placeName;
        private String categoryName;
        private String categoryGroupCode;
        private String categoryGroupName;
        private String phone;
        private String addressName;
        private String roadAddressName;
        private String x; // 경도
        private String y; // 위도
        private String placeUrl;
        private String distance;
        
        // 키워드 검색 응답
        @JsonProperty("place_name")
        private String placeNameKeyword;
        
        @JsonProperty("category_name")
        private String categoryNameKeyword;
        
        @JsonProperty("category_group_code")
        private String categoryGroupCodeKeyword;
        
        @JsonProperty("category_group_name")
        private String categoryGroupNameKeyword;
        
        @JsonProperty("phone")
        private String phoneKeyword;
        
        @JsonProperty("address_name")
        private String addressNameKeyword;
        
        @JsonProperty("road_address_name")
        private String roadAddressNameKeyword;
        
        @JsonProperty("place_url")
        private String placeUrlKeyword;
        
        // 주소 검색 응답
        @JsonProperty("address")
        private Address address;
        
        @JsonProperty("road_address")
        private RoadAddress roadAddress;
        
        @Data
        public static class Address {
            @JsonProperty("address_name")
            private String addressName;
            
            @JsonProperty("region_1depth_name")
            private String region1depthName;
            
            @JsonProperty("region_2depth_name")
            private String region2depthName;
            
            @JsonProperty("region_3depth_name")
            private String region3depthName;
            
            @JsonProperty("region_3depth_h_name")
            private String region3depthHName;
            
            @JsonProperty("h_code")
            private String hCode;
            
            @JsonProperty("b_code")
            private String bCode;
            
            @JsonProperty("mountain_yn")
            private String mountainYn;
            
            @JsonProperty("main_address_no")
            private String mainAddressNo;
            
            @JsonProperty("sub_address_no")
            private String subAddressNo;
            
            private String x;
            private String y;
        }
        
        @Data
        public static class RoadAddress {
            @JsonProperty("address_name")
            private String addressName;
            
            @JsonProperty("region_1depth_name")
            private String region1depthName;
            
            @JsonProperty("region_2depth_name")
            private String region2depthName;
            
            @JsonProperty("region_3depth_name")
            private String region3depthName;
            
            @JsonProperty("region_3depth_h_name")
            private String region3depthHName;
            
            @JsonProperty("road_name")
            private String roadName;
            
            @JsonProperty("underground_yn")
            private String undergroundYn;
            
            @JsonProperty("main_building_no")
            private String mainBuildingNo;
            
            @JsonProperty("sub_building_no")
            private String subBuildingNo;
            
            @JsonProperty("building_name")
            private String buildingName;
            
            @JsonProperty("zone_no")
            private String zoneNo;
            
            private String x;
            private String y;
        }
    }
}
