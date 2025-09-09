package com.compass.domain.chat.dto;

import com.compass.domain.chat.entity.TravelInfoCollectionState;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 후속 질문 DTO
 * REQ-FOLLOW-002: 사용자에게 전달할 후속 질문 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpQuestionDto {
    
    /**
     * 세션 ID
     */
    private String sessionId;
    
    /**
     * 현재 수집 단계
     */
    private TravelInfoCollectionState.CollectionStep currentStep;
    
    /**
     * 주요 질문
     */
    private String primaryQuestion;
    
    /**
     * 보조 설명 또는 예시
     */
    private String helpText;
    
    /**
     * 예시 답변들
     */
    private List<String> exampleAnswers;
    
    /**
     * 빠른 선택 옵션들 (버튼으로 표시 가능)
     */
    private List<QuickOption> quickOptions;
    
    /**
     * 입력 타입 힌트 (text, date, number, select 등)
     */
    private String inputType;
    
    /**
     * 필수 여부
     */
    private boolean isRequired;
    
    /**
     * 건너뛸 수 있는지 여부
     */
    private boolean canSkip;
    
    /**
     * 수집 진행률 (0-100)
     */
    private int progressPercentage;
    
    /**
     * 남은 질문 수
     */
    private int remainingQuestions;
    
    /**
     * 이미 수집된 정보 요약
     */
    private Map<String, Object> collectedInfo;
    
    /**
     * 빠른 선택 옵션
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickOption {
        private String value;
        private String label;
        private String description;
        private String icon; // 옵션별 아이콘 (선택사항)
    }
    
    /**
     * 다양한 질문 타입을 위한 팩토리 메서드들
     */
    public static FollowUpQuestionDto createDestinationQuestion(String sessionId, int progress) {
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.DESTINATION)
                .primaryQuestion("어디로 여행을 가고 싶으신가요?")
                .helpText("도시나 국가 이름을 알려주세요. 예: 서울, 부산, 제주도, 도쿄, 파리")
                .exampleAnswers(List.of("제주도", "부산", "강릉", "경주", "전주"))
                .quickOptions(List.of(
                        QuickOption.builder()
                                .value("제주도")
                                .label("제주도")
                                .description("한국의 하와이, 자연과 휴양")
                                .build(),
                        QuickOption.builder()
                                .value("부산")
                                .label("부산")
                                .description("해변과 도시의 조화")
                                .build(),
                        QuickOption.builder()
                                .value("서울")
                                .label("서울")
                                .description("대한민국의 수도")
                                .build()
                ))
                .inputType("text")
                .isRequired(true)
                .canSkip(false)
                .progressPercentage(progress)
                .remainingQuestions(5)
                .build();
    }
    
    public static FollowUpQuestionDto createDateQuestion(String sessionId, int progress, Map<String, Object> collected) {
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.DATES)
                .primaryQuestion("언제 여행을 가실 예정인가요?")
                .helpText("출발 날짜와 도착 날짜를 알려주세요.")
                .exampleAnswers(List.of("12월 24일부터 26일까지", "다음주 금요일부터 일요일", "1월 1일부터 3일"))
                .inputType("date-range")
                .isRequired(true)
                .canSkip(false)
                .progressPercentage(progress)
                .remainingQuestions(4)
                .collectedInfo(collected)
                .build();
    }
    
    public static FollowUpQuestionDto createCompanionQuestion(String sessionId, int progress, Map<String, Object> collected) {
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.COMPANIONS)
                .primaryQuestion("누구와 함께 여행하시나요?")
                .helpText("동행자 정보를 알려주시면 맞춤형 추천을 해드릴 수 있어요.")
                .quickOptions(List.of(
                        QuickOption.builder()
                                .value("solo")
                                .label("혼자")
                                .description("나만의 자유로운 여행")
                                .icon("👤")
                                .build(),
                        QuickOption.builder()
                                .value("couple")
                                .label("연인/배우자")
                                .description("둘만의 로맨틱한 시간")
                                .icon("💑")
                                .build(),
                        QuickOption.builder()
                                .value("family")
                                .label("가족")
                                .description("온 가족이 함께하는 여행")
                                .icon("👨‍👩‍👧‍👦")
                                .build(),
                        QuickOption.builder()
                                .value("friends")
                                .label("친구들")
                                .description("친구들과의 즐거운 추억")
                                .icon("👥")
                                .build()
                ))
                .inputType("select")
                .isRequired(true)
                .canSkip(false)
                .progressPercentage(progress)
                .remainingQuestions(2)
                .collectedInfo(collected)
                .build();
    }
    
    public static FollowUpQuestionDto createBudgetQuestion(String sessionId, int progress, Map<String, Object> collected) {
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.BUDGET)
                .primaryQuestion("여행 예산은 어느 정도로 생각하고 계신가요?")
                .helpText("1인당 예산을 알려주시거나, 대략적인 수준을 선택해주세요.")
                .quickOptions(List.of(
                        QuickOption.builder()
                                .value("budget")
                                .label("알뜰하게")
                                .description("가성비 좋은 여행")
                                .icon("💰")
                                .build(),
                        QuickOption.builder()
                                .value("moderate")
                                .label("적당하게")
                                .description("편안하고 합리적인 여행")
                                .icon("💵")
                                .build(),
                        QuickOption.builder()
                                .value("luxury")
                                .label("럭셔리하게")
                                .description("프리미엄 경험")
                                .icon("💎")
                                .build()
                ))
                .exampleAnswers(List.of("1인당 50만원", "총 200만원", "럭셔리하게"))
                .inputType("mixed")
                .isRequired(true)
                .canSkip(true)
                .progressPercentage(progress)
                .remainingQuestions(1)
                .collectedInfo(collected)
                .build();
    }
}