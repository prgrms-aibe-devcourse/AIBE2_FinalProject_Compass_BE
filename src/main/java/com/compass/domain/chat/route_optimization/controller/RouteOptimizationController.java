package com.compass.domain.chat.route_optimization.controller;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationRequest;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationResponse;
import com.compass.domain.chat.route_optimization.model.UserSelectionRequest;
import com.compass.domain.chat.route_optimization.service.RouteOptimizationOrchestrationService;
import com.compass.domain.chat.route_optimization.service.UserSelectionRouteOptimizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/route-optimization")
@RequiredArgsConstructor
@Tag(name = "Route Optimization", description = "여행 동선 최적화 API")
public class RouteOptimizationController {

    private final RouteOptimizationOrchestrationService orchestrationService;
    private final UserSelectionRouteOptimizer userSelectionOptimizer;

    /**
     * Stage 2 결과에서 사용자 선택을 위한 후보 리스트 조회
     */
    @GetMapping("/candidates/{sessionId}")
    @Operation(summary = "동선 최적화 후보 조회", description = "일자별 여행지 후보 리스트를 조회합니다")
    public ResponseEntity<CandidatesResponse> getCandidates(@PathVariable Long sessionId) {
        log.info("후보 리스트 조회: sessionId={}", sessionId);

        // TODO: DB에서 Stage 2 결과 조회
        // 임시로 Mock 데이터 반환
        Map<Integer, List<PlaceSummary>> candidates = generateMockCandidates();

        return ResponseEntity.ok(new CandidatesResponse(
            sessionId,
            candidates,
            "각 날짜별로 3-5개의 꼭 가고 싶은 장소를 선택해주세요"
        ));
    }

    /**
     * 사용자 선택 기반 동선 최적화
     */
    @PostMapping("/optimize-with-selection")
    @Operation(summary = "사용자 선택 기반 동선 최적화", description = "사용자가 선택한 장소를 기반으로 최적 동선을 생성합니다")
    public ResponseEntity<RouteOptimizationResponse> optimizeWithUserSelection(
        @RequestBody UserSelectionRequest request
    ) {
        log.info("사용자 선택 기반 최적화: sessionId={}, 선택 수={}",
            request.sessionId(),
            request.selectedPlaceIds().values().stream().mapToInt(List::size).sum()
        );

        // TODO: 실제 구현
        // 1. DB에서 전체 후보 리스트 조회
        // 2. 사용자 선택 장소 추출
        // 3. UserSelectionRouteOptimizer 호출
        // 4. 결과 반환

        return ResponseEntity.ok(RouteOptimizationResponse.success(
            Map.of(), // AI 추천 일정
            Map.of(), // 전체 후보
            Map.of()  // 경로 정보
        ));
    }

    /**
     * 자동 동선 최적화 (기존 방식)
     */
    @PostMapping("/optimize-auto")
    @Operation(summary = "자동 동선 최적화", description = "AI가 자동으로 최적 동선을 생성합니다")
    public ResponseEntity<RouteOptimizationResponse> optimizeAutomatically(
        @RequestBody RouteOptimizationRequest request
    ) {
        log.info("자동 동선 최적화: sessionId={}", request.threadId());

        RouteOptimizationResponse response = orchestrationService.processRouteOptimization(
            1L, // sessionId - TODO: 실제 세션 ID 사용
            request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Mock 데이터 생성 (테스트용)
     */
    private Map<Integer, List<PlaceSummary>> generateMockCandidates() {
        Map<Integer, List<PlaceSummary>> candidates = new HashMap<>();

        // Day 1 - 서울 중심부
        candidates.put(1, List.of(
            new PlaceSummary("place_1_1", "경복궁", "관광명소", 4.5, "서울 종로구", true),
            new PlaceSummary("place_1_2", "북촌한옥마을", "관광명소", 4.4, "서울 종로구", false),
            new PlaceSummary("place_1_3", "인사동", "쇼핑/문화", 4.2, "서울 종로구", false),
            new PlaceSummary("place_1_4", "명동", "쇼핑", 4.3, "서울 중구", true),
            new PlaceSummary("place_1_5", "남산타워", "관광명소", 4.5, "서울 용산구", true),
            new PlaceSummary("place_1_6", "동대문디자인플라자", "문화", 4.1, "서울 중구", false),
            new PlaceSummary("place_1_7", "광장시장", "맛집/시장", 4.3, "서울 종로구", false),
            new PlaceSummary("place_1_8", "청계천", "산책", 4.0, "서울 중구", false)
        ));

        // Day 2 - 강남/강동
        candidates.put(2, List.of(
            new PlaceSummary("place_2_1", "가로수길", "쇼핑", 4.2, "서울 강남구", false),
            new PlaceSummary("place_2_2", "코엑스", "쇼핑/전시", 4.1, "서울 강남구", true),
            new PlaceSummary("place_2_3", "봉은사", "사찰", 4.3, "서울 강남구", false),
            new PlaceSummary("place_2_4", "롯데월드타워", "관광명소", 4.4, "서울 송파구", true),
            new PlaceSummary("place_2_5", "석촌호수", "산책", 4.2, "서울 송파구", false),
            new PlaceSummary("place_2_6", "압구정로데오", "쇼핑", 4.0, "서울 강남구", false)
        ));

        // Day 3 - 서울 서부
        candidates.put(3, List.of(
            new PlaceSummary("place_3_1", "홍대입구", "문화/쇼핑", 4.2, "서울 마포구", true),
            new PlaceSummary("place_3_2", "연남동", "카페/맛집", 4.3, "서울 마포구", false),
            new PlaceSummary("place_3_3", "이태원", "다국적 문화", 4.1, "서울 용산구", false),
            new PlaceSummary("place_3_4", "한강공원", "휴식", 4.4, "서울 마포구", false),
            new PlaceSummary("place_3_5", "연세대학교", "관광", 3.9, "서울 서대문구", false),
            new PlaceSummary("place_3_6", "서울숲", "공원", 4.3, "서울 성동구", true)
        ));

        return candidates;
    }

    /**
     * 후보 리스트 응답
     */
    public record CandidatesResponse(
        Long sessionId,
        Map<Integer, List<PlaceSummary>> candidatesByDay,
        String instruction
    ) {}

    /**
     * 장소 요약 정보
     */
    public record PlaceSummary(
        String id,
        String name,
        String category,
        double rating,
        String address,
        boolean isPopular  // 인기 장소 표시
    ) {}
}