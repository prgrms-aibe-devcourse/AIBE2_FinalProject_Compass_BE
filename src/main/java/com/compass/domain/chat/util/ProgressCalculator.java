package com.compass.domain.chat.util;

import com.compass.domain.chat.constant.FollowUpConstants;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 여행 정보 수집 진행률 계산 유틸리티
 * 중복된 진행률 계산 로직을 중앙화
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProgressCalculator {
    
    /**
     * 여행 정보 수집 진행률 계산
     * 
     * @param state 현재 수집 상태
     * @return 진행률 (0-100)
     */
    public static int calculateProgress(TravelInfoCollectionState state) {
        if (state == null) {
            return 0;
        }
        
        int completedFields = countCompletedFields(state);
        int percentage = (completedFields * FollowUpConstants.PROGRESS_PERCENTAGE_MULTIPLIER) 
                        / FollowUpConstants.TOTAL_REQUIRED_FIELDS;
        
        log.debug("Progress calculation: {}/{} fields completed = {}%", 
                 completedFields, FollowUpConstants.TOTAL_REQUIRED_FIELDS, percentage);
        
        return Math.min(percentage, 100); // 100%를 초과하지 않도록 보장
    }
    
    /**
     * 완료된 필드 개수 계산
     */
    private static int countCompletedFields(TravelInfoCollectionState state) {
        int count = 0;
        
        if (state.isOriginCollected()) count++;
        if (state.isDestinationCollected()) count++;
        if (state.isDatesCollected()) count++;
        if (state.isDurationCollected()) count++;
        if (state.isCompanionsCollected()) count++;
        if (state.isBudgetCollected()) count++;
        
        return count;
    }
    
    /**
     * 여행 계획 생성 가능 여부 판단
     * 모든 필수 정보가 수집되어야 계획 생성 가능 (100% 완료)
     */
    public static boolean canGenerateTravelPlan(TravelInfoCollectionState state) {
        if (state == null) {
            return false;
        }
        
        // 모든 필수 필드가 수집되었는지 확인
        boolean hasOrigin = state.isOriginCollected();
        boolean hasDestination = state.isDestinationCollected();
        boolean hasTimeInfo = state.isDatesCollected() || state.isDurationCollected();
        boolean hasCompanions = state.isCompanionsCollected();
        boolean hasBudget = state.isBudgetCollected();
        
        int completedCount = countCompletedFields(state);
        double completionRatio = (double) completedCount / FollowUpConstants.TOTAL_REQUIRED_FIELDS;
        
        // 모든 필수 정보가 수집되고, 완성도가 100%일 때만 계획 생성 가능
        boolean canGenerate = hasOrigin && hasDestination && hasTimeInfo 
                            && hasCompanions && hasBudget
                            && completionRatio >= 1.0;
        
        log.debug("Can generate plan check - Origin: {}, Destination: {}, Time: {}, Companions: {}, Budget: {}, Completion: {:.0%} = {}",
                 hasOrigin, hasDestination, hasTimeInfo, hasCompanions, hasBudget, completionRatio, canGenerate);
        
        return canGenerate;
    }
    
    /**
     * 다음 필요한 필드 계산
     */
    public static TravelInfoCollectionState.CollectionStep getNextRequiredStep(TravelInfoCollectionState state) {
        if (state == null) {
            return TravelInfoCollectionState.CollectionStep.DESTINATION;
        }
        
        // 우선순위에 따라 다음 단계 결정
        if (!state.isOriginCollected()) {
            return TravelInfoCollectionState.CollectionStep.ORIGIN;
        }
        if (!state.isDestinationCollected()) {
            return TravelInfoCollectionState.CollectionStep.DESTINATION;
        }
        if (!state.isDatesCollected()) {
            return TravelInfoCollectionState.CollectionStep.DATES;
        }
        if (!state.isDurationCollected()) {
            return TravelInfoCollectionState.CollectionStep.DURATION;
        }
        if (!state.isCompanionsCollected()) {
            return TravelInfoCollectionState.CollectionStep.COMPANIONS;
        }
        if (!state.isBudgetCollected()) {
            return TravelInfoCollectionState.CollectionStep.BUDGET;
        }
        
        return TravelInfoCollectionState.CollectionStep.CONFIRMATION;
    }
}