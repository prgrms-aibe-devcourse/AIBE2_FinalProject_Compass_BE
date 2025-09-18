package com.compass.domain.chat.function.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.function.Function;

// 한국관광공사 API 검색 Function
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchTourAPIFunction implements Function<SearchTourAPIFunction.Location, List<SearchTourAPIFunction.TourPlace>> {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${TOUR_API_KEY}")
    private String tourApiKey;

    @Value("${TOUR_API_URL:http://apis.data.go.kr/B551011/KorService1}")
    private String tourApiUrl;

    @Override
    public List<TourPlace> apply(Location location) {
        log.info("관광공사 API 검색 시작: {}", location.name());

        try {
            // 한국관광공사 API 호출
            String response = callTourAPI(location);
            
            // 응답을 TourPlace 리스트로 변환
            return parseTourAPIResults(response);

        } catch (Exception e) {
            log.error("관광공사 API 검색 실패: {}", location.name(), e);
            return List.of();
        }
    }

    // Tour API 호출
    private String callTourAPI(Location location) {
        var url = String.format(
            "%s/areaBasedList1?serviceKey=%s&numOfRows=20&pageNo=1&MobileOS=ETC&MobileApp=Compass&_type=json&areaCode=%s",
            tourApiUrl,
            tourApiKey,
            getAreaCode(location.name())
        );

        // TODO: 실제 API 호출 구현
        return "{}"; // 임시 응답
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
        // TODO: 실제 JSON 파싱 로직 구현
        return List.of(
            new TourPlace(
                "관광지명",
                "주소",
                "관광지",
                4.0,
                "공식 관광지 정보"
            )
        );
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

