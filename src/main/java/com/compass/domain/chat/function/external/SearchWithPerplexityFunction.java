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
        // 기본 검색 결과 반환 (향후 실제 파싱 로직 구현 예정)
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

