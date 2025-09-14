package com.compass.domain.trip.service;

import com.compass.domain.trip.client.KakaoMapApiClient;
import com.compass.domain.trip.config.KakaoMapApiProperties;
import com.compass.domain.trip.dto.KakaoMapApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Kakao Map API 검색 서비스
 * REQ-SEARCH-003: Kakao Map API 검색 (폴백 검색)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoMapSearchService {

    private final KakaoMapApiClient kakaoMapApiClient;
    private final KakaoMapApiProperties properties;

    /**
     * 키워드로 장소 검색
     * @param keyword 검색 키워드
     * @param x 경도 (선택)
     * @param y 위도 (선택)
     * @param radius 반경 (미터, 최대 20000)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 방식 (accuracy, distance)
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> searchByKeyword(String keyword, String x, String y, 
                                                       int radius, int page, int size, String sort) {
        log.info("Kakao Map API 키워드 검색: keyword={}, x={}, y={}, radius={}m, page={}, size={}, sort={}", 
                keyword, x, y, radius, page, size, sort);
        
        Optional<KakaoMapApiResponse> response = kakaoMapApiClient.searchKeyword(
                keyword, x, y, radius, null, page, size, sort);
        
        if (response.isPresent()) {
            int totalCount = response.get().getMeta().getTotalCount();
            log.info("Kakao Map API 키워드 검색 성공: {}개 결과", totalCount);
        } else {
            log.warn("Kakao Map API 키워드 검색 실패: keyword={}", keyword);
        }
        
        return response;
    }

    /**
     * 카테고리로 장소 검색
     * @param categoryGroupCode 카테고리 그룹 코드
     * @param x 경도
     * @param y 위도
     * @param radius 반경 (미터, 최대 20000)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 방식
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> searchByCategory(String categoryGroupCode, String x, String y,
                                                        int radius, int page, int size, String sort) {
        log.info("Kakao Map API 카테고리 검색: category={}, x={}, y={}, radius={}m, page={}, size={}, sort={}", 
                categoryGroupCode, x, y, radius, page, size, sort);
        
        Optional<KakaoMapApiResponse> response = kakaoMapApiClient.searchCategory(
                categoryGroupCode, x, y, radius, null, page, size, sort);
        
        if (response.isPresent()) {
            int totalCount = response.get().getMeta().getTotalCount();
            log.info("Kakao Map API 카테고리 검색 성공: {}개 결과", totalCount);
        } else {
            log.warn("Kakao Map API 카테고리 검색 실패: category={}", categoryGroupCode);
        }
        
        return response;
    }

    /**
     * 주소로 장소 검색
     * @param address 검색할 주소
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> searchByAddress(String address, int page, int size) {
        log.info("Kakao Map API 주소 검색: address={}, page={}, size={}", address, page, size);
        
        Optional<KakaoMapApiResponse> response = kakaoMapApiClient.searchAddress(address, page, size);
        
        if (response.isPresent()) {
            int totalCount = response.get().getMeta().getTotalCount();
            log.info("Kakao Map API 주소 검색 성공: {}개 결과", totalCount);
        } else {
            log.warn("Kakao Map API 주소 검색 실패: address={}", address);
        }
        
        return response;
    }

    /**
     * 좌표를 주소로 변환
     * @param x 경도
     * @param y 위도
     * @param inputCoord 입력 좌표계
     * @param outputCoord 출력 좌표계
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> coordToAddress(String x, String y, String inputCoord, String outputCoord) {
        log.info("Kakao Map API 좌표->주소 변환: x={}, y={}, inputCoord={}, outputCoord={}", 
                x, y, inputCoord, outputCoord);
        
        Optional<KakaoMapApiResponse> response = kakaoMapApiClient.coordToAddress(x, y, inputCoord, outputCoord);
        
        if (response.isPresent()) {
            int resultCount = response.get().getDocuments().size();
            log.info("Kakao Map API 좌표->주소 변환 성공: {}개 결과", resultCount);
        } else {
            log.warn("Kakao Map API 좌표->주소 변환 실패: x={}, y={}", x, y);
        }
        
        return response;
    }

    /**
     * 주소를 좌표로 변환
     * @param address 검색할 주소
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> addressToCoord(String address) {
        log.info("Kakao Map API 주소->좌표 변환: address={}", address);
        
        Optional<KakaoMapApiResponse> response = kakaoMapApiClient.addressToCoord(address);
        
        if (response.isPresent()) {
            int resultCount = response.get().getDocuments().size();
            log.info("Kakao Map API 주소->좌표 변환 성공: {}개 결과", resultCount);
        } else {
            log.warn("Kakao Map API 주소->좌표 변환 실패: address={}", address);
        }
        
        return response;
    }

    /**
     * 통합 검색 - 키워드 우선, 카테고리 폴백
     * @param keyword 검색 키워드
     * @param x 경도 (선택)
     * @param y 위도 (선택)
     * @param radius 반경 (미터)
     * @param categoryGroupCode 카테고리 그룹 코드 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 방식
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> integratedSearch(String keyword, String x, String y, int radius,
                                                        String categoryGroupCode, int page, int size, String sort) {
        log.info("Kakao Map API 통합 검색: keyword={}, x={}, y={}, radius={}m, category={}, page={}, size={}, sort={}", 
                keyword, x, y, radius, categoryGroupCode, page, size, sort);
        
        // 1순위: 키워드 검색
        Optional<KakaoMapApiResponse> response = searchByKeyword(keyword, x, y, radius, page, size, sort);
        
        // 키워드 검색 결과가 없거나 적으면 카테고리 검색으로 폴백
        if (response.isEmpty() || response.get().getMeta().getTotalCount() < 5) {
            log.info("키워드 검색 결과 부족, 카테고리 검색으로 폴백");
            
            if (categoryGroupCode != null && !categoryGroupCode.trim().isEmpty() && 
                x != null && y != null) {
                response = searchByCategory(categoryGroupCode, x, y, radius, page, size, sort);
            }
        }
        
        if (response.isPresent()) {
            int totalCount = response.get().getMeta().getTotalCount();
            log.info("Kakao Map API 통합 검색 완료: {}개 결과", totalCount);
        } else {
            log.warn("Kakao Map API 통합 검색 실패: keyword={}", keyword);
        }
        
        return response;
    }

    /**
     * 검색 결과 통계
     * @param response KakaoMapApiResponse
     * @return 검색 통계 정보
     */
    public String getSearchStatistics(KakaoMapApiResponse response) {
        if (response == null || response.getMeta() == null) {
            return "검색 결과 없음";
        }
        
        KakaoMapApiResponse.Meta meta = response.getMeta();
        int totalCount = meta.getTotalCount();
        int pageableCount = meta.getPageableCount();
        boolean isEnd = meta.isEnd();
        
        return String.format("총 %d개 결과 (검색 가능 %d개, 마지막 페이지: %s)", 
                totalCount, pageableCount, isEnd ? "예" : "아니오");
    }

    /**
     * 기본값으로 키워드 검색
     * @param keyword 검색 키워드
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> searchByKeywordDefault(String keyword) {
        return searchByKeyword(keyword, null, null, 0, 
                properties.getDefaults().getPage(), 
                properties.getDefaults().getSize(), 
                properties.getDefaults().getSort());
    }

    /**
     * 기본값으로 카테고리 검색
     * @param categoryGroupCode 카테고리 그룹 코드
     * @param x 경도
     * @param y 위도
     * @param radius 반경
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> searchByCategoryDefault(String categoryGroupCode, String x, String y, int radius) {
        return searchByCategory(categoryGroupCode, x, y, radius,
                properties.getDefaults().getPage(),
                properties.getDefaults().getSize(),
                properties.getDefaults().getSort());
    }
}
