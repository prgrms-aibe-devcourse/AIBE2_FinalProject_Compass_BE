package com.compass.domain.chat.function.planning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

// 목적지 검색 Function
@Slf4j
@Component
@RequiredArgsConstructor
public class DestinationSearchFunction implements Function<DestinationSearchFunction.DestinationRequest, DestinationSearchFunction.DestinationSearchResult> {

    @Override
    public DestinationSearchResult apply(DestinationRequest request) {
        log.info("목적지 검색 시작: {}", request.destination());

        try {
            // 목적지가 정해진 경우 vs 미정인 경우 처리
            if (request.hasDestination()) {
                return searchExistingDestination(request);
            } else {
                return recommendDestinations(request);
            }

        } catch (Exception e) {
            log.error("목적지 검색 실패", e);
            return DestinationSearchResult.error("목적지 검색 중 오류가 발생했습니다");
        }
    }

    // 기존 목적지 검색
    private DestinationSearchResult searchExistingDestination(DestinationRequest request) {
        // DB와 Perplexity 기반 목적지 검색
        return new DestinationSearchResult(
            request.destination(),
            "검색된 목적지 정보",
            true
        );
    }

    // 추천 목적지 생성
    private DestinationSearchResult recommendDestinations(DestinationRequest request) {
        // 추천 목적지 생성 로직
        return new DestinationSearchResult(
            "추천 목적지",
            "추천 목적지 정보",
            false
        );
    }

    // 목적지 요청 Record
    public record DestinationRequest(
        String destination,      // 목적지
        String departureLocation, // 출발지
        String travelStyle       // 여행 스타일
    ) {
        public boolean hasDestination() {
            return destination != null && !destination.isEmpty() && !"목적지 미정".equals(destination);
        }
    }

    // 목적지 검색 결과 Record
    public record DestinationSearchResult(
        String destination,      // 선택된 목적지
        String description,      // 설명
        boolean isExisting       // 기존 목적지 여부
    ) {
        public static DestinationSearchResult error(String message) {
            return new DestinationSearchResult("", message, false);
        }
    }
}

