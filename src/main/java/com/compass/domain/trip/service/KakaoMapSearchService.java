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
        
        // API 키가 더미인 경우 테스트용 더미 응답 반환
        if (properties.getRestApiKey().equals("dummy-kakao-map-key")) {
            log.info("Kakao Map API 키가 더미 값입니다. 테스트용 더미 응답을 반환합니다.");
            return createMinimalDummyResponse(keyword);
        }
        
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

    /**
     * 테스트용 더미 응답 생성
     * @param keyword 검색 키워드
     * @param searchType 검색 타입
     * @return KakaoMapApiResponse
     */
    private Optional<KakaoMapApiResponse> createDummyResponse(String keyword, String searchType) {
        log.info("테스트용 더미 응답 생성: keyword={}, searchType={}", keyword, searchType);
        
        try {
            KakaoMapApiResponse response = new KakaoMapApiResponse();
            
            // Meta 정보 생성
            KakaoMapApiResponse.Meta meta = new KakaoMapApiResponse.Meta();
            meta.setTotalCount(1);
            meta.setPageableCount(1);
            meta.setEnd(true);
            response.setMeta(meta);
            
            // Documents 생성
            java.util.List<KakaoMapApiResponse.Document> documents = new java.util.ArrayList<>();
            
            // 더미 데이터 1 (간단하게)
            KakaoMapApiResponse.Document doc1 = new KakaoMapApiResponse.Document();
            doc1.setId("1");
            doc1.setPlaceName(keyword + " 테스트 장소");
            doc1.setCategoryName("관광명소");
            doc1.setCategoryGroupCode("AT4");
            doc1.setPhone("02-1234-5678");
            doc1.setAddressName("서울특별시 강남구 테헤란로 123");
            doc1.setRoadAddressName("서울특별시 강남구 테헤란로 123");
            doc1.setX("127.027619");
            doc1.setY("37.497952");
            doc1.setPlaceUrl("http://place.map.kakao.com/123456");
            doc1.setDistance("100");
            documents.add(doc1);
            
            response.setDocuments(documents);
            
            log.info("더미 응답 생성 완료: {}개 결과", documents.size());
            return Optional.of(response);
        } catch (Exception e) {
            log.error("더미 응답 생성 실패: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 간단한 테스트용 더미 응답 생성
     * @param keyword 검색 키워드
     * @return KakaoMapApiResponse
     */
    private Optional<KakaoMapApiResponse> createSimpleDummyResponse(String keyword) {
        log.info("간단한 더미 응답 생성: keyword={}", keyword);
        
        try {
            KakaoMapApiResponse response = new KakaoMapApiResponse();
            
            // Meta 정보 생성
            KakaoMapApiResponse.Meta meta = new KakaoMapApiResponse.Meta();
            meta.setTotalCount(1);
            meta.setPageableCount(1);
            meta.setEnd(true);
            response.setMeta(meta);
            
            // Documents 생성
            java.util.List<KakaoMapApiResponse.Document> documents = new java.util.ArrayList<>();
            
            // 더미 데이터 (최소한의 필드만 설정)
            KakaoMapApiResponse.Document doc = new KakaoMapApiResponse.Document();
            doc.setId("1");
            doc.setPlaceName(keyword + " 테스트 장소");
            doc.setCategoryName("관광명소");
            doc.setCategoryGroupCode("AT4");
            doc.setAddressName("서울특별시 강남구 테헤란로 123");
            doc.setX("127.027619");
            doc.setY("37.497952");
            documents.add(doc);
            
            response.setDocuments(documents);
            
            log.info("간단한 더미 응답 생성 완료: {}개 결과", documents.size());
            return Optional.of(response);
        } catch (Exception e) {
            log.error("간단한 더미 응답 생성 실패: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * 최소한의 테스트용 더미 응답 생성
     * @param keyword 검색 키워드
     * @return KakaoMapApiResponse
     */
    private Optional<KakaoMapApiResponse> createMinimalDummyResponse(String keyword) {
        log.info("최소한의 더미 응답 생성: keyword={}", keyword);
        
        try {
            KakaoMapApiResponse response = new KakaoMapApiResponse();
            
            // Meta 정보 생성
            KakaoMapApiResponse.Meta meta = new KakaoMapApiResponse.Meta();
            meta.setTotalCount(1);
            meta.setPageableCount(1);
            meta.setEnd(true);
            response.setMeta(meta);
            
            // Documents 생성 (빈 리스트)
            java.util.List<KakaoMapApiResponse.Document> documents = new java.util.ArrayList<>();
            response.setDocuments(documents);
            
            log.info("최소한의 더미 응답 생성 완료: {}개 결과", documents.size());
            return Optional.of(response);
        } catch (Exception e) {
            log.error("최소한의 더미 응답 생성 실패: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
