package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 재질문 생성 서비스
 * REQ-FOLLOW-006: 파싱 실패시 다른 표현으로 재질문
 * 
 * 사용자 응답 파싱이 실패했을 때 더 명확한 형태의 재질문을 생성
 */
@Slf4j
@Service
public class ClarificationQuestionGenerator {
    
    private static final int MAX_RETRY_COUNT = 3;
    
    /**
     * 파싱 실패 시 재질문 생성
     * 재시도 횟수에 따라 점진적으로 더 구체적인 질문 생성
     */
    public FollowUpQuestionDto generateClarificationQuestion(
            TravelInfoCollectionState state, 
            String failedField,
            String originalResponse,
            int retryCount) {
        
        log.info("Generating clarification question for field: {}, retry: {}", 
                failedField, retryCount);
        
        if (retryCount >= MAX_RETRY_COUNT) {
            // 최대 재시도 횟수 초과 시 기본값 사용 권유
            return generateDefaultSuggestionQuestion(state, failedField);
        }
        
        return switch (failedField.toLowerCase()) {
            case "destination" -> generateDestinationClarification(state, originalResponse, retryCount);
            case "dates" -> generateDatesClarification(state, originalResponse, retryCount);
            case "companions" -> generateCompanionsClarification(state, originalResponse, retryCount);
            case "budget" -> generateBudgetClarification(state, originalResponse, retryCount);
            case "origin" -> generateOriginClarification(state, originalResponse, retryCount);
            default -> generateGenericClarification(state, failedField, retryCount);
        };
    }
    
    /**
     * 목적지 재질문 생성
     */
    private FollowUpQuestionDto generateDestinationClarification(
            TravelInfoCollectionState state, 
            String originalResponse, 
            int retryCount) {
        
        String sessionId = state.getSessionId();
        Map<String, Object> collectedInfo = state.toInfoMap();
        
        List<String> questions = List.of(
            "목적지를 알려주세요! 어느 도시로 가시나요? 📍",
            "여행 목적지를 다시 입력해주세요. (예: 서울, 부산, 제주)",
            "정확한 도시명을 입력해주세요!"
        );
        
        String primaryQuestion = retryCount < questions.size() ? 
            questions.get(retryCount) : 
            questions.get(questions.size() - 1);
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.DESTINATION)
                .primaryQuestion(primaryQuestion)
                .helpText("국내 주요 도시 중에서 선택하시거나 직접 입력해주세요")
                .exampleAnswers(List.of("서울", "부산", "제주", "강릉", "경주", "전주"))
                .quickOptions(generateDestinationQuickOptions())
                .inputType("text")
                .isRequired(true)
                .canSkip(false)
                .isClarification(true)
                .retryCount(retryCount)
                .originalResponse(originalResponse)
                .collectedInfo(collectedInfo)
                .build();
    }
    
    /**
     * 날짜 재질문 생성
     */
    private FollowUpQuestionDto generateDatesClarification(
            TravelInfoCollectionState state, 
            String originalResponse, 
            int retryCount) {
        
        String sessionId = state.getSessionId();
        Map<String, Object> collectedInfo = state.toInfoMap();
        
        List<String> questions = List.of(
            "여행 날짜를 알려주세요! 언제부터 언제까지 가시나요? 📅",
            "출발일과 도착일을 다시 입력해주세요. (예: 12월 25일 ~ 12월 27일)",
            "정확한 날짜를 입력해주세요!"
        );
        
        String primaryQuestion = retryCount < questions.size() ? 
            questions.get(retryCount) : 
            questions.get(questions.size() - 1);
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.DATES)
                .primaryQuestion(primaryQuestion)
                .helpText("여행 시작일과 종료일을 입력해주세요")
                .exampleAnswers(List.of(
                    "12월 25일 ~ 12월 27일",
                    "2024-12-25 ~ 2024-12-27",
                    "다음주 금요일부터 일요일까지",
                    "크리스마스 연휴"
                ))
                .inputType("text")
                .isRequired(true)
                .canSkip(false)
                .isClarification(true)
                .retryCount(retryCount)
                .originalResponse(originalResponse)
                .collectedInfo(collectedInfo)
                .build();
    }
    
    /**
     * 동행자 재질문 생성
     */
    private FollowUpQuestionDto generateCompanionsClarification(
            TravelInfoCollectionState state, 
            String originalResponse, 
            int retryCount) {
        
        String sessionId = state.getSessionId();
        Map<String, Object> collectedInfo = state.toInfoMap();
        
        List<String> questions = List.of(
            "누구와 함께 가시나요? 👥",
            "동행자를 다시 알려주세요. (혼자/연인/가족/친구)",
            "몇 명이 함께 가시는지 입력해주세요!"
        );
        
        String primaryQuestion = retryCount < questions.size() ? 
            questions.get(retryCount) : 
            questions.get(questions.size() - 1);
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.COMPANIONS)
                .primaryQuestion(primaryQuestion)
                .helpText("여행을 함께하는 사람들에 대해 알려주세요")
                .quickOptions(generateCompanionQuickOptions())
                .inputType("select")
                .isRequired(true)
                .canSkip(false)
                .isClarification(true)
                .retryCount(retryCount)
                .originalResponse(originalResponse)
                .collectedInfo(collectedInfo)
                .build();
    }
    
    /**
     * 예산 재질문 생성
     */
    private FollowUpQuestionDto generateBudgetClarification(
            TravelInfoCollectionState state, 
            String originalResponse, 
            int retryCount) {
        
        String sessionId = state.getSessionId();
        Map<String, Object> collectedInfo = state.toInfoMap();
        
        List<String> questions = List.of(
            "예산을 알려주세요! 💰",
            "예산 수준을 선택해주세요. (알뜰/적당/럭셔리)",
            "1인당 예산이나 전체 수준을 입력해주세요!"
        );
        
        String primaryQuestion = retryCount < questions.size() ? 
            questions.get(retryCount) : 
            questions.get(questions.size() - 1);
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.BUDGET)
                .primaryQuestion(primaryQuestion)
                .helpText("여행 예산 수준을 선택하거나 금액을 입력해주세요")
                .quickOptions(generateBudgetQuickOptions())
                .exampleAnswers(List.of(
                    "1인당 50만원",
                    "알뜰하게",
                    "적당한 수준",
                    "럭셔리"
                ))
                .inputType("select")
                .isRequired(true)
                .canSkip(false)
                .isClarification(true)
                .retryCount(retryCount)
                .originalResponse(originalResponse)
                .collectedInfo(collectedInfo)
                .build();
    }
    
    /**
     * 출발지 재질문 생성
     */
    private FollowUpQuestionDto generateOriginClarification(
            TravelInfoCollectionState state, 
            String originalResponse, 
            int retryCount) {
        
        String sessionId = state.getSessionId();
        Map<String, Object> collectedInfo = state.toInfoMap();
        
        return FollowUpQuestionDto.builder()
                .sessionId(sessionId)
                .currentStep(TravelInfoCollectionState.CollectionStep.ORIGIN)
                .primaryQuestion("출발지를 알려주세요! 어느 도시에서 출발하시나요? 🛫")
                .helpText("출발 도시나 공항을 선택해주세요")
                .quickOptions(generateOriginQuickOptions())
                .inputType("text")
                .isRequired(true)
                .canSkip(false)
                .isClarification(true)
                .retryCount(retryCount)
                .originalResponse(originalResponse)
                .collectedInfo(collectedInfo)
                .build();
    }
    
    /**
     * 일반적인 재질문 생성
     */
    private FollowUpQuestionDto generateGenericClarification(
            TravelInfoCollectionState state, 
            String fieldName, 
            int retryCount) {
        
        return FollowUpQuestionDto.builder()
                .sessionId(state.getSessionId())
                .primaryQuestion(String.format(
                    "%s 정보를 알려주세요!", 
                    fieldName))
                .helpText("명확하고 구체적으로 입력해주세요")
                .inputType("text")
                .isRequired(true)
                .canSkip(false)
                .isClarification(true)
                .retryCount(retryCount)
                .collectedInfo(state.toInfoMap())
                .build();
    }
    
    /**
     * 최대 재시도 초과 시 기본값 제안 질문
     */
    private FollowUpQuestionDto generateDefaultSuggestionQuestion(
            TravelInfoCollectionState state, 
            String failedField) {
        
        String defaultValue = getDefaultValueForField(failedField);
        
        return FollowUpQuestionDto.builder()
                .sessionId(state.getSessionId())
                .primaryQuestion(String.format(
                    "기본값 '%s'로 진행할까요? 아니면 다시 입력하시겠어요?", 
                    defaultValue))
                .helpText("기본값으로 진행하거나 다시 입력해주세요")
                .quickOptions(List.of(
                    FollowUpQuestionDto.QuickOption.builder()
                        .value(defaultValue)
                        .label("기본값 사용")
                        .icon("✅")
                        .build(),
                    FollowUpQuestionDto.QuickOption.builder()
                        .value("retry")
                        .label("다시 입력")
                        .icon("🔄")
                        .build()
                ))
                .inputType("select")
                .isRequired(true)
                .canSkip(true)
                .isClarification(true)
                .retryCount(MAX_RETRY_COUNT)
                .collectedInfo(state.toInfoMap())
                .build();
    }
    
    /**
     * 필드별 기본값
     */
    private String getDefaultValueForField(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "destination" -> "서울";
            case "origin" -> "서울";
            case "dates" -> "미정";
            case "companions" -> "혼자";
            case "budget" -> "적당한 수준";
            default -> "미정";
        };
    }
    
    // Quick Options 생성 메서드들
    private List<FollowUpQuestionDto.QuickOption> generateDestinationQuickOptions() {
        return List.of(
            FollowUpQuestionDto.QuickOption.builder()
                .value("서울").label("서울").icon("🏙️").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("부산").label("부산").icon("🌊").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("제주").label("제주").icon("🏝️").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("강릉").label("강릉").icon("🏖️").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("경주").label("경주").icon("🏛️").build()
        );
    }
    
    private List<FollowUpQuestionDto.QuickOption> generateOriginQuickOptions() {
        return List.of(
            FollowUpQuestionDto.QuickOption.builder()
                .value("서울").label("서울/수도권").icon("🏙️").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("부산").label("부산/경남").icon("🌊").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("대구").label("대구/경북").icon("🏛️").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("광주").label("광주/전남").icon("🌻").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("대전").label("대전/충청").icon("🏢").build()
        );
    }
    
    private List<FollowUpQuestionDto.QuickOption> generateCompanionQuickOptions() {
        return List.of(
            FollowUpQuestionDto.QuickOption.builder()
                .value("solo").label("혼자").icon("👤").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("couple").label("연인/배우자").icon("💑").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("family").label("가족").icon("👨‍👩‍👧‍👦").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("friends").label("친구").icon("👥").build()
        );
    }
    
    private List<FollowUpQuestionDto.QuickOption> generateBudgetQuickOptions() {
        return List.of(
            FollowUpQuestionDto.QuickOption.builder()
                .value("budget").label("알뜰").description("가성비 중시")
                .icon("💰").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("moderate").label("적당").description("균형잡힌")
                .icon("💵").build(),
            FollowUpQuestionDto.QuickOption.builder()
                .value("luxury").label("럭셔리").description("편안함 중시")
                .icon("💎").build()
        );
    }
}