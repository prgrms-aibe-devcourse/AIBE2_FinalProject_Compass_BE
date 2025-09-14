package com.compass.domain.trip.service;

import com.compass.domain.trip.client.TourApiClient;
import com.compass.domain.trip.dto.TourApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Tour API 검색 서비스
 * REQ-SEARCH-002: Tour API 검색 (실시간 API 호출)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TourApiSearchService {

    private final TourApiClient tourApiClient;

    /**
     * 키워드 기반 검색
     * @param keyword 검색 키워드
     * @param areaCode 지역코드 (선택)
     * @param contentTypeId 컨텐츠 타입 (선택)
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> searchByKeyword(String keyword, String areaCode, String contentTypeId) {
        log.info("Tour API 키워드 검색: keyword={}, areaCode={}, contentTypeId={}", keyword, areaCode, contentTypeId);
        
        Optional<TourApiResponse> response = tourApiClient.searchKeyword(keyword, areaCode, contentTypeId);
        
        if (response.isPresent()) {
            int totalCount = response.get().getResponse().getBody().getTotalCount();
            log.info("Tour API 키워드 검색 성공: {}개 결과", totalCount);
        } else {
            log.warn("Tour API 키워드 검색 실패: keyword={}", keyword);
        }
        
        return response;
    }

    /**
     * 지역 기반 관광지 검색
     * @param areaCode 지역코드 (1=서울, 6=부산, 39=제주)
     * @param contentTypeId 컨텐츠 타입 (12=관광지, 39=음식점 등)
     * @param pageNo 페이지 번호
     * @param numOfRows 한 페이지 결과 수
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> searchByArea(String areaCode, String contentTypeId, int pageNo, int numOfRows) {
        log.info("Tour API 지역 검색: areaCode={}, contentTypeId={}, pageNo={}, numOfRows={}", 
                areaCode, contentTypeId, pageNo, numOfRows);
        
        Optional<TourApiResponse> response = tourApiClient.getAreaBasedList(areaCode, contentTypeId, pageNo, numOfRows);
        
        if (response.isPresent()) {
            int totalCount = response.get().getResponse().getBody().getTotalCount();
            log.info("Tour API 지역 검색 성공: {}개 결과", totalCount);
        } else {
            log.warn("Tour API 지역 검색 실패: areaCode={}, contentTypeId={}", areaCode, contentTypeId);
        }
        
        return response;
    }

    /**
     * 위치 기반 근거리 검색
     * @param mapX 경도
     * @param mapY 위도
     * @param radius 반경 (미터 단위, 최대 20000)
     * @param contentTypeId 컨텐츠 타입
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> searchByLocation(String mapX, String mapY, int radius, String contentTypeId) {
        log.info("Tour API 위치 검색: mapX={}, mapY={}, radius={}m, contentTypeId={}", 
                mapX, mapY, radius, contentTypeId);
        
        Optional<TourApiResponse> response = tourApiClient.getLocationBasedList(mapX, mapY, radius, contentTypeId);
        
        if (response.isPresent()) {
            int totalCount = response.get().getResponse().getBody().getTotalCount();
            log.info("Tour API 위치 검색 성공: {}개 결과", totalCount);
        } else {
            log.warn("Tour API 위치 검색 실패: mapX={}, mapY={}", mapX, mapY);
        }
        
        return response;
    }

    /**
     * 관광지 상세 정보 조회
     * @param contentId 컨텐츠 ID
     * @param contentTypeId 컨텐츠 타입 ID
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> getDetailInfo(String contentId, String contentTypeId) {
        log.info("Tour API 상세 정보 조회: contentId={}, contentTypeId={}", contentId, contentTypeId);
        
        Optional<TourApiResponse> response = tourApiClient.getDetailCommon(contentId, contentTypeId);
        
        if (response.isPresent()) {
            log.info("Tour API 상세 정보 조회 성공: contentId={}", contentId);
        } else {
            log.warn("Tour API 상세 정보 조회 실패: contentId={}", contentId);
        }
        
        return response;
    }

    /**
     * 통합 검색 - 키워드 우선, 지역 필터 적용
     * @param keyword 검색 키워드
     * @param areaCode 지역코드 (선택)
     * @param contentTypeId 컨텐츠 타입 (선택)
     * @return TourApiResponse
     */
    public Optional<TourApiResponse> integratedSearch(String keyword, String areaCode, String contentTypeId) {
        log.info("Tour API 통합 검색: keyword={}, areaCode={}, contentTypeId={}", keyword, areaCode, contentTypeId);
        
        // 1순위: 키워드 검색
        Optional<TourApiResponse> response = searchByKeyword(keyword, areaCode, contentTypeId);
        
        // 키워드 검색 결과가 없거나 적으면 지역 기반 검색으로 폴백
        if (response.isEmpty() || response.get().getResponse().getBody().getTotalCount() < 5) {
            log.info("키워드 검색 결과 부족, 지역 기반 검색으로 폴백");
            
            if (areaCode != null && !areaCode.trim().isEmpty()) {
                response = searchByArea(areaCode, contentTypeId, 1, 20);
            }
        }
        
        if (response.isPresent()) {
            int totalCount = response.get().getResponse().getBody().getTotalCount();
            log.info("Tour API 통합 검색 완료: {}개 결과", totalCount);
        } else {
            log.warn("Tour API 통합 검색 실패: keyword={}", keyword);
        }
        
        return response;
    }

    /**
     * 검색 결과 통계
     * @param response TourApiResponse
     * @return 검색 통계 정보
     */
    public String getSearchStatistics(TourApiResponse response) {
        if (response == null || response.getResponse() == null || response.getResponse().getBody() == null) {
            return "검색 결과 없음";
        }
        
        int totalCount = response.getResponse().getBody().getTotalCount();
        int numOfRows = response.getResponse().getBody().getNumOfRows();
        int pageNo = response.getResponse().getBody().getPageNo();
        
        return String.format("총 %d개 결과 (페이지 %d/%d, 페이지당 %d개)", 
                totalCount, pageNo, (totalCount + numOfRows - 1) / numOfRows, numOfRows);
    }
}
