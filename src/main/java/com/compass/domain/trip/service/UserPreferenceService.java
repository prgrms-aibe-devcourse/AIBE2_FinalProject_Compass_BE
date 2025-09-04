package com.compass.domain.trip.service;

import com.compass.domain.trip.dto.BudgetRequest;
import com.compass.domain.trip.dto.BudgetResponse;
import com.compass.domain.trip.dto.TravelStyleItem;
import com.compass.domain.trip.dto.TravelStylePreferenceRequest;
import com.compass.domain.trip.dto.TravelStylePreferenceResponse;
import com.compass.domain.trip.entity.UserPreference;
import com.compass.domain.trip.enums.BudgetLevel;
import com.compass.domain.trip.enums.TravelStyle;
import com.compass.domain.trip.exception.DuplicateTravelStyleException;
import com.compass.domain.trip.exception.InvalidWeightRangeException;
import com.compass.domain.trip.exception.InvalidWeightSumException;
import com.compass.domain.trip.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 선호도 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    
    private static final String TRAVEL_STYLE_TYPE = "TRAVEL_STYLE";
    private static final String BUDGET_LEVEL_TYPE = "BUDGET_LEVEL";

    /**
     * 여행 스타일 선호도 설정
     * 
     * @param userId 사용자 ID
     * @param request 설정 요청
     * @return 설정 결과
     */
    @Transactional
    public TravelStylePreferenceResponse setTravelStylePreferences(Long userId, 
                                                                  TravelStylePreferenceRequest request) {
        log.info("Setting travel style preferences for user: {}", userId);
        
        // 유효성 검증
        validateTravelStylePreferenceRequest(request);
        
        // 기존 선호도 삭제
        userPreferenceRepository.deleteByUserIdAndPreferenceType(userId, TRAVEL_STYLE_TYPE);
        
        // 새로운 선호도 저장
        List<UserPreference> preferences = createUserPreferences(userId, request.getPreferences());
        userPreferenceRepository.saveAll(preferences);
        
        // 응답 생성
        List<TravelStyleItem> responseItems = convertToTravelStyleItems(preferences);
        
        log.info("Successfully set travel style preferences for user: {}", userId);
        return TravelStylePreferenceResponse.success(
                userId, 
                responseItems, 
                "여행 스타일 선호도가 성공적으로 설정되었습니다."
        );
    }

    /**
     * 여행 스타일 선호도 조회
     * 
     * @param userId 사용자 ID
     * @return 조회 결과
     */
    public TravelStylePreferenceResponse getTravelStylePreferences(Long userId) {
        log.info("Getting travel style preferences for user: {}", userId);
        
        List<UserPreference> preferences = userPreferenceRepository
                .findTravelStylePreferencesByUserId(userId);
        
        if (preferences.isEmpty()) {
            log.info("No travel style preferences found for user: {}", userId);
            return TravelStylePreferenceResponse.empty(
                    userId, 
                    "설정된 여행 스타일 선호도가 없습니다."
            );
        }
        
        List<TravelStyleItem> responseItems = convertToTravelStyleItems(preferences);
        
        log.info("Found {} travel style preferences for user: {}", preferences.size(), userId);
        return TravelStylePreferenceResponse.success(userId, responseItems, null);
    }

    /**
     * 여행 스타일 선호도 수정
     * 
     * @param userId 사용자 ID
     * @param request 수정 요청
     * @return 수정 결과
     */
    @Transactional
    public TravelStylePreferenceResponse updateTravelStylePreferences(Long userId, 
                                                                     TravelStylePreferenceRequest request) {
        log.info("Updating travel style preferences for user: {}", userId);
        
        // 유효성 검증
        validateTravelStylePreferenceRequest(request);
        
        // 기존 선호도 삭제 후 새로운 선호도 저장 (간단한 구현)
        userPreferenceRepository.deleteByUserIdAndPreferenceType(userId, TRAVEL_STYLE_TYPE);
        
        List<UserPreference> preferences = createUserPreferences(userId, request.getPreferences());
        userPreferenceRepository.saveAll(preferences);
        
        // 응답 생성
        List<TravelStyleItem> responseItems = convertToTravelStyleItems(preferences);
        
        log.info("Successfully updated travel style preferences for user: {}", userId);
        return TravelStylePreferenceResponse.success(
                userId, 
                responseItems, 
                "여행 스타일 선호도가 성공적으로 수정되었습니다."
        );
    }
    
    // 예산 수준 설정 또는 수정
    @Transactional
    public BudgetResponse setOrUpdateBudgetLevel(Long userId, BudgetRequest request) {
        BudgetLevel budgetLevel = BudgetLevel.fromString(request.getBudgetLevel());
        if (budgetLevel == null) {
            throw new IllegalArgumentException("유효하지 않은 예산 수준입니다.");
        }

        Optional<UserPreference> existingPreference = userPreferenceRepository.findByUserIdAndPreferenceType(userId, BUDGET_LEVEL_TYPE).stream().findFirst();

        UserPreference preferenceToSave = existingPreference.orElseGet(() -> UserPreference.builder()
                .userId(userId)
                .preferenceType(BUDGET_LEVEL_TYPE)
                .build());
        
        preferenceToSave.updateBudgetData(budgetLevel);

        userPreferenceRepository.save(preferenceToSave);
        return BudgetResponse.from(userId, budgetLevel, "예산 수준이 성공적으로 설정되었습니다.");
    }
    
    // 예산 수준 조회
    public BudgetResponse getBudgetLevel(Long userId) {
        Optional<UserPreference> preference = userPreferenceRepository.findByUserIdAndPreferenceType(userId, BUDGET_LEVEL_TYPE).stream().findFirst();

        if (preference.isPresent()) {
            BudgetLevel budgetLevel = BudgetLevel.fromString(preference.get().getPreferenceKey());
            return BudgetResponse.of(userId, budgetLevel);
        } else {
            return BudgetResponse.from(userId, null, "설정된 예산 수준이 없습니다.");
        }
    }

    /**
     * 여행 스타일 선호도 요청 유효성 검증
     * 
     * @param request 검증할 요청
     */
    private void validateTravelStylePreferenceRequest(TravelStylePreferenceRequest request) {
        // 중복 여행 스타일 검증
        if (request.hasDuplicateTravelStyles()) {
            List<String> duplicates = request.getDuplicateTravelStyles();
            throw new DuplicateTravelStyleException(duplicates);
        }
        
        // 여행 스타일 유효성 검증
        if (!request.areAllTravelStylesValid()) {
            List<String> invalidStyles = request.getInvalidTravelStyles();
            throw new IllegalArgumentException("유효하지 않은 여행 스타일: " + String.join(", ", invalidStyles));
        }
        
        // 가중치 범위 검증
        List<InvalidWeightRangeException.InvalidWeight> invalidWeights = validateWeightRanges(request.getPreferences());
        if (!invalidWeights.isEmpty()) {
            throw new InvalidWeightRangeException(invalidWeights);
        }
        
        // 가중치 합계 검증
        if (!request.isValidWeightSum()) {
            BigDecimal actualSum = request.getTotalWeight();
            throw new InvalidWeightSumException(actualSum);
        }
    }

    /**
     * 가중치 범위 검증
     * 
     * @param preferences 선호도 목록
     * @return 유효하지 않은 가중치 목록
     */
    private List<InvalidWeightRangeException.InvalidWeight> validateWeightRanges(List<TravelStyleItem> preferences) {
        List<InvalidWeightRangeException.InvalidWeight> invalidWeights = new ArrayList<>();
        
        for (TravelStyleItem item : preferences) {
            BigDecimal weight = item.getWeight();
            if (weight.compareTo(BigDecimal.ZERO) < 0) {
                invalidWeights.add(new InvalidWeightRangeException.InvalidWeight(
                        item.getTravelStyle(), weight, "가중치가 0.0 미만입니다."));
            } else if (weight.compareTo(BigDecimal.ONE) > 0) {
                invalidWeights.add(new InvalidWeightRangeException.InvalidWeight(
                        item.getTravelStyle(), weight, "가중치가 1.0을 초과합니다."));
            }
        }
        
        return invalidWeights;
    }

    /**
     * UserPreference 엔티티 목록 생성
     * 
     * @param userId 사용자 ID
     * @param items 여행 스타일 항목 목록
     * @return UserPreference 목록
     */
    private List<UserPreference> createUserPreferences(Long userId, List<TravelStyleItem> items) {
        return items.stream()
                .map(item -> {
                    TravelStyle travelStyle = item.getTravelStyleEnum();
                    return UserPreference.createTravelStylePreference(
                            userId,
                            travelStyle.name(),
                            item.getWeight(),
                            travelStyle.getDescription()
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * UserPreference를 TravelStyleItem으로 변환
     * 
     * @param preferences UserPreference 목록
     * @return TravelStyleItem 목록
     */
    private List<TravelStyleItem> convertToTravelStyleItems(List<UserPreference> preferences) {
        return preferences.stream()
                .map(preference -> {
                    TravelStyle travelStyle = TravelStyle.fromString(preference.getPreferenceKey());
                    return TravelStyleItem.from(
                            travelStyle,
                            preference.getPreferenceValue(),
                            preference.getCreatedAt(),
                            preference.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());
    }
}
