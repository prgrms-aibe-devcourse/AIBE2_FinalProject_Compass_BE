package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 후속 질문 생성 서비스
 * REQ-FOLLOW-002: 컨텍스트 기반 동적 질문 생성
 */
@Slf4j
@Service
public class FollowUpQuestionGenerator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일");
    
    /**
     * 현재 상태에 따른 다음 질문 생성
     */
    public FollowUpQuestionDto generateNextQuestion(TravelInfoCollectionState state) {
        TravelInfoCollectionState.CollectionStep nextStep = state.getNextRequiredStep();
        Map<String, Object> collectedInfo = state.toInfoMap();
        int progress = state.getCompletionPercentage();
        
        log.info("Generating follow-up question for step: {}, progress: {}%", nextStep, progress);
        
        return switch (nextStep) {
            case ORIGIN -> generateOriginQuestion(state.getSessionId(), progress, collectedInfo);
            case DESTINATION -> generateDestinationQuestion(state.getSessionId(), progress, collectedInfo);
            case DATES -> generateDateQuestion(state.getSessionId(), progress, collectedInfo);
            case DURATION -> generateDurationQuestion(state.getSessionId(), progress, collectedInfo);
            case COMPANIONS -> generateCompanionQuestion(state.getSessionId(), progress, collectedInfo);
            case BUDGET -> generateBudgetQuestion(state.getSessionId(), progress, collectedInfo);
            case CONFIRMATION -> generateConfirmationQuestion(state.getSessionId(), collectedInfo);
            default -> generateDefaultQuestion(state.getSessionId());
        };
    }
    
    /**
     * 출발지 질문 생성
     */
    private FollowUpQuestionDto generateOriginQuestion(String sessionId, int progress, Map<String, Object> collected) {
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.ORIGIN)
                .primaryQuestion("어디에서 출발하시나요? 🛫")
                .helpText("출발 도시나 공항 이름을 알려주세요.")
                .exampleAnswers(List.of(
                        "서울",
                        "인천공항",
                        "부산",
                        "김포공항"
                ))
                .quickOptions(List.of(
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("서울")
                                .label("서울")
                                .description("수도권")
                                .icon("🏙️")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("부산")
                                .label("부산")
                                .description("부산/경남")
                                .icon("🌊")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("대구")
                                .label("대구")
                                .description("대구/경북")
                                .icon("🏛️")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("광주")
                                .label("광주")
                                .description("광주/전남")
                                .icon("🌻")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("대전")
                                .label("대전")
                                .description("대전/충청")
                                .icon("🏢")
                                .build()
                ))
                .inputType("text")
                .isRequired(true)
                .canSkip(false)
                .progressPercentage(progress)
                .remainingQuestions(6 - (progress * 6 / 100))
                .collectedInfo(collected)
                .build();
    }
    
    /**
     * 목적지 질문 생성
     */
    private FollowUpQuestionDto generateDestinationQuestion(String sessionId, int progress, Map<String, Object> collected) {
        List<FollowUpQuestionDto.QuickOption> popularDestinations = getPopularDestinations();
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.DESTINATION)
                .primaryQuestion("어디로 여행을 떠나고 싶으신가요? 🗺️")
                .helpText("도시나 국가 이름을 입력해주세요. 국내외 모두 가능합니다!")
                .exampleAnswers(List.of(
                        "제주도로 가고 싶어요",
                        "부산 해운대",
                        "일본 도쿄",
                        "유럽 여행"
                ))
                .quickOptions(popularDestinations)
                .inputType("text")
                .isRequired(true)
                .canSkip(false)
                .progressPercentage(progress)
                .remainingQuestions(6 - (progress * 6 / 100))
                .collectedInfo(collected)
                .build();
    }
    
    /**
     * 날짜 질문 생성 (목적지 정보 활용)
     */
    private FollowUpQuestionDto generateDateQuestion(String sessionId, int progress, Map<String, Object> collected) {
        String destination = (String) collected.get("destination");
        String contextualQuestion = destination != null ?
                String.format("%s 여행은 언제 가실 예정인가요? 📅", destination) :
                "언제 여행을 떠나실 예정인가요? 📅";
        
        // 다음 주말, 다음 달 등 빠른 선택 옵션 생성
        List<FollowUpQuestionDto.QuickOption> dateOptions = generateQuickDateOptions();
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.DATES)
                .primaryQuestion(contextualQuestion)
                .helpText("출발일과 도착일을 알려주세요. 대략적인 시기만 알려주셔도 됩니다.")
                .exampleAnswers(List.of(
                        "12월 24일부터 26일까지",
                        "다음 주 금요일부터 일요일",
                        "1월 첫째 주",
                        "크리스마스 연휴"
                ))
                .quickOptions(dateOptions)
                .inputType("date-range")
                .isRequired(true)
                .canSkip(false)
                .progressPercentage(progress)
                .remainingQuestions(5 - (progress * 6 / 100))
                .collectedInfo(collected)
                .build();
    }
    
    /**
     * 기간 질문 생성 (날짜 정보가 있으면 자동 계산)
     */
    private FollowUpQuestionDto generateDurationQuestion(String sessionId, int progress, Map<String, Object> collected) {
        // 날짜가 이미 수집되었으면 기간 자동 계산 가능
        LocalDate startDate = (LocalDate) collected.get("startDate");
        LocalDate endDate = (LocalDate) collected.get("endDate");
        
        String question = "여행 기간은 어느 정도로 계획하고 계신가요? ⏱️";
        String helpText = "당일치기부터 장기 여행까지 모두 가능합니다.";
        
        if (startDate != null && endDate != null) {
            long nights = endDate.toEpochDay() - startDate.toEpochDay();
            question = String.format("선택하신 날짜 기준 %d박 %d일 여행이 맞나요?", nights, nights + 1);
            helpText = "날짜를 기준으로 계산했습니다. 수정이 필요하시면 알려주세요.";
        }
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.DURATION)
                .primaryQuestion(question)
                .helpText(helpText)
                .quickOptions(List.of(
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("0")
                                .label("당일치기")
                                .description("하루 알차게")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("1")
                                .label("1박 2일")
                                .description("짧고 굵게")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("2")
                                .label("2박 3일")
                                .description("적당한 여행")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("3")
                                .label("3박 4일")
                                .description("여유로운 여행")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("4+")
                                .label("4박 이상")
                                .description("장기 여행")
                                .build()
                ))
                .inputType("select")
                .isRequired(true)
                .canSkip(startDate != null && endDate != null) // 날짜로 계산 가능하면 건너뛸 수 있음
                .progressPercentage(progress)
                .remainingQuestions(4 - (progress * 6 / 100))
                .collectedInfo(collected)
                .build();
    }
    
    /**
     * 동행자 질문 생성 (목적지와 기간 정보 활용)
     */
    private FollowUpQuestionDto generateCompanionQuestion(String sessionId, int progress, Map<String, Object> collected) {
        String destination = (String) collected.get("destination");
        Integer nights = (Integer) collected.get("durationNights");
        
        String contextualQuestion = "누구와 함께 여행하시나요? 👥";
        if (destination != null && nights != null) {
            contextualQuestion = String.format("%s %d박 %d일, 누구와 함께 가시나요? 👥", 
                    destination, nights, nights + 1);
        }
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.COMPANIONS)
                .primaryQuestion(contextualQuestion)
                .helpText("동행자 정보를 알려주시면 더 적합한 코스를 추천해드릴 수 있어요.")
                .quickOptions(List.of(
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("solo")
                                .label("혼자")
                                .description("나만의 자유로운 여행")
                                .icon("🚶")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("couple")
                                .label("연인/배우자")
                                .description("둘만의 로맨틱한 시간")
                                .icon("💑")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("family")
                                .label("가족")
                                .description("온 가족이 함께")
                                .icon("👨‍👩‍👧‍👦")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("friends")
                                .label("친구들")
                                .description("친구들과 즐거운 시간")
                                .icon("👥")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("business")
                                .label("비즈니스/출장")
                                .description("업무 관련 여행")
                                .icon("💼")
                                .build()
                ))
                .exampleAnswers(List.of(
                        "친구 3명이랑 같이 가요",
                        "가족 여행이에요 (부모님, 아이 2명)",
                        "신혼여행입니다",
                        "혼자 갑니다"
                ))
                .inputType("select")
                .isRequired(true)
                .canSkip(false)
                .progressPercentage(progress)
                .remainingQuestions(3 - (progress * 6 / 100))
                .collectedInfo(collected)
                .build();
    }
    
    /**
     * 예산 질문 생성 (수집된 모든 정보 활용)
     */
    private FollowUpQuestionDto generateBudgetQuestion(String sessionId, int progress, Map<String, Object> collected) {
        String destination = (String) collected.get("destination");
        Integer nights = (Integer) collected.get("durationNights");
        Integer travelers = (Integer) collected.get("numberOfTravelers");
        
        StringBuilder contextBuilder = new StringBuilder("여행 예산은 어느 정도로 생각하고 계신가요? 💰");
        
        if (destination != null && nights != null && travelers != null) {
            contextBuilder = new StringBuilder();
            contextBuilder.append(String.format("%s %d박 %d일", destination, nights, nights + 1));
            if (travelers > 1) {
                contextBuilder.append(String.format(" (%d명)", travelers));
            }
            contextBuilder.append(" 여행 예산은 어떻게 되시나요? 💰");
        }
        
        // 목적지별 예산 추천
        List<String> budgetExamples = generateBudgetExamples(destination, nights, travelers);
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.BUDGET)
                .primaryQuestion(contextBuilder.toString())
                .helpText("구체적인 금액이나 대략적인 수준을 알려주세요. 더 정확한 추천이 가능합니다.")
                .quickOptions(List.of(
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("budget")
                                .label("알뜰하게")
                                .description("가성비 중심")
                                .icon("💰")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("moderate")
                                .label("적당하게")
                                .description("편안하고 합리적인")
                                .icon("💵")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("luxury")
                                .label("여유있게")
                                .description("프리미엄 경험")
                                .icon("💎")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("no-limit")
                                .label("제한 없음")
                                .description("최고의 경험 추구")
                                .icon("🏆")
                                .build()
                ))
                .exampleAnswers(budgetExamples)
                .inputType("mixed")
                .isRequired(false)
                .canSkip(true)
                .progressPercentage(progress)
                .remainingQuestions(1)
                .collectedInfo(collected)
                .build();
    }
    
    /**
     * 확인 질문 생성
     */
    private FollowUpQuestionDto generateConfirmationQuestion(String sessionId, Map<String, Object> collected) {
        StringBuilder summary = new StringBuilder("수집된 정보를 확인해주세요:\n\n");
        
        if (collected.containsKey("origin")) {
            summary.append("🛫 출발지: ").append(collected.get("origin")).append("\n");
        }
        if (collected.containsKey("destination")) {
            summary.append("📍 목적지: ").append(collected.get("destination")).append("\n");
        }
        if (collected.containsKey("startDate") && collected.containsKey("endDate")) {
            summary.append("📅 날짜: ")
                    .append(((LocalDate) collected.get("startDate")).format(DATE_FORMATTER))
                    .append(" ~ ")
                    .append(((LocalDate) collected.get("endDate")).format(DATE_FORMATTER))
                    .append("\n");
        }
        if (collected.containsKey("durationNights")) {
            Integer nights = (Integer) collected.get("durationNights");
            summary.append("⏱️ 기간: ").append(nights).append("박 ").append(nights + 1).append("일\n");
        }
        if (collected.containsKey("numberOfTravelers") && collected.containsKey("companionType")) {
            summary.append("👥 동행: ").append(getCompanionLabel(
                    (String) collected.get("companionType"),
                    (Integer) collected.get("numberOfTravelers")
            )).append("\n");
        }
        if (collected.containsKey("budgetLevel")) {
            summary.append("💰 예산: ").append(getBudgetLabel((String) collected.get("budgetLevel")));
            if (collected.containsKey("budgetPerPerson")) {
                summary.append(" (1인당 ").append(collected.get("budgetPerPerson")).append("원)");
            }
            summary.append("\n");
        }
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.CONFIRMATION)
                .primaryQuestion("입력하신 정보가 맞나요? 여행 계획을 시작할까요? ✨")
                .helpText(summary.toString())
                .quickOptions(List.of(
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("confirm")
                                .label("네, 맞아요! 시작해주세요")
                                .description("여행 계획 생성 시작")
                                .icon("✅")
                                .build(),
                        FollowUpQuestionDto.QuickOption.builder()
                                .value("modify")
                                .label("수정할게요")
                                .description("정보 수정하기")
                                .icon("✏️")
                                .build()
                ))
                .inputType("confirm")
                .isRequired(true)
                .canSkip(false)
                .progressPercentage(100)
                .remainingQuestions(0)
                .collectedInfo(collected)
                .build();
    }
    
    /**
     * 기본 질문 생성
     */
    private FollowUpQuestionDto generateDefaultQuestion(String sessionId) {
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .primaryQuestion("여행 계획을 도와드릴까요?")
                .helpText("어떤 여행을 원하시는지 자유롭게 말씀해주세요.")
                .inputType("text")
                .isRequired(false)
                .canSkip(true)
                .progressPercentage(0)
                .remainingQuestions(5)
                .build();
    }
    
    /**
     * 인기 목적지 옵션 생성
     */
    private List<FollowUpQuestionDto.QuickOption> getPopularDestinations() {
        return List.of(
                FollowUpQuestionDto.QuickOption.builder()
                        .value("제주도")
                        .label("제주도")
                        .description("한국의 하와이")
                        .icon("🏝️")
                        .build(),
                FollowUpQuestionDto.QuickOption.builder()
                        .value("부산")
                        .label("부산")
                        .description("바다와 도시의 조화")
                        .icon("🌊")
                        .build(),
                FollowUpQuestionDto.QuickOption.builder()
                        .value("강릉")
                        .label("강릉")
                        .description("동해안의 매력")
                        .icon("☕")
                        .build(),
                FollowUpQuestionDto.QuickOption.builder()
                        .value("경주")
                        .label("경주")
                        .description("천년 고도의 역사")
                        .icon("🏛️")
                        .build(),
                FollowUpQuestionDto.QuickOption.builder()
                        .value("서울")
                        .label("서울")
                        .description("대한민국의 수도")
                        .icon("🏙️")
                        .build()
        );
    }
    
    /**
     * 빠른 날짜 선택 옵션 생성
     */
    private List<FollowUpQuestionDto.QuickOption> generateQuickDateOptions() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeekend = today.plusDays((6 - today.getDayOfWeek().getValue()) % 7);
        if (nextWeekend.isBefore(today.plusDays(3))) {
            nextWeekend = nextWeekend.plusWeeks(1);
        }
        
        return List.of(
                FollowUpQuestionDto.QuickOption.builder()
                        .value("next-weekend")
                        .label("다음 주말")
                        .description(nextWeekend.format(DATE_FORMATTER) + " 주말")
                        .build(),
                FollowUpQuestionDto.QuickOption.builder()
                        .value("next-month")
                        .label("다음 달")
                        .description(today.plusMonths(1).getMonth().toString())
                        .build(),
                FollowUpQuestionDto.QuickOption.builder()
                        .value("holiday")
                        .label("다음 연휴")
                        .description("가까운 공휴일")
                        .build()
        );
    }
    
    /**
     * 예산 예시 생성
     */
    private List<String> generateBudgetExamples(String destination, Integer nights, Integer travelers) {
        List<String> examples = new ArrayList<>();
        
        if (nights != null) {
            int baseBudget = nights == 0 ? 10 : nights * 30; // 기본 예산 계산
            examples.add(String.format("1인당 %d만원", baseBudget));
            examples.add(String.format("1인당 %d만원", baseBudget * 2));
        }
        
        if (travelers != null && travelers > 1) {
            examples.add(String.format("총 %d만원 (전체)", travelers * 50));
        }
        
        examples.add("가성비 위주로");
        examples.add("특별한 날이라 넉넉하게");
        
        return examples.subList(0, Math.min(4, examples.size()));
    }
    
    /**
     * 동행자 라벨 생성
     */
    private String getCompanionLabel(String type, Integer count) {
        String typeLabel = switch (type) {
            case "solo" -> "혼자";
            case "couple" -> "연인/배우자";
            case "family" -> "가족";
            case "friends" -> "친구";
            case "business" -> "비즈니스";
            default -> type;
        };
        
        if (count != null && count > 1) {
            return String.format("%s %d명", typeLabel, count);
        }
        return typeLabel;
    }
    
    /**
     * 예산 라벨 생성
     */
    private String getBudgetLabel(String level) {
        return switch (level) {
            case "budget" -> "알뜰한 여행";
            case "moderate" -> "적당한 수준";
            case "luxury" -> "럭셔리 여행";
            case "no-limit" -> "제한 없음";
            default -> level;
        };
    }
}