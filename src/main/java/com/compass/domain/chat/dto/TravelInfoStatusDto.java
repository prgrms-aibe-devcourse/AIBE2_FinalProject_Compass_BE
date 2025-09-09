package com.compass.domain.chat.dto;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 여행 정보 수집 상태 DTO
 * REQ-FOLLOW-002: 현재 수집 상태와 진행 상황을 전달
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelInfoStatusDto {
    
    /**
     * 세션 ID
     */
    private String sessionId;
    
    /**
     * 사용자 ID
     */
    private Long userId;
    
    /**
     * 채팅 스레드 ID
     */
    private String chatThreadId;
    
    /**
     * 현재 수집 단계
     */
    private TravelInfoCollectionState.CollectionStep currentStep;
    
    /**
     * 수집 완료 여부
     */
    private boolean isCompleted;
    
    /**
     * 완료 퍼센트 (0-100)
     */
    private int completionPercentage;
    
    /**
     * 수집된 정보 상태
     */
    private CollectionFieldStatus fieldStatus;
    
    /**
     * 수집된 정보 상세
     */
    private CollectedInfo collectedInfo;
    
    /**
     * 다음 필요한 단계
     */
    private TravelInfoCollectionState.CollectionStep nextRequiredStep;
    
    /**
     * 마지막 질문
     */
    private String lastQuestionAsked;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 업데이트 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * 완료 시간
     */
    private LocalDateTime completedAt;
    
    /**
     * 예상 소요 시간 (분)
     */
    private Integer estimatedMinutesRemaining;
    
    /**
     * 각 필드별 수집 상태
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionFieldStatus {
        private boolean destinationCollected;
        private boolean datesCollected;
        private boolean durationCollected;
        private boolean companionsCollected;
        private boolean budgetCollected;
        
        /**
         * 수집된 필드 개수
         */
        public int getCollectedFieldCount() {
            int count = 0;
            if (destinationCollected) count++;
            if (datesCollected) count++;
            if (durationCollected) count++;
            if (companionsCollected) count++;
            if (budgetCollected) count++;
            return count;
        }
        
        /**
         * 전체 필드 개수
         */
        public int getTotalFieldCount() {
            return 5;
        }
    }
    
    /**
     * 수집된 정보 상세
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectedInfo {
        // 목적지 정보
        private String destination;
        
        // 날짜 정보
        private LocalDate startDate;
        private LocalDate endDate;
        
        // 기간 정보
        private Integer durationNights;
        private String durationDescription; // "2박 3일"
        
        // 동행자 정보
        private Integer numberOfTravelers;
        private String companionType;
        private String companionDescription; // "가족 4명"
        
        // 예산 정보
        private Integer budgetPerPerson;
        private String budgetCurrency;
        private String budgetLevel;
        private String budgetDescription; // "1인당 50만원 (적당한 수준)"
        
        // 추가 정보
        private Map<String, Object> additionalPreferences;
    }
    
    /**
     * Entity를 DTO로 변환
     */
    public static TravelInfoStatusDto fromEntity(TravelInfoCollectionState entity) {
        CollectionFieldStatus fieldStatus = CollectionFieldStatus.builder()
                .destinationCollected(entity.isDestinationCollected())
                .datesCollected(entity.isDatesCollected())
                .durationCollected(entity.isDurationCollected())
                .companionsCollected(entity.isCompanionsCollected())
                .budgetCollected(entity.isBudgetCollected())
                .build();
        
        CollectedInfo.CollectedInfoBuilder infoBuilder = CollectedInfo.builder()
                .destination(entity.getDestination())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .durationNights(entity.getDurationNights())
                .numberOfTravelers(entity.getNumberOfTravelers())
                .companionType(entity.getCompanionType())
                .budgetPerPerson(entity.getBudgetPerPerson())
                .budgetCurrency(entity.getBudgetCurrency())
                .budgetLevel(entity.getBudgetLevel());
        
        // 설명 텍스트 생성
        if (entity.getDurationNights() != null) {
            infoBuilder.durationDescription(String.format("%d박 %d일", 
                    entity.getDurationNights(), entity.getDurationNights() + 1));
        }
        
        if (entity.getCompanionType() != null && entity.getNumberOfTravelers() != null) {
            String companionLabel = switch (entity.getCompanionType()) {
                case "solo" -> "혼자";
                case "couple" -> "연인";
                case "family" -> "가족";
                case "friends" -> "친구";
                case "business" -> "동료";
                default -> entity.getCompanionType();
            };
            infoBuilder.companionDescription(String.format("%s %d명", 
                    companionLabel, entity.getNumberOfTravelers()));
        }
        
        if (entity.getBudgetPerPerson() != null || entity.getBudgetLevel() != null) {
            StringBuilder budgetDesc = new StringBuilder();
            if (entity.getBudgetPerPerson() != null) {
                budgetDesc.append(String.format("1인당 %d%s", 
                        entity.getBudgetPerPerson(), 
                        entity.getBudgetCurrency() != null ? entity.getBudgetCurrency() : "원"));
            }
            if (entity.getBudgetLevel() != null) {
                String levelLabel = switch (entity.getBudgetLevel()) {
                    case "budget" -> "알뜰한";
                    case "moderate" -> "적당한";
                    case "luxury" -> "럭셔리";
                    default -> entity.getBudgetLevel();
                };
                if (budgetDesc.length() > 0) {
                    budgetDesc.append(" (").append(levelLabel).append(" 수준)");
                } else {
                    budgetDesc.append(levelLabel).append(" 수준");
                }
            }
            infoBuilder.budgetDescription(budgetDesc.toString());
        }
        
        // 예상 소요 시간 계산 (각 필드당 약 1-2분)
        int remainingFields = 5 - fieldStatus.getCollectedFieldCount();
        Integer estimatedMinutes = remainingFields > 0 ? remainingFields * 2 : null;
        
        return TravelInfoStatusDto.builder()
                .sessionId(entity.getSessionId())
                .userId(entity.getUser().getId())
                .chatThreadId(entity.getChatThread() != null ? entity.getChatThread().getId() : null)
                .currentStep(entity.getCurrentStep())
                .isCompleted(entity.isCompleted())
                .completionPercentage(entity.getCompletionPercentage())
                .fieldStatus(fieldStatus)
                .collectedInfo(infoBuilder.build())
                .nextRequiredStep(entity.getNextRequiredStep())
                .lastQuestionAsked(entity.getLastQuestionAsked())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .estimatedMinutesRemaining(estimatedMinutes)
                .build();
    }
    
    /**
     * 간단한 요약 메시지 생성
     */
    public String generateSummaryMessage() {
        if (isCompleted) {
            return String.format("✅ 모든 정보가 수집되었습니다! %s로 %s 여행을 준비하겠습니다.", 
                    collectedInfo.getDestination(),
                    collectedInfo.getDurationDescription());
        } else {
            return String.format("📝 정보 수집 중... (%d%% 완료, %d개 항목 남음)", 
                    completionPercentage,
                    fieldStatus.getTotalFieldCount() - fieldStatus.getCollectedFieldCount());
        }
    }
}