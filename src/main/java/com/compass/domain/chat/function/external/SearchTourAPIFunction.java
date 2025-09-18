package com.compass.domain.chat.function.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// 한국관광공사 API 검색 Function
@Slf4j
@Component
public class SearchTourAPIFunction implements Function<SearchTourAPIFunction.Location, List<SearchTourAPIFunction.TourPlace>> {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${TOUR_API_KEY}")
    private String tourApiKey;

    @Value("${TOUR_API_URL:http://apis.data.go.kr/B551011/KorService1}")
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

    // 실제 Tour API 호출
    private String performTourAPICall(Location location) {
        var url = String.format(
            "%s/areaBasedList1?serviceKey=%s&numOfRows=20&pageNo=1&MobileOS=ETC&MobileApp=Compass&_type=json&areaCode=%s",
            tourApiUrl,
            tourApiKey,
            getAreaCode(location.name())
        );

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("관광공사 API 응답 실패: " + response.getStatusCode());
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
        // TODO: 실제 JSON 파싱 로직 구현 예정
        // 현재는 플레이스홀더로 구현
        try {
            if (response == null || response.trim().isEmpty()) {
                return List.of();
            }

            // 임시 응답 생성 (실제 API 응답 구조 파악 후 개선)
            return List.of(
                new TourPlace(
                    "관광지명 예시",
                    "주소 정보",
                    "관광지",
                    4.0,
                    "한국관광공사 공식 관광지 정보"
                )
            );
        } catch (Exception e) {
            log.error("Tour API 응답 파싱 실패", e);
            return List.of();
        }
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

