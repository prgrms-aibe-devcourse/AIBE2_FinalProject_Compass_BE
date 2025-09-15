package com.compass.domain.trip.client;

import com.compass.domain.trip.config.KakaoMapApiProperties;
import com.compass.domain.trip.dto.KakaoMapApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

/**
 * Kakao Map API 클라이언트
 * REQ-SEARCH-003: Kakao Map API 검색 (폴백 검색)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoMapApiClient {
    
    private final KakaoMapApiProperties properties;
    private final RestTemplate restTemplate;
    
    /**
     * 키워드로 장소 검색
     * @param keyword 검색 키워드
     * @param x 경도 (선택)
     * @param y 위도 (선택)
     * @param radius 반경 (미터, 최대 20000)
     * @param rect 사각형 영역 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 방식 (accuracy, distance)
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> searchKeyword(String keyword, String x, String y, 
                                                      int radius, String rect, int page, int size, String sort) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path(properties.getSearch().getKeyword())
                    .queryParam("query", keyword)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParam("sort", sort);
            
            if (x != null && y != null) {
                builder.queryParam("x", x).queryParam("y", y);
            }
            
            if (radius > 0) {
                builder.queryParam("radius", radius);
            }
            
            if (rect != null && !rect.trim().isEmpty()) {
                builder.queryParam("rect", rect);
            }
            
            URI uri = builder.build().toUri();
            
            log.debug("Kakao Map API 키워드 검색: keyword={}, x={}, y={}, radius={}", keyword, x, y, radius);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<KakaoMapApiResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoMapApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KakaoMapApiResponse body = response.getBody();
                log.info("Kakao Map API 키워드 검색 성공: {}개 결과", body.getMeta().getTotalCount());
                return Optional.of(body);
            } else {
                log.warn("Kakao Map API 키워드 검색 실패: status={}", response.getStatusCode());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Kakao Map API 키워드 검색 호출 실패: keyword={}", keyword, e);
            return Optional.empty();
        }
    }
    
    /**
     * 카테고리로 장소 검색
     * @param categoryGroupCode 카테고리 그룹 코드
     * @param x 경도
     * @param y 위도
     * @param radius 반경 (미터, 최대 20000)
     * @param rect 사각형 영역 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 방식
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> searchCategory(String categoryGroupCode, String x, String y,
                                                       int radius, String rect, int page, int size, String sort) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path(properties.getSearch().getCategory())
                    .queryParam("category_group_code", categoryGroupCode)
                    .queryParam("x", x)
                    .queryParam("y", y)
                    .queryParam("radius", radius)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .queryParam("sort", sort);
            
            if (rect != null && !rect.trim().isEmpty()) {
                builder.queryParam("rect", rect);
            }
            
            URI uri = builder.build().toUri();
            
            log.debug("Kakao Map API 카테고리 검색: category={}, x={}, y={}, radius={}", 
                    categoryGroupCode, x, y, radius);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<KakaoMapApiResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoMapApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KakaoMapApiResponse body = response.getBody();
                log.info("Kakao Map API 카테고리 검색 성공: {}개 결과", body.getMeta().getTotalCount());
                return Optional.of(body);
            } else {
                log.warn("Kakao Map API 카테고리 검색 실패: status={}", response.getStatusCode());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Kakao Map API 카테고리 검색 호출 실패: category={}", categoryGroupCode, e);
            return Optional.empty();
        }
    }
    
    /**
     * 주소로 장소 검색
     * @param address 검색할 주소
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> searchAddress(String address, int page, int size) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path(properties.getSearch().getAddress())
                    .queryParam("query", address)
                    .queryParam("page", page)
                    .queryParam("size", size)
                    .build()
                    .toUri();
            
            log.debug("Kakao Map API 주소 검색: address={}", address);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<KakaoMapApiResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoMapApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KakaoMapApiResponse body = response.getBody();
                log.info("Kakao Map API 주소 검색 성공: {}개 결과", body.getMeta().getTotalCount());
                return Optional.of(body);
            } else {
                log.warn("Kakao Map API 주소 검색 실패: status={}", response.getStatusCode());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Kakao Map API 주소 검색 호출 실패: address={}", address, e);
            return Optional.empty();
        }
    }
    
    /**
     * 좌표를 주소로 변환
     * @param x 경도
     * @param y 위도
     * @param inputCoord 입력 좌표계 (WGS84, WCONGNAMUL, CONGNAMUL, WTM, TM)
     * @param outputCoord 출력 좌표계
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> coordToAddress(String x, String y, String inputCoord, String outputCoord) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path(properties.getSearch().getCoord2address())
                    .queryParam("x", x)
                    .queryParam("y", y)
                    .queryParam("input_coord", inputCoord)
                    .queryParam("output_coord", outputCoord)
                    .build()
                    .toUri();
            
            log.debug("Kakao Map API 좌표->주소 변환: x={}, y={}", x, y);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<KakaoMapApiResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoMapApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KakaoMapApiResponse body = response.getBody();
                log.info("Kakao Map API 좌표->주소 변환 성공: {}개 결과", body.getDocuments().size());
                return Optional.of(body);
            } else {
                log.warn("Kakao Map API 좌표->주소 변환 실패: status={}", response.getStatusCode());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Kakao Map API 좌표->주소 변환 호출 실패: x={}, y={}", x, y, e);
            return Optional.empty();
        }
    }
    
    /**
     * 주소를 좌표로 변환
     * @param address 검색할 주소
     * @return KakaoMapApiResponse
     */
    public Optional<KakaoMapApiResponse> addressToCoord(String address) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                    .path(properties.getSearch().getAddress2coord())
                    .queryParam("query", address)
                    .build()
                    .toUri();
            
            log.debug("Kakao Map API 주소->좌표 변환: address={}", address);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<KakaoMapApiResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoMapApiResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                KakaoMapApiResponse body = response.getBody();
                log.info("Kakao Map API 주소->좌표 변환 성공: {}개 결과", body.getDocuments().size());
                return Optional.of(body);
            } else {
                log.warn("Kakao Map API 주소->좌표 변환 실패: status={}", response.getStatusCode());
                return Optional.empty();
            }
            
        } catch (Exception e) {
            log.error("Kakao Map API 주소->좌표 변환 호출 실패: address={}", address, e);
            return Optional.empty();
        }
    }
    
    /**
     * HTTP 헤더 생성 (Authorization 포함)
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + properties.getRestApiKey());
        headers.set("Content-Type", "application/json;charset=UTF-8");
        return headers;
    }
}
