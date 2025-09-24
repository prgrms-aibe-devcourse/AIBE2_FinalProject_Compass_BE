package com.compass.domain.chat.function.external;

import com.compass.domain.chat.service.PerplexityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

// Perplexity API 검색 Function
@Slf4j
@Component
public class SearchWithPerplexityFunction implements Function<SearchWithPerplexityFunction.SearchQuery, List<SearchWithPerplexityFunction.SearchResult>> {

    private final PerplexityClient perplexityClient;

    public SearchWithPerplexityFunction(PerplexityClient perplexityClient) {
        this.perplexityClient = perplexityClient;
    }

    @Override
    public List<SearchResult> apply(SearchQuery searchQuery) {
        log.info("Perplexity 검색 시작: {}", searchQuery.query());

        try {
            // Perplexity API로 검색 수행
            String response = perplexityClient.search(buildSearchPrompt(searchQuery));
            
            // 응답을 SearchResult 리스트로 변환
            return parseSearchResults(response, searchQuery);

        } catch (Exception e) {
            log.error("Perplexity 검색 실패: {}", searchQuery.query(), e);
            return List.of();
        }
    }

    // 검색 프롬프트 생성
    private String buildSearchPrompt(SearchQuery query) {
        return String.format("""
            %s 지역의 %s에 대한 최신 정보를 검색해주세요.
            다음 정보를 포함해주세요:
            - 장소명
            - 주소
            - 운영시간
            - 추천 이유
            - 최신 리뷰나 트렌드
            """, 
            query.location(), 
            query.category()
        );
    }

    // 검색 결과 파싱
    private List<SearchResult> parseSearchResults(String response, SearchQuery query) {
        try {
            // 실제 장소명 추출 시도
            String placeName = extractPlaceName(response);
            String address = extractAddress(response);
            String hours = extractHours(response);
            
            return List.of(
                new SearchResult(
                    placeName,
                    query.location(),
                    address,
                    hours,
                    response.substring(0, Math.min(500, response.length())),
                    4.5
                )
            );
        } catch (Exception e) {
            log.warn("검색 결과 파싱 실패, 기본값 사용: {}", e.getMessage());
            return List.of(
                new SearchResult(
                    "검색된 장소",
                    query.location(),
                    "주소 정보",
                    "운영시간 정보",
                    response.substring(0, Math.min(100, response.length())),
                    4.5
                )
            );
        }
    }
    
    // 장소명 추출
    private String extractPlaceName(String response) {
        // **장소명** 패턴 찾기
        if (response.contains("**")) {
            String[] parts = response.split("\\*\\*");
            for (int i = 1; i < parts.length; i += 2) {
                if (parts[i].length() > 2 && parts[i].length() < 50) {
                    return parts[i].trim();
                }
            }
        }
        return "검색된 장소";
    }
    
    // 주소 추출
    private String extractAddress(String response) {
        if (response.contains("주소:")) {
            String[] parts = response.split("주소:");
            if (parts.length > 1) {
                String address = parts[1].split("\n")[0].trim();
                return address.length() > 50 ? address.substring(0, 50) : address;
            }
        }
        return "주소 정보";
    }
    
    // 운영시간 추출
    private String extractHours(String response) {
        if (response.contains("운영시간:")) {
            String[] parts = response.split("운영시간:");
            if (parts.length > 1) {
                String hours = parts[1].split("\n")[0].trim();
                return hours.length() > 30 ? hours.substring(0, 30) : hours;
            }
        }
        return "운영시간 정보";
    }

    // 검색 쿼리 Record
    public record SearchQuery(
        String query,        // 검색어
        String location,     // 지역
        String category      // 카테고리 (맛집, 관광지, 숙박 등)
    ) {}

    // 검색 결과 Record
    public record SearchResult(
        String name,         // 장소명
        String location,     // 위치
        String address,      // 주소
        String hours,        // 운영시간
        String description,  // 설명
        Double rating        // 평점
    ) {}
}

