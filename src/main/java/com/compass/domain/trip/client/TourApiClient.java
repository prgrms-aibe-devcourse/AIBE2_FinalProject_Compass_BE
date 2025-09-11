package com.compass.domain.trip.client;

import com.compass.domain.trip.config.TourApiProperties;
import com.compass.domain.trip.dto.TourApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * 한국관광공사 Tour API 클라이언트
 * Seoul JSON 데이터를 보완하고 실시간 관광 정보를 제공
 */
@Component
public class TourApiClient {
    
    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);
    
    private final TourApiProperties properties;
    private final RestTemplate restTemplate;
    
    public TourApiClient(TourApiProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 지역 기반 관광정보 조회 (Seoul JSON 데이터 보완용)
     * @param areaCode 지역코드 (1=서울, 6=부산, 39=제주)
     * @param contentTypeId 컨텐츠 타입 (12=관광지, 39=음식점 등)
     * @param pageNo 페이지 번호
     * @param numOfRows 한 페이지 결과 수
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> getAreaBasedList(String areaCode, String contentTypeId, 
                                                     int pageNo, int numOfRows) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path("/areaBasedList2")
                    .queryParam("serviceKey", properties.getServiceKey())
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "Compass")
                    .queryParam("_type", properties.getResponseType())
                    .queryParam("areaCode", areaCode)
                    .queryParam("contentTypeId", contentTypeId)
                    .queryParam("arrange", properties.getArrange())
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .build()
                    .toUri();
            
            log.debug("Tour API 호출: {}", uri);
            
            TourApiResponse response = restTemplate.getForObject(uri, TourApiResponse.class);
            
            if (response != null && isSuccessResponse(response)) {
                log.info("Tour API 성공: 지역={}, 타입={}, 결과={}개", 
                        areaCode, contentTypeId, 
                        response.getResponse().getBody().getTotalCount());
                return Optional.of(response);
            } else {
                log.warn("Tour API 응답 오류: {}", 
                        response != null ? response.getResponse().getHeader().getResultMsg() : "null response");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Tour API 호출 실패: areaCode={}, contentTypeId={}", areaCode, contentTypeId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 관광지 상세 정보 조회 (Seoul JSON에 없는 상세 정보 보완)
     * @param contentId 컨텐츠 ID
     * @param contentTypeId 컨텐츠 타입 ID
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> getDetailCommon(String contentId, String contentTypeId) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path("/detailCommon1")
                    .queryParam("serviceKey", properties.getServiceKey())
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "Compass")
                    .queryParam("_type", properties.getResponseType())
                    .queryParam("contentId", contentId)
                    .queryParam("contentTypeId", contentTypeId)
                    .queryParam("defaultYN", "Y") // 기본정보 조회
                    .queryParam("firstImageYN", "Y") // 대표이미지 조회
                    .queryParam("areacodeYN", "Y") // 지역코드 조회
                    .queryParam("addrinfoYN", "Y") // 주소정보 조회
                    .queryParam("mapinfoYN", "Y") // 지도정보 조회
                    .queryParam("overviewYN", "Y") // 개요정보 조회
                    .build()
                    .toUri();
            
            log.debug("Tour API 상세정보 호출: contentId={}", contentId);
            
            TourApiResponse response = restTemplate.getForObject(uri, TourApiResponse.class);
            
            if (response != null && isSuccessResponse(response)) {
                log.debug("Tour API 상세정보 성공: contentId={}", contentId);
                return Optional.of(response);
            } else {
                log.warn("Tour API 상세정보 오류: contentId={}, msg={}", 
                        contentId, 
                        response != null ? response.getResponse().getHeader().getResultMsg() : "null response");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Tour API 상세정보 호출 실패: contentId={}", contentId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 위치 기반 관광정보 조회 (Seoul JSON 근거리 검색 보완)
     * @param mapX 경도 (Seoul JSON: lng)
     * @param mapY 위도 (Seoul JSON: lat)
     * @param radius 반경 (미터 단위, 최대 20000)
     * @param contentTypeId 컨텐츠 타입
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> getLocationBasedList(String mapX, String mapY, 
                                                         int radius, String contentTypeId) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path("/locationBasedList1")
                    .queryParam("serviceKey", properties.getServiceKey())
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "Compass")
                    .queryParam("_type", properties.getResponseType())
                    .queryParam("mapX", mapX)
                    .queryParam("mapY", mapY)
                    .queryParam("radius", radius)
                    .queryParam("contentTypeId", contentTypeId)
                    .queryParam("arrange", "E") // E=거리순
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", properties.getNumOfRows())
                    .build()
                    .toUri();
            
            log.debug("Tour API 위치기반 호출: 좌표=({},{}), 반경={}m", mapX, mapY, radius);
            
            TourApiResponse response = restTemplate.getForObject(uri, TourApiResponse.class);
            
            if (response != null && isSuccessResponse(response)) {
                log.info("Tour API 위치기반 성공: 좌표=({},{}), 결과={}개", 
                        mapX, mapY, 
                        response.getResponse().getBody().getTotalCount());
                return Optional.of(response);
            } else {
                log.warn("Tour API 위치기반 오류: 좌표=({},{}), msg={}", 
                        mapX, mapY,
                        response != null ? response.getResponse().getHeader().getResultMsg() : "null response");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Tour API 위치기반 호출 실패: 좌표=({},{})", mapX, mapY, e);
            return Optional.empty();
        }
    }
    
    /**
     * 키워드 기반 검색 (Seoul JSON tags 보완)
     * @param keyword 검색 키워드
     * @param areaCode 지역코드 (선택)
     * @param contentTypeId 컨텐츠 타입 (선택)
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> searchKeyword(String keyword, String areaCode, String contentTypeId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path("/searchKeyword1")
                    .queryParam("serviceKey", properties.getServiceKey())
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", "Compass")
                    .queryParam("_type", properties.getResponseType())
                    .queryParam("keyword", keyword)
                    .queryParam("arrange", properties.getArrange())
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", properties.getNumOfRows());
            
            if (areaCode != null && !areaCode.trim().isEmpty()) {
                builder.queryParam("areaCode", areaCode);
            }
            
            if (contentTypeId != null && !contentTypeId.trim().isEmpty()) {
                builder.queryParam("contentTypeId", contentTypeId);
            }
            
            URI uri = builder.build().toUri();
            
            log.debug("Tour API 키워드 검색: keyword={}, areaCode={}, contentTypeId={}", 
                     keyword, areaCode, contentTypeId);
            
            TourApiResponse response = restTemplate.getForObject(uri, TourApiResponse.class);
            
            if (response != null && isSuccessResponse(response)) {
                log.info("Tour API 키워드 검색 성공: keyword={}, 결과={}개", 
                        keyword, 
                        response.getResponse().getBody().getTotalCount());
                return Optional.of(response);
            } else {
                log.warn("Tour API 키워드 검색 오류: keyword={}, msg={}", 
                        keyword,
                        response != null ? response.getResponse().getHeader().getResultMsg() : "null response");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Tour API 키워드 검색 실패: keyword={}", keyword, e);
            return Optional.empty();
        }
    }
    
    /**
     * API 응답 성공 여부 확인
     */
    private boolean isSuccessResponse(TourApiResponse response) {
        return response.getResponse() != null 
                && response.getResponse().getHeader() != null
                && "0000".equals(response.getResponse().getHeader().getResultCode());
    }
}
