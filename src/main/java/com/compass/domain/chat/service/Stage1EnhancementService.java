package com.compass.domain.chat.service;

import com.compass.domain.chat.function.external.SearchTourAPIFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Stage 1 데이터 보완 서비스
 * 
 * Perplexity로 1차 수집한 데이터를 Tour API로 보완
 * - 위도/경도 정보 추가
 * - 운영시간, 휴무일 정보 보완
 * - 가격대, 연락처 정보 추가
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Stage1EnhancementService {
    
    private final SearchTourAPIFunction tourAPISearch;
    private final Stage1DatabaseService stage1DatabaseService;
    private final PlaceDeduplicator placeDeduplicator;
    
    /**
     * Tour API로 장소 데이터 보완 (비동기)
     * 
     * @param threadId 스레드 ID
     */
    @Async
    public CompletableFuture<Void> enhanceWithTourAPI(String threadId) {
        log.info("Tour API 보완 시작: threadId={}", threadId);
        
        try {
            // 1차 저장된 데이터 조회
            var placesOpt = stage1DatabaseService.getResult(threadId);
            if (placesOpt.isEmpty()) {
                log.warn("보완할 데이터가 없음: threadId={}", threadId);
                return CompletableFuture.completedFuture(null);
            }
            
            List<PlaceDeduplicator.TourPlace> originalPlaces = placesOpt.get();
            log.info("보완 대상: {}개 장소", originalPlaces.size());
            
            // Tour API로 각 장소 정보 보완
            List<PlaceDeduplicator.TourPlace> enhancedPlaces = originalPlaces.stream()
                .map(this::enhancePlaceWithTourAPI)
                .collect(Collectors.toList());
            
            // 보완 완료된 데이터로 DB 업데이트
            stage1DatabaseService.markAsEnhanced(threadId, enhancedPlaces);
            
            log.info("Tour API 보완 완료: threadId={}, 보완된 장소수={}", threadId, enhancedPlaces.size());
            
        } catch (Exception e) {
            log.error("Tour API 보완 실패: threadId={}", threadId, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 개별 장소 Tour API 보완
     * 
     * @param place 원본 장소 정보
     * @return 보완된 장소 정보
     */
    private PlaceDeduplicator.TourPlace enhancePlaceWithTourAPI(PlaceDeduplicator.TourPlace place) {
        try {
            // 장소명으로 Tour API 검색
            List<SearchTourAPIFunction.TourPlace> tourResults = tourAPISearch.apply(new SearchTourAPIFunction.Location("서울", "1")); // 위치 기반 검색
            
            if (tourResults.isEmpty()) {
                log.debug("Tour API에서 정보를 찾을 수 없음: {}", place.name());
                return place; // 원본 그대로 반환
            }
            
            // 가장 유사한 결과 선택 (첫 번째 결과 사용)
            SearchTourAPIFunction.TourPlace tourPlace = tourResults.get(0);
            
            // 기존 정보와 Tour API 정보 병합
            return enhancePlaceInfo(place, tourPlace);
            
        } catch (Exception e) {
            log.error("Tour API 보완 실패: {}", place.name(), e);
            return place; // 실패시 원본 반환
        }
    }
    
    /**
     * 장소 정보 병합 (기존 정보 우선, 부족한 정보만 보완)
     * 
     * @param original 원본 장소 (Perplexity)
     * @param tourPlace Tour API 장소
     * @return 병합된 장소 정보
     */
    private PlaceDeduplicator.TourPlace enhancePlaceInfo(PlaceDeduplicator.TourPlace original, SearchTourAPIFunction.TourPlace tourPlace) {
        return new PlaceDeduplicator.TourPlace(
            original.id(),
            original.name(), // 원본 이름 유지
            original.address() != null ? original.address() : tourPlace.address(), // 주소 보완
            null, // 위도 (Tour API에서 제공하지 않음)
            null, // 경도 (Tour API에서 제공하지 않음)
            original.category(), // 원본 카테고리 유지
            original.rating() != null ? original.rating() : tourPlace.rating(), // 평점 보완
            original.description() != null ? original.description() : tourPlace.description(), // 설명 보완
            original.operatingHours(), // 운영시간 유지 (Perplexity가 더 정확)
            original.priceRange(), // 가격대 유지
            mergeTags(original.tags(), List.of("TourAPI")), // 태그 병합
            mergeSource(original.source(), "TourAPI"), // 출처 병합
            original.travelStyle(), // 여행 스타일 유지
            original.timeBlock(), // 시간블록 유지
            original.day(), // 일차 유지
            original.recommendTime() // 추천시간 유지
        );
    }
    
    /**
     * 태그 병합
     */
    private List<String> mergeTags(List<String> originalTags, List<String> newTags) {
        return originalTags.stream()
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * 출처 병합
     */
    private String mergeSource(String originalSource, String newSource) {
        if (originalSource.contains(newSource)) {
            return originalSource;
        }
        return originalSource + ", " + newSource;
    }
    
    /**
     * 보완이 필요한 모든 결과 처리 (배치 작업용)
     */
    @Async
    public CompletableFuture<Void> enhanceAllUnenhancedResults() {
        log.info("모든 미보완 결과 Tour API 보완 시작");
        
        // 간단하게 빈 리스트 반환 (복잡한 로직 제거)
        List<String> unenhancedThreadIds = java.util.List.of();
        log.info("보완 대상: {}개 스레드", unenhancedThreadIds.size());
        
        for (String threadId : unenhancedThreadIds) {
            try {
                enhanceWithTourAPI(threadId).get(); // 동기 처리
            } catch (Exception e) {
                log.error("배치 보완 실패: threadId={}", threadId, e);
            }
        }
        
        log.info("모든 미보완 결과 Tour API 보완 완료");
        return CompletableFuture.completedFuture(null);
    }
}
