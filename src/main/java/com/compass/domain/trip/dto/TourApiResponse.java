package com.compass.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 한국관광공사 Tour API 응답 DTO
 * Seoul JSON 데이터와 매핑하여 TourPlace 엔티티로 변환
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiResponse {
    
    @JsonProperty("response")
    private ResponseData response;
    
    public ResponseData getResponse() {
        return response;
    }
    
    public void setResponse(ResponseData response) {
        this.response = response;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseData {
        
        @JsonProperty("header")
        private Header header;
        
        @JsonProperty("body")
        private Body body;
        
        public Header getHeader() {
            return header;
        }
        
        public void setHeader(Header header) {
            this.header = header;
        }
        
        public Body getBody() {
            return body;
        }
        
        public void setBody(Body body) {
            this.body = body;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        
        @JsonProperty("resultCode")
        private String resultCode;
        
        @JsonProperty("resultMsg")
        private String resultMsg;
        
        public String getResultCode() {
            return resultCode;
        }
        
        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }
        
        public String getResultMsg() {
            return resultMsg;
        }
        
        public void setResultMsg(String resultMsg) {
            this.resultMsg = resultMsg;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        
        @JsonProperty("items")
        private Items items;
        
        @JsonProperty("numOfRows")
        private Integer numOfRows;
        
        @JsonProperty("pageNo")
        private Integer pageNo;
        
        @JsonProperty("totalCount")
        private Integer totalCount;
        
        public Items getItems() {
            return items;
        }
        
        public void setItems(Items items) {
            this.items = items;
        }
        
        public Integer getNumOfRows() {
            return numOfRows;
        }
        
        public void setNumOfRows(Integer numOfRows) {
            this.numOfRows = numOfRows;
        }
        
        public Integer getPageNo() {
            return pageNo;
        }
        
        public void setPageNo(Integer pageNo) {
            this.pageNo = pageNo;
        }
        
        public Integer getTotalCount() {
            return totalCount;
        }
        
        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        
        @JsonProperty("item")
        private List<TourItem> item;
        
        public List<TourItem> getItem() {
            return item;
        }
        
        public void setItem(List<TourItem> item) {
            this.item = item;
        }
    }
    
    /**
     * Tour API Item DTO - Seoul JSON과 매핑 가능한 필드들
     * Seoul JSON: {id, name, category, district, area, tags[], lat, lng}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TourItem {
        
        @JsonProperty("contentid")
        private String contentId; // Seoul JSON: "id"
        
        @JsonProperty("contenttypeid")
        private String contentTypeId; // 12=관광지, 39=음식점 등
        
        @JsonProperty("title")
        private String title; // Seoul JSON: "name"
        
        @JsonProperty("addr1")
        private String addr1; // 주소 - district 파싱 가능
        
        @JsonProperty("addr2")
        private String addr2; // 상세주소 - area 정보
        
        @JsonProperty("areacode")
        private String areaCode; // 지역코드 (1=서울)
        
        @JsonProperty("sigungucode")
        private String sigunguCode; // 시군구코드
        
        @JsonProperty("mapx")
        private String mapX; // Seoul JSON: "lng" (경도)
        
        @JsonProperty("mapy")
        private String mapY; // Seoul JSON: "lat" (위도)
        
        @JsonProperty("mlevel")
        private String mapLevel; // 지도레벨
        
        @JsonProperty("tel")
        private String tel; // 전화번호
        
        @JsonProperty("firstimage")
        private String firstImage; // 대표이미지 (원본)
        
        @JsonProperty("firstimage2")
        private String firstImage2; // 대표이미지 (썸네일)
        
        @JsonProperty("readcount")
        private Integer readCount; // 조회수
        
        @JsonProperty("modifiedtime")
        private String modifiedTime; // 수정일
        
        @JsonProperty("createdtime")
        private String createdTime; // 등록일
        
        // Getters and Setters
        public String getContentId() {
            return contentId;
        }
        
        public void setContentId(String contentId) {
            this.contentId = contentId;
        }
        
        public String getContentTypeId() {
            return contentTypeId;
        }
        
        public void setContentTypeId(String contentTypeId) {
            this.contentTypeId = contentTypeId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getAddr1() {
            return addr1;
        }
        
        public void setAddr1(String addr1) {
            this.addr1 = addr1;
        }
        
        public String getAddr2() {
            return addr2;
        }
        
        public void setAddr2(String addr2) {
            this.addr2 = addr2;
        }
        
        public String getAreaCode() {
            return areaCode;
        }
        
        public void setAreaCode(String areaCode) {
            this.areaCode = areaCode;
        }
        
        public String getSigunguCode() {
            return sigunguCode;
        }
        
        public void setSigunguCode(String sigunguCode) {
            this.sigunguCode = sigunguCode;
        }
        
        public String getMapX() {
            return mapX;
        }
        
        public void setMapX(String mapX) {
            this.mapX = mapX;
        }
        
        public String getMapY() {
            return mapY;
        }
        
        public void setMapY(String mapY) {
            this.mapY = mapY;
        }
        
        public String getMapLevel() {
            return mapLevel;
        }
        
        public void setMapLevel(String mapLevel) {
            this.mapLevel = mapLevel;
        }
        
        public String getTel() {
            return tel;
        }
        
        public void setTel(String tel) {
            this.tel = tel;
        }
        
        public String getFirstImage() {
            return firstImage;
        }
        
        public void setFirstImage(String firstImage) {
            this.firstImage = firstImage;
        }
        
        public String getFirstImage2() {
            return firstImage2;
        }
        
        public void setFirstImage2(String firstImage2) {
            this.firstImage2 = firstImage2;
        }
        
        public Integer getReadCount() {
            return readCount;
        }
        
        public void setReadCount(Integer readCount) {
            this.readCount = readCount;
        }
        
        public String getModifiedTime() {
            return modifiedTime;
        }
        
        public void setModifiedTime(String modifiedTime) {
            this.modifiedTime = modifiedTime;
        }
        
        public String getCreatedTime() {
            return createdTime;
        }
        
        public void setCreatedTime(String createdTime) {
            this.createdTime = createdTime;
        }
    }
}
