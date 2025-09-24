package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelPlaceEntity;
import com.compass.domain.chat.repository.TravelPlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 여행 장소 저장 및 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlaceService {

    private final TravelPlaceRepository travelPlaceRepository;

    /**
     * Stage 1 결과를 데이터베이스에 저장
     */
    @Transactional
    public List<TravelPlaceEntity> saveStage1Results(
            String threadId,
            String userId,
            String destination,
            PlaceSelectionService.Stage1Output stage1Output,
            String travelStyle,
            String budget) {

        log.info("Stage 1 결과 저장 시작: threadId={}, 장소 수={}", threadId, stage1Output.places().size());

        // 기존 데이터 삭제 (같은 스레드의 이전 결과)
        travelPlaceRepository.deleteByThreadId(threadId);

        // 새로운 장소 데이터 저장
        List<TravelPlaceEntity> entities = stage1Output.places().stream()
                .map(place -> TravelPlaceEntity.fromTourPlace(
                        threadId, userId, destination, place, 
                        travelStyle, budget, stage1Output.tripDays()))
                .collect(Collectors.toList());

        List<TravelPlaceEntity> savedEntities = travelPlaceRepository.saveAll(entities);

        log.info("Stage 1 결과 저장 완료: {} 개 장소 저장됨", savedEntities.size());
        return savedEntities;
    }

    /**
     * 스레드 ID로 저장된 여행 장소 조회
     */
    @Transactional(readOnly = true)
    public List<TravelPlaceEntity> getPlacesByThreadId(String threadId) {
        return travelPlaceRepository.findByThreadIdOrderByCreatedAtDesc(threadId);
    }

    /**
     * 목적지별 여행 장소 조회 (평점 순)
     */
    @Transactional(readOnly = true)
    public List<TravelPlaceEntity> getPlacesByDestination(String destination) {
        return travelPlaceRepository.findByDestinationOrderByRatingDesc(destination);
    }

    /**
     * 스레드의 여행 장소 통계 조회
     */
    @Transactional(readOnly = true)
    public TravelPlaceStatistics getStatistics(String threadId) {
        long totalCount = travelPlaceRepository.countByThreadId(threadId);
        
        List<Object[]> categoryStats = travelPlaceRepository.findCategoryStatsByThreadId(threadId);
        Map<String, Long> categoryDistribution = categoryStats.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        Double averageRating = travelPlaceRepository.findAverageRatingByThreadId(threadId);

        return new TravelPlaceStatistics(
                (int) totalCount,
                categoryDistribution,
                averageRating != null ? averageRating : 0.0
        );
    }

    /**
     * 스레드의 여행 장소 삭제
     */
    @Transactional
    public void deletePlacesByThreadId(String threadId) {
        log.info("스레드 {} 의 여행 장소 삭제", threadId);
        travelPlaceRepository.deleteByThreadId(threadId);
    }

    /**
     * 여행 장소 통계 정보
     */
    public record TravelPlaceStatistics(
            int totalCount,
            Map<String, Long> categoryDistribution,
            double averageRating
    ) {}
}

