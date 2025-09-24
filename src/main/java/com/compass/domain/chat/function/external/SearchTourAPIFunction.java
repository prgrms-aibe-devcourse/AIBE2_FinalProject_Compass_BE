package com.compass.domain.chat.function.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// 한국관광공사 API 검색 Function
@Slf4j
@Component
public class SearchTourAPIFunction implements Function<SearchTourAPIFunction.Location, List<SearchTourAPIFunction.TourPlace>> {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tour.api.key}")
    private String tourApiKey;

    @Value("${TOUR_API_URL:http://apis.data.go.kr/B551011/KorService2}")
    private String tourApiUrl;

    @Value("${TOUR_API_TIMEOUT:30}")
    private int timeoutSeconds;

    @Value("${TOUR_API_MAX_RETRIES:3}")
    private int maxRetries;

    @Override
    public List<TourPlace> apply(Location location) {
        log.info("관광공사 API 검색 시작: {}", location.name());

        try {
            // API 키 검증
            if (tourApiKey == null || tourApiKey.isEmpty()) {
                throw new RuntimeException("TOUR_API_KEY 환경변수가 설정되지 않았습니다. .env 파일을 확인해주세요.");
            }

            // 한국관광공사 API 호출 (재시도 로직 포함)
            String response = callTourAPIWithRetry(location);
            
            // 응답을 TourPlace 리스트로 변환
            return parseTourAPIResults(response);

        } catch (Exception e) {
            log.error("관광공사 API 검색 실패: {}", location.name(), e);
            return List.of();
        }
    }

    // 비동기 검색
    public CompletableFuture<List<TourPlace>> searchAsync(Location location) {
        return CompletableFuture.supplyAsync(() -> apply(location));
    }

    // 재시도 로직이 포함된 Tour API 호출
    private String callTourAPIWithRetry(Location location) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("관광공사 API 호출 시도 {}: {}", attempt, location.name());
                return performTourAPICall(location);
            } catch (Exception e) {
                lastException = e;
                log.warn("관광공사 API 호출 실패 (시도 {}/{})", attempt, maxRetries, e);
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("관광공사 API 호출 중 스레드 중단", ie);
                    }
                }
            }
        }
        throw new RuntimeException("관광공사 API 호출 최종 실패", lastException);
    }

    // 실제 Tour API 호출 (검증된 엔드포인트 사용)
    private String performTourAPICall(Location location) {
        var url = String.format(
            "%s/areaBasedList2?serviceKey=%s&MobileApp=CompassApp&MobileOS=ETC&arrange=A&areaCode=%s&_type=json&numOfRows=20&pageNo=1",
            tourApiUrl,
            tourApiKey,
            getAreaCode(location.name())
        );

        log.info("Tour API 호출: {} 지역 (코드: {})", location.name(), getAreaCode(location.name()));

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String body = response.getBody();
            
            // 성공 응답 확인
            if (body.contains("\"resultCode\":\"0000\"")) {
                log.info("Tour API 호출 성공");
                return body;
            } else {
                log.warn("Tour API 응답 에러: {}", body.substring(0, Math.min(200, body.length())));
                return body; // 에러 응답도 파싱에서 처리
            }
        } else {
            throw new RuntimeException("Tour API HTTP 응답 실패: " + response.getStatusCode());
        }
    }

    // 지역 코드 매핑
    private String getAreaCode(String locationName) {
        return switch (locationName) {
            case "서울" -> "1";
            case "인천" -> "2";
            case "대전" -> "3";
            case "대구" -> "4";
            case "광주" -> "5";
            case "부산" -> "6";
            case "울산" -> "7";
            case "세종" -> "8";
            case "경기" -> "31";
            case "강원" -> "32";
            case "충북" -> "33";
            case "충남" -> "34";
            case "경북" -> "35";
            case "경남" -> "36";
            case "전북" -> "37";
            case "전남" -> "38";
            case "제주" -> "39";
            default -> "1"; // 기본값: 서울
        };
    }

    // Tour API 응답 파싱
    private List<TourPlace> parseTourAPIResults(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                return List.of();
            }

            // XML 에러 응답 체크 및 분석
            if (response.startsWith("<OpenAPI_ServiceResponse>")) {
                if (response.contains("returnReasonCode>12<")) {
                    log.error("Tour API 키 권한 문제 (코드 12): API 서비스 신청이 필요하거나 키가 비활성화됨");
                } else if (response.contains("SERVICE ERROR")) {
                    log.error("Tour API 서비스 에러: {}", response);
                } else {
                    log.warn("Tour API XML 에러 응답: {}", response);
                }
                return List.of();
            }

            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode responseNode = rootNode.path("response");
            JsonNode bodyNode = responseNode.path("body");
            JsonNode itemsNode = bodyNode.path("items");
            JsonNode itemArray = itemsNode.path("item");

            // 응답 상태 확인
            String resultCode = responseNode.path("header").path("resultCode").asText();
            if (!"0000".equals(resultCode)) {
                log.warn("Tour API 응답 에러: {}", responseNode.path("header").path("resultMsg").asText());
                return List.of();
            }

            var tourPlaces = new ArrayList<TourPlace>();
            
            // 배열 형태의 아이템 파싱
            if (itemArray.isArray()) {
                for (JsonNode item : itemArray) {
                    var tourPlace = parseTourPlaceItem(item);
                    if (tourPlace != null) {
                        tourPlaces.add(tourPlace);
                    }
                }
            }

            log.info("Tour API 파싱 완료: {}개 관광지", tourPlaces.size());
            return tourPlaces;

        } catch (Exception e) {
            log.error("Tour API 응답 파싱 실패", e);
            return List.of();
        }
    }

    // 개별 관광지 아이템 파싱 (실제 API 응답 구조 적용)
    private TourPlace parseTourPlaceItem(JsonNode item) {
        try {
            String title = item.path("title").asText("");
            String addr1 = item.path("addr1").asText("");
            String addr2 = item.path("addr2").asText("");
            String address = (addr1 + " " + addr2).trim();
            
            // 카테고리 매핑 (실제 API 응답 기준)
            String contentTypeId = item.path("contenttypeid").asText("");
            String category = mapContentTypeToCategory(contentTypeId);
            
            // 평점 기본값 (API에서 제공하지 않음)
            Double rating = 4.0;
            
            // 전화번호와 함께 상세 설명 구성
            String tel = item.path("tel").asText("");
            String contentId = item.path("contentid").asText("");
            String description = String.format("한국관광공사 공식 정보 (ID: %s)%s", 
                contentId, 
                !tel.isEmpty() ? " | 전화: " + tel : "");

            // 빈 제목이나 주소인 경우 건너뛰기
            if (title.isEmpty() || address.trim().isEmpty()) {
                log.debug("빈 제목 또는 주소로 인해 건너뛰기: {}", contentId);
                return null;
            }

            return new TourPlace(title, address, category, rating, description);

        } catch (Exception e) {
            log.warn("관광지 아이템 파싱 실패", e);
            return null;
        }
    }

    // 콘텐츠 타입 ID를 카테고리로 매핑
    private String mapContentTypeToCategory(String contentTypeId) {
        return switch (contentTypeId) {
            case "12" -> "관광지";
            case "14" -> "문화시설";
            case "15" -> "축제공연행사";
            case "25" -> "여행코스";
            case "28" -> "레포츠";
            case "32" -> "숙박";
            case "38" -> "쇼핑";
            case "39" -> "음식점";
            default -> "기타";
        };
    }

    // 위치 Record
    public record Location(
        String name,            // 지역명
        String code             // 지역 코드
    ) {}

    // 관광지 Record
    public record TourPlace(
        String name,            // 관광지명
        String address,         // 주소
        String category,        // 카테고리
        Double rating,          // 평점
        String description      // 설명
    ) {}
}

