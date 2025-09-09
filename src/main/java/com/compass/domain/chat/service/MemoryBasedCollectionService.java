package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.TemplateStatusDto;
import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.engine.QuestionFlowEngine;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.model.TravelInfoTemplate;
import com.compass.domain.chat.prompt.travel.TravelPlanningPrompt;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.repository.TravelInfoCollectionRepository;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메모리 기반 여행 정보 수집 서비스
 * DB 접근을 최소화하고 메모리/캐시를 활용한 효율적인 정보 수집
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryBasedCollectionService {
    
    private final UserRepository userRepository;
    private final ChatThreadRepository chatThreadRepository;
    private final TravelInfoCollectionRepository collectionRepository;
    private final QuestionFlowEngine flowEngine;
    private final FollowUpQuestionGenerator questionGenerator;
    private final NaturalLanguageParsingService parsingService;
    private final ChatModelService chatModelService;
    
    // 메모리 저장소 (세션별 템플릿 관리)
    private final Map<String, TravelInfoTemplate> templateStore = new ConcurrentHashMap<>();
    
    // 세션 만료 시간 (24시간)
    private static final long SESSION_EXPIRY_HOURS = 24;
    
    /**
     * 새로운 정보 수집 세션 시작 (메모리 기반)
     */
    @Transactional(readOnly = true)
    public TemplateStatusDto startCollection(Long userId, String chatThreadId, String initialMessage) {
        log.info("Starting memory-based collection for user: {}, thread: {}", userId, chatThreadId);
        
        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        // 기존 활성 템플릿 확인
        String existingSessionId = findActiveSession(userId);
        if (existingSessionId != null) {
            log.info("Reusing existing session: {}", existingSessionId);
            return processMessage(existingSessionId, initialMessage);
        }
        
        // 새 템플릿 생성
        String sessionId = generateSessionId();
        TravelInfoTemplate template = TravelInfoTemplate.builder()
                .sessionId(sessionId)
                .userId(userId)
                .chatThreadId(chatThreadId)
                .createdAt(LocalDate.now())
                .updatedAt(LocalDate.now())
                .currentStep("ORIGIN")
                .build();
        
        // 초기 메시지 처리
        if (initialMessage != null && !initialMessage.isEmpty()) {
            processInitialMessage(template, initialMessage);
        }
        
        // 메모리에 저장
        templateStore.put(sessionId, template);
        
        // 다음 질문 생성
        return generateTemplateStatus(template);
    }
    
    /**
     * 템플릿 정보 업데이트 (메모리 기반)
     */
    public TemplateStatusDto updateTemplate(String sessionId, String userResponse) {
        log.info("Updating template for session: {} with response: {}", sessionId, userResponse);
        
        // 템플릿 조회
        TravelInfoTemplate template = templateStore.get(sessionId);
        if (template == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }
        
        // 응답 처리
        processMessage(template, userResponse);
        
        // 업데이트 시간 갱신
        template.setUpdatedAt(LocalDate.now());
        template.setLastUserResponse(userResponse);
        
        // 상태 반환
        return generateTemplateStatus(template);
    }
    
    /**
     * 템플릿 상태 조회
     */
    public TemplateStatusDto getTemplateStatus(String sessionId) {
        TravelInfoTemplate template = templateStore.get(sessionId);
        if (template == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }
        
        return generateTemplateStatus(template);
    }
    
    /**
     * 템플릿을 DB에 저장 (선택적)
     */
    @Transactional
    public void saveTemplateToDatabase(String sessionId) {
        log.info("Saving template to database: {}", sessionId);
        
        TravelInfoTemplate template = templateStore.get(sessionId);
        if (template == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }
        
        // User 조회
        User user = userRepository.findById(template.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // ChatThread 조회 (선택적)
        ChatThread chatThread = null;
        if (template.getChatThreadId() != null) {
            chatThread = chatThreadRepository.findById(template.getChatThreadId())
                    .orElse(null);
        }
        
        // Entity로 변환하여 저장
        TravelInfoCollectionState state = TravelInfoCollectionState.builder()
                .user(user)
                .chatThread(chatThread)
                .sessionId(sessionId)
                .origin(template.getOrigin())
                .destination(template.getDestination())
                .startDate(template.getStartDate())
                .endDate(template.getEndDate())
                .durationNights(template.getDurationNights())
                .numberOfTravelers(template.getNumberOfTravelers())
                .companionType(template.getCompanionType())
                .budgetPerPerson(template.getBudgetPerPerson())
                .budgetCurrency(template.getBudgetCurrency())
                .budgetLevel(template.getBudgetLevel())
                .originCollected(template.getOrigin() != null)
                .destinationCollected(template.getDestination() != null)
                .datesCollected(template.getStartDate() != null && template.getEndDate() != null)
                .durationCollected(template.getDurationNights() != null)
                .companionsCollected(template.getNumberOfTravelers() != null)
                .budgetCollected(template.getBudgetPerPerson() != null || template.getBudgetLevel() != null)
                .isCompleted(template.isComplete())
                .completedAt(template.isComplete() ? LocalDateTime.now() : null)
                .build();
        
        collectionRepository.save(state);
        log.info("Template saved to database successfully");
    }
    
    /**
     * 여행 계획 생성
     */
    @Transactional
    public String generateTravelPlan(String sessionId) {
        log.info("Generating travel plan for session: {}", sessionId);
        
        TravelInfoTemplate template = templateStore.get(sessionId);
        if (template == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }
        
        if (!template.canGeneratePlan()) {
            throw new IllegalStateException("필수 정보가 부족하여 계획을 생성할 수 없습니다");
        }
        
        // DB 저장 (계획 생성 전)
        saveTemplateToDatabase(sessionId);
        
        // TravelPlanningPrompt 활용
        TravelPlanningPrompt prompt = new TravelPlanningPrompt();
        Map<String, Object> variables = template.toPromptVariables();
        
        // 프롬프트 생성
        String planPrompt = prompt.buildPrompt(variables);
        
        // LLM을 통한 계획 생성
        String travelPlan = chatModelService.generateResponse(planPrompt);
        
        // 템플릿 제거 (생성 완료)
        templateStore.remove(sessionId);
        
        return travelPlan;
    }
    
    /**
     * 세션 취소
     */
    public void cancelSession(String sessionId) {
        log.info("Cancelling session: {}", sessionId);
        templateStore.remove(sessionId);
    }
    
    /**
     * 초기 메시지 처리
     */
    private void processInitialMessage(TravelInfoTemplate template, String message) {
        try {
            // NLP 파싱
            Map<String, Object> parsedInfo = parsingService.parseNaturalLanguageRequest(message);
            
            // 정보 추출 및 템플릿 업데이트
            updateTemplateFromParsedInfo(template, parsedInfo);
        } catch (Exception e) {
            log.error("Failed to process initial message", e);
        }
    }
    
    /**
     * 메시지 처리 (템플릿 업데이트)
     */
    private TemplateStatusDto processMessage(String sessionId, String message) {
        TravelInfoTemplate template = templateStore.get(sessionId);
        if (template == null) {
            throw new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId);
        }
        
        processMessage(template, message);
        return generateTemplateStatus(template);
    }
    
    /**
     * 메시지 처리 (내부)
     */
    private void processMessage(TravelInfoTemplate template, String message) {
        try {
            // 현재 단계에 따른 처리
            String currentField = template.getNextRequiredField();
            
            if (currentField != null) {
                // NLP 파싱
                Map<String, Object> parsedInfo = parsingService.parseNaturalLanguageRequest(message);
                
                // 특정 필드 업데이트
                updateTemplateFieldFromResponse(template, currentField, message, parsedInfo);
            }
        } catch (Exception e) {
            log.error("Failed to process message", e);
        }
    }
    
    /**
     * 파싱된 정보로 템플릿 업데이트
     */
    private void updateTemplateFromParsedInfo(TravelInfoTemplate template, Map<String, Object> parsedInfo) {
        if (parsedInfo.containsKey("origin")) {
            template.setOrigin((String) parsedInfo.get("origin"));
        }
        if (parsedInfo.containsKey("destination")) {
            template.setDestination((String) parsedInfo.get("destination"));
        }
        if (parsedInfo.containsKey("startDate")) {
            template.setStartDate((LocalDate) parsedInfo.get("startDate"));
        }
        if (parsedInfo.containsKey("endDate")) {
            template.setEndDate((LocalDate) parsedInfo.get("endDate"));
        }
        if (parsedInfo.containsKey("numberOfTravelers") && parsedInfo.get("numberOfTravelers") != null) {
            template.setNumberOfTravelers(((Number) parsedInfo.get("numberOfTravelers")).intValue());
        }
        if (parsedInfo.containsKey("groupType")) {
            template.setCompanionType((String) parsedInfo.get("groupType"));
        }
        if (parsedInfo.containsKey("budget")) {
            String budget = (String) parsedInfo.get("budget");
            if (budget != null) {
                template.setBudgetLevel(budget);
            }
        }
    }
    
    /**
     * 특정 필드 업데이트
     */
    private void updateTemplateFieldFromResponse(TravelInfoTemplate template, String field, 
                                                  String response, Map<String, Object> parsedInfo) {
        switch (field) {
            case "origin":
                // 출발지 파싱
                if (parsedInfo.containsKey("origin")) {
                    template.setOrigin((String) parsedInfo.get("origin"));
                } else {
                    // 단순 텍스트로 처리
                    template.setOrigin(response.trim());
                }
                template.setCurrentStep("DESTINATION");
                break;
                
            case "destination":
                // 목적지 파싱
                if (parsedInfo.containsKey("destination")) {
                    template.setDestination((String) parsedInfo.get("destination"));
                } else {
                    template.setDestination(response.trim());
                }
                template.setCurrentStep("DATES");
                break;
                
            case "dates":
                // 날짜 파싱
                if (parsedInfo.containsKey("startDate") && parsedInfo.containsKey("endDate")) {
                    template.setStartDate((LocalDate) parsedInfo.get("startDate"));
                    template.setEndDate((LocalDate) parsedInfo.get("endDate"));
                } else {
                    // 텍스트에서 날짜 추출 시도
                    parseDatesFromText(template, response);
                }
                template.setCurrentStep("COMPANIONS");
                break;
                
            case "companions":
                // 동행자 정보 파싱
                if (parsedInfo.containsKey("numberOfTravelers")) {
                    Object travelers = parsedInfo.get("numberOfTravelers");
                    if (travelers != null) {
                        template.setNumberOfTravelers(((Number) travelers).intValue());
                    }
                }
                if (parsedInfo.containsKey("groupType")) {
                    template.setCompanionType((String) parsedInfo.get("groupType"));
                }
                template.setCurrentStep("BUDGET");
                break;
                
            case "budget":
                // 예산 정보 파싱
                if (parsedInfo.containsKey("budget")) {
                    String budgetLevel = (String) parsedInfo.get("budget");
                    template.setBudgetLevel(budgetLevel);
                }
                if (parsedInfo.containsKey("budgetAmount")) {
                    Object amount = parsedInfo.get("budgetAmount");
                    if (amount != null) {
                        template.setBudgetPerPerson(((Number) amount).intValue());
                    }
                }
                template.setCurrentStep("CONFIRMATION");
                break;
        }
    }
    
    /**
     * 텍스트에서 날짜 파싱 (간단한 구현)
     */
    private void parseDatesFromText(TravelInfoTemplate template, String text) {
        // TODO: 실제 날짜 파싱 로직 구현
        // 예: "3월 15일부터 17일까지" -> LocalDate 변환
        log.info("Parsing dates from text: {}", text);
    }
    
    /**
     * 템플릿 상태 DTO 생성
     */
    private TemplateStatusDto generateTemplateStatus(TravelInfoTemplate template) {
        // 다음 질문 결정
        String nextQuestion = null;
        String nextField = template.getNextRequiredField();
        
        if (nextField != null) {
            nextQuestion = generateQuestionForField(nextField);
        }
        
        return TemplateStatusDto.builder()
                .sessionId(template.getSessionId())
                .template(template)
                .nextQuestion(nextQuestion)
                .canGeneratePlan(template.canGeneratePlan())
                .completionPercentage(template.getCompletionPercentage())
                .missingFields(template.getMissingFields())
                .validationErrors(template.validate())
                .summary(template.getSummary())
                .build();
    }
    
    /**
     * 필드별 질문 생성
     */
    private String generateQuestionForField(String field) {
        return switch (field) {
            case "origin" -> "어디에서 출발하시나요? 🛫";
            case "destination" -> "어디로 여행을 가시나요? ✈️";
            case "dates" -> "언제 여행을 떠나실 예정인가요? 📅";
            case "companions" -> "누구와 함께 여행하시나요? 👥";
            case "budget" -> "예산은 어느 정도 생각하고 계신가요? 💰 (선택사항)";
            default -> null;
        };
    }
    
    /**
     * 세션 ID 생성
     */
    private String generateSessionId() {
        return "TIC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 활성 세션 찾기
     */
    private String findActiveSession(Long userId) {
        // 메모리에서 사용자의 활성 세션 찾기
        return templateStore.entrySet().stream()
                .filter(entry -> entry.getValue().getUserId().equals(userId))
                .filter(entry -> !isSessionExpired(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 세션 만료 확인
     */
    private boolean isSessionExpired(TravelInfoTemplate template) {
        if (template.getCreatedAt() == null) return false;
        
        LocalDate expiryDate = template.getCreatedAt().plusDays(1);
        return LocalDate.now().isAfter(expiryDate);
    }
    
    /**
     * 만료된 세션 정리 (스케줄러에서 호출)
     */
    public void cleanupExpiredSessions() {
        templateStore.entrySet().removeIf(entry -> isSessionExpired(entry.getValue()));
    }
}