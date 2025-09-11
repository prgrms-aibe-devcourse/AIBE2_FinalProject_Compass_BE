package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.TravelInfoStatusDto;
import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.dto.ValidationResult;
import com.compass.domain.chat.engine.QuestionFlowEngine;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.repository.TravelInfoCollectionRepository;
import com.compass.domain.chat.util.TravelInfoValidator;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.compass.domain.chat.constant.TravelConstants;
import com.compass.domain.chat.util.TravelParsingUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 여행 정보 수집 서비스
 * REQ-FOLLOW-002: 필수 여행 정보를 체계적으로 수집하는 핵심 서비스
 */
@Slf4j
@Service
public class TravelInfoCollectionService {
    
    private final TravelInfoCollectionRepository collectionRepository;
    private final UserRepository userRepository;
    private final ChatThreadRepository chatThreadRepository;
    private final FollowUpQuestionGenerator questionGenerator;
    private final NaturalLanguageParsingService parsingService;
    private final QuestionFlowEngine flowEngine;
    private final ObjectMapper objectMapper;
    private SessionManagementService sessionService;
    private TravelInfoValidator validator;
    
    @Autowired
    public TravelInfoCollectionService(
            TravelInfoCollectionRepository collectionRepository,
            UserRepository userRepository,
            ChatThreadRepository chatThreadRepository,
            FollowUpQuestionGenerator questionGenerator,
            NaturalLanguageParsingService parsingService,
            QuestionFlowEngine flowEngine,
            ObjectMapper objectMapper,
            @Autowired(required = false) SessionManagementService sessionService,
            @Autowired(required = false) TravelInfoValidator validator) {
        this.collectionRepository = collectionRepository;
        this.userRepository = userRepository;
        this.chatThreadRepository = chatThreadRepository;
        this.questionGenerator = questionGenerator;
        this.parsingService = parsingService;
        this.flowEngine = flowEngine;
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
        this.validator = validator;
    }
    
    /**
     * 새로운 정보 수집 세션 시작
     */
    @Transactional
    public FollowUpQuestionDto startInfoCollection(Long userId, String chatThreadId, String initialMessage) {
        log.info("Starting info collection for user: {}, thread: {}", userId, chatThreadId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        ChatThread chatThread = null;
        if (chatThreadId != null) {
            chatThread = chatThreadRepository.findById(chatThreadId)
                    .orElse(null);
        }
        
        // Redis 캐시에서 먼저 확인 (REQ-FOLLOW-004)
        String tempSessionId = user.getId() + "_temp";
        Optional<TravelInfoCollectionState> cachedState = Optional.empty();
        if (sessionService != null) {
            TravelInfoCollectionState tempState = sessionService.loadSession(tempSessionId);
            if (tempState != null) {
                cachedState = Optional.of(tempState);
            }
        }
        
        // 기존 미완료 세션 확인
        Optional<TravelInfoCollectionState> existingState = cachedState.isPresent() ? 
                cachedState : collectionRepository.findFirstByUserAndIsCompletedFalseOrderByCreatedAtDesc(user);
        
        if (existingState.isPresent()) {
            // 타임아웃 체크
            if (isSessionExpired(existingState.get())) {
                existingState.get().setCompleted(true);
                collectionRepository.save(existingState.get());
            } else {
                // 기존 세션 계속 사용
                return continueCollection(existingState.get(), initialMessage);
            }
        }
        
        // 새 세션 생성
        TravelInfoCollectionState newState = TravelInfoCollectionState.builder()
                .user(user)
                .chatThread(chatThread)
                .sessionId(generateSessionId())
                .currentStep(TravelInfoCollectionState.CollectionStep.INITIAL)
                .isCompleted(false)
                .destinationCollected(false)
                .datesCollected(false)
                .durationCollected(false)
                .companionsCollected(false)
                .budgetCollected(false)
                .build();
        
        // 초기 메시지에서 정보 추출 시도
        if (initialMessage != null && !initialMessage.isEmpty()) {
            extractAndUpdateInfo(newState, initialMessage);
        }
        
        newState = collectionRepository.save(newState);
        
        // Redis에 캐싱 (REQ-FOLLOW-004: 30분 TTL)
        if (sessionService != null) {
            sessionService.saveSession(newState.getSessionId(), newState);
        }
        
        // 다음 질문 생성
        return flowEngine.generateNextQuestion(newState);
    }
    
    /**
     * 후속 응답 처리
     */
    @Transactional
    public FollowUpQuestionDto processFollowUpResponse(String sessionId, String userResponse) {
        log.info("Processing follow-up response for session: {}", sessionId);
        
        // Redis 캐시에서 먼저 조회 (REQ-FOLLOW-004)
        TravelInfoCollectionState state = null;
        if (sessionService != null) {
            state = sessionService.loadSession(sessionId);
        }
        
        if (state == null) {
            state = collectionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        }
        
        if (state.isCompleted()) {
            throw new IllegalStateException("이미 완료된 수집 세션입니다");
        }
        
        // 플로우 엔진을 통해 응답 처리
        state = flowEngine.processResponse(state, userResponse);
        
        // 저장
        state.setLastQuestionAsked(userResponse);
        state = collectionRepository.save(state);
        
        // Redis 캐시 업데이트 (REQ-FOLLOW-004)
        if (sessionService != null) {
            sessionService.saveSession(state.getSessionId(), state);
        }
        
        // 플로우 완료 여부 확인
        if (flowEngine.isFlowComplete(state)) {
            state.setCurrentStep(TravelInfoCollectionState.CollectionStep.CONFIRMATION);
            return flowEngine.generateNextQuestion(state);
        }
        
        // 플로우 엔진을 통해 다음 질문 생성
        return flowEngine.generateNextQuestion(state);
    }
    
    /**
     * 수집 완료 처리
     * REQ-FOLLOW-005: 완료 전 필수 필드 검증
     */
    @Transactional
    public TripPlanningRequest completeCollection(String sessionId) {
        log.info("Completing collection for session: {}", sessionId);
        
        TravelInfoCollectionState state = collectionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        
        // REQ-FOLLOW-005: 검증 수행
        if (validator != null) {
            ValidationResult validationResult = validator.validate(state, ValidationResult.ValidationLevel.STRICT);
            
            if (!validationResult.isValid()) {
                log.warn("Collection cannot be completed due to validation errors: {}", 
                        validationResult.getUserFriendlyMessage());
                throw new IllegalStateException("검증 실패: " + validationResult.getUserFriendlyMessage());
            }
        } else {
            // Fallback: 기본 검증
            if (!state.isAllRequiredInfoCollected()) {
                String missingFields = state.getMissingFieldsMessage();
                log.warn("Collection cannot be completed - missing fields: {}", missingFields);
                throw new IllegalStateException(missingFields);
            }
            
            if (!state.hasValidData()) {
                throw new IllegalStateException("입력된 정보에 유효하지 않은 데이터가 있습니다");
            }
        }
        
        // 완료 처리
        state.markAsCompleted();
        collectionRepository.save(state);
        
        // Redis 캐시 삭제 (완료 시)
        if (sessionService != null) {
            sessionService.deleteSession(sessionId);
        }
        
        // TripPlanningRequest로 변환
        return convertToTripPlanningRequest(state);
    }
    
    /**
     * 수집 상태 조회
     */
    @Transactional(readOnly = true)
    public TravelInfoStatusDto getCollectionStatus(String sessionId) {
        TravelInfoCollectionState state = collectionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        
        return TravelInfoStatusDto.fromEntity(state);
    }
    
    /**
     * 사용자의 현재 수집 상태 조회
     */
    @Transactional(readOnly = true)
    public Optional<TravelInfoStatusDto> getCurrentUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        return collectionRepository.findFirstByUserAndIsCompletedFalseOrderByCreatedAtDesc(user)
                .map(TravelInfoStatusDto::fromEntity);
    }
    
    /**
     * 특정 정보 업데이트
     */
    @Transactional
    public TravelInfoStatusDto updateSpecificInfo(String sessionId, String fieldName, Object value) {
        log.info("Updating specific info - session: {}, field: {}, value: {}", sessionId, fieldName, value);
        
        TravelInfoCollectionState state = collectionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        
        switch (fieldName.toLowerCase()) {
            case "destination" -> {
                state.setDestination((String) value);
                state.setDestinationCollected(true);
            }
            case "startdate" -> {
                state.setStartDate(LocalDate.parse((String) value));
                if (state.getEndDate() != null) {
                    state.setDatesCollected(true);
                }
            }
            case "enddate" -> {
                state.setEndDate(LocalDate.parse((String) value));
                if (state.getStartDate() != null) {
                    state.setDatesCollected(true);
                    calculateDuration(state);
                }
            }
            case "duration" -> {
                state.setDurationNights((Integer) value);
                state.setDurationCollected(true);
            }
            case "companions" -> {
                Map<String, Object> companionInfo = (Map<String, Object>) value;
                state.setNumberOfTravelers((Integer) companionInfo.get("count"));
                state.setCompanionType((String) companionInfo.get("type"));
                state.setCompanionsCollected(true);
            }
            case "budget" -> {
                Map<String, Object> budgetInfo = (Map<String, Object>) value;
                state.setBudgetPerPerson((Integer) budgetInfo.get("amount"));
                state.setBudgetLevel((String) budgetInfo.get("level"));
                state.setBudgetCurrency((String) budgetInfo.getOrDefault("currency", "KRW"));
                state.setBudgetCollected(true);
            }
            default -> log.warn("Unknown field name: {}", fieldName);
        }
        
        state = collectionRepository.save(state);
        return TravelInfoStatusDto.fromEntity(state);
    }
    
    /**
     * 세션 취소
     */
    @Transactional
    public void cancelCollection(String sessionId) {
        log.info("Cancelling collection session: {}", sessionId);
        
        TravelInfoCollectionState state = collectionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        
        state.setCompleted(true);
        state.setCompletedAt(LocalDateTime.now());
        collectionRepository.save(state);
        
        // Redis 캐시 삭제 (취소 시)
        if (sessionService != null) {
            sessionService.deleteSession(sessionId);
        }
    }
    
    /**
     * 현재 수집 상태 검증
     * REQ-FOLLOW-005: 실시간 검증 피드백 제공
     */
    @Transactional(readOnly = true)
    public ValidationResult validateCurrentState(String sessionId) {
        log.info("Validating current state for session: {}", sessionId);
        
        TravelInfoCollectionState state = collectionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        
        if (validator != null) {
            return validator.validate(state, ValidationResult.ValidationLevel.STANDARD);
        }
        
        // Fallback: 기본 검증 결과
        ValidationResult result = ValidationResult.builder()
                .valid(state.isAllRequiredInfoCollected() && state.hasValidData())
                .completionPercentage(state.getCompletionPercentage())
                .validationLevel(ValidationResult.ValidationLevel.BASIC)
                .build();
        
        if (!result.isValid()) {
            state.getIncompleteFields().forEach(field -> 
                result.addIncompleteField(field)
            );
            
            if (!state.hasValidData()) {
                result.addFieldError("data", "입력된 정보에 유효하지 않은 데이터가 있습니다");
            }
        }
        
        return result;
    }
    
    // === Private Helper Methods ===
    
    /**
     * 기존 세션 계속
     */
    private FollowUpQuestionDto continueCollection(TravelInfoCollectionState state, String message) {
        if (message != null && !message.isEmpty()) {
            extractAndUpdateInfo(state, message);
            collectionRepository.save(state);
        }
        
        return flowEngine.generateNextQuestion(state);
    }
    
    /**
     * 메시지에서 정보 추출 및 업데이트
     * 원문을 그대로 저장하고 간단한 키워드 매칭만 수행
     */
    private boolean extractAndUpdateInfo(TravelInfoCollectionState state, String message) {
        // 초기 메시지를 원문 그대로 저장
        // 파싱하지 않고 키워드 기반으로만 어떤 정보인지 판단
        boolean updated = false;
        String lowerMessage = message.toLowerCase();
        
        // 간단한 키워드 매칭으로 타입 판별
        if ((lowerMessage.contains("제주") || lowerMessage.contains("부산") || 
             lowerMessage.contains("서울") || lowerMessage.contains("경주") ||
             lowerMessage.contains("강릉") || lowerMessage.contains("여수")) && 
            !state.isDestinationCollected()) {
            // 목적지로 추정
            state.setDestinationRaw(message);
            state.setDestination(message);
            state.setDestinationCollected(true);
            updated = true;
            log.info("Initial destination detected (raw): {}", message);
        }
        
        return updated;
    }
    
    /**
     * 단계별 응답 파싱
     */
    private void parseResponseByStep(TravelInfoCollectionState state, String response) {
        switch (state.getCurrentStep()) {
            case ORIGIN -> parseOrigin(state, response);
            case DESTINATION -> parseDestination(state, response);
            case DATES -> parseDates(state, response);
            case DURATION -> parseDuration(state, response);
            case COMPANIONS -> parseCompanions(state, response);
            case BUDGET -> parseBudget(state, response);
        }
    }
    
    /**
     * 출발지 저장 (원문 그대로)
     */
    private void parseOrigin(TravelInfoCollectionState state, String response) {
        String origin = response.trim();
        if (!origin.isEmpty()) {
            state.setOriginRaw(origin);
            state.setOrigin(origin);
            state.setOriginCollected(true);
            state.setCurrentStep(TravelInfoCollectionState.CollectionStep.DESTINATION);
        }
    }
    
    /**
     * 목적지 저장 (원문 그대로)
     */
    private void parseDestination(TravelInfoCollectionState state, String response) {
        String destination = response.trim();
        if (!destination.isEmpty()) {
            state.setDestinationRaw(destination);
            state.setDestination(destination);
            state.setDestinationCollected(true);
            state.setCurrentStep(TravelInfoCollectionState.CollectionStep.DATES);
        }
    }
    
    /**
     * 날짜 저장 (원문 그대로)
     */
    private void parseDates(TravelInfoCollectionState state, String response) {
        // 원문 저장
        state.setDatesRaw(response.trim());
        state.setDatesCollected(true);
        
        // 간단한 날짜 파싱 시도 (선택적)
        try {
            TravelParsingUtils.DateRange dateRange = TravelParsingUtils.parseDateRange(response);
            if (dateRange != null) {
                state.setStartDate(dateRange.startDate());
                state.setEndDate(dateRange.endDate());
                state.setDurationNights(dateRange.getNights());
                state.setDurationCollected(true);
            }
        } catch (Exception e) {
            log.debug("Date parsing failed, but raw data saved: {}", e.getMessage());
        }
        
        // 다음 단계로 이동
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.COMPANIONS);
    }
    
    /**
     * 기간 저장 (원문 그대로)
     */
    private void parseDuration(TravelInfoCollectionState state, String response) {
        // 원문 저장
        state.setDurationRaw(response.trim());
        state.setDurationCollected(true);
        
        // 간단한 기간 파싱 시도 (선택적)
        try {
            int nights = TravelParsingUtils.parseDurationNights(response);
            state.setDurationNights(nights);
            
            // 시작 날짜가 있으면 종료 날짜 계산
            if (state.getStartDate() != null && state.getEndDate() == null) {
                state.setEndDate(state.getStartDate().plusDays(nights));
                state.setDatesCollected(true);
            }
        } catch (Exception e) {
            log.debug("Duration parsing failed, but raw data saved: {}", e.getMessage());
        }
        
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.COMPANIONS);
    }
    
    /**
     * 동행자 저장 (원문 그대로)
     */
    private void parseCompanions(TravelInfoCollectionState state, String response) {
        // 원문 저장
        state.setCompanionsRaw(response.trim());
        state.setCompanionsCollected(true);
        
        // 간단한 동행자 파싱 시도 (선택적)
        try {
            String lowerResponse = response.toLowerCase();
            
            if (lowerResponse.contains("혼자") || lowerResponse.contains("나홀로")) {
                state.setCompanionType("solo");
                state.setNumberOfTravelers(1);
            } else if (lowerResponse.contains("연인") || lowerResponse.contains("배우자") || 
                       lowerResponse.contains("커플") || lowerResponse.contains("둘이")) {
                state.setCompanionType("couple");
                state.setNumberOfTravelers(2);
            } else if (lowerResponse.contains("가족")) {
                state.setCompanionType("family");
                int travelerCount = TravelParsingUtils.parseTravelerCount(response, "family");
                state.setNumberOfTravelers(travelerCount);
            } else if (lowerResponse.contains("친구")) {
                state.setCompanionType("friends");
                int travelerCount = TravelParsingUtils.parseTravelerCount(response, "friends");
                state.setNumberOfTravelers(travelerCount);
            }
        } catch (Exception e) {
            log.debug("Companion parsing failed, but raw data saved: {}", e.getMessage());
        }
        
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.BUDGET);
    }
    
    /**
     * 예산 저장 (원문 그대로)
     */
    private void parseBudget(TravelInfoCollectionState state, String response) {
        // 원문 저장
        state.setBudgetRaw(response.trim());
        state.setBudgetCollected(true);
        
        // 간단한 예산 파싱 시도 (선택적)
        try {
            String lowerResponse = response.toLowerCase();
            
            if (lowerResponse.contains("알뜰") || lowerResponse.contains("저렴") || 
                lowerResponse.contains("가성비")) {
                state.setBudgetLevel("budget");
            } else if (lowerResponse.contains("적당") || lowerResponse.contains("보통") || 
                       lowerResponse.contains("중간")) {
                state.setBudgetLevel("moderate");
            } else if (lowerResponse.contains("럭셔리") || lowerResponse.contains("프리미엄") || 
                       lowerResponse.contains("고급")) {
                state.setBudgetLevel("luxury");
            }
            
            Integer amount = TravelParsingUtils.parseMoneyAmount(response);
            if (amount != null) {
                state.setBudgetPerPerson(amount);
                state.setBudgetCurrency("KRW");
            }
        } catch (Exception e) {
            log.debug("Budget parsing failed, but raw data saved: {}", e.getMessage());
        }
        
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.CONFIRMATION);
    }
    
    /**
     * 기간 계산
     */
    private void calculateDuration(TravelInfoCollectionState state) {
        if (state.getStartDate() != null && state.getEndDate() != null) {
            long nights = state.getEndDate().toEpochDay() - state.getStartDate().toEpochDay();
            state.setDurationNights((int) nights);
            state.setDurationCollected(true);
        }
    }
    
    /**
     * TripPlanningRequest로 변환
     * 원문 필드들을 포함하여 전달
     */
    private TripPlanningRequest convertToTripPlanningRequest(TravelInfoCollectionState state) {
        TripPlanningRequest request = new TripPlanningRequest();
        
        // 파싱된 데이터
        request.setDestination(state.getDestination());
        request.setStartDate(state.getStartDate());
        request.setEndDate(state.getEndDate());
        request.setNumberOfTravelers(state.getNumberOfTravelers());
        request.setBudgetPerPerson(state.getBudgetPerPerson());
        request.setCurrency(state.getBudgetCurrency() != null ? state.getBudgetCurrency() : "KRW");
        
        // Travel style 매핑
        if (state.getBudgetLevel() != null) {
            request.setTravelStyle(state.getBudgetLevel());
        }
        
        // Origin은 수집된 값 사용
        request.setOrigin(state.getOrigin());
        
        // 원문 데이터를 preferences에 추가
        Map<String, Object> preferences = new HashMap<>();
        
        // 원문 필드들 추가
        if (state.getOriginRaw() != null) {
            preferences.put("originRaw", state.getOriginRaw());
        }
        if (state.getDestinationRaw() != null) {
            preferences.put("destinationRaw", state.getDestinationRaw());
        }
        if (state.getDatesRaw() != null) {
            preferences.put("datesRaw", state.getDatesRaw());
        }
        if (state.getDurationRaw() != null) {
            preferences.put("durationRaw", state.getDurationRaw());
        }
        if (state.getCompanionsRaw() != null) {
            preferences.put("companionsRaw", state.getCompanionsRaw());
        }
        if (state.getBudgetRaw() != null) {
            preferences.put("budgetRaw", state.getBudgetRaw());
        }
        
        // 기존 추가 정보가 있으면 병합
        if (state.getAdditionalPreferences() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> additionalPrefs = objectMapper.readValue(
                        state.getAdditionalPreferences(), Map.class);
                preferences.putAll(additionalPrefs);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse additional preferences", e);
            }
        }
        
        request.setPreferences(preferences);
        
        return request;
    }
    
    /**
     * 세션 ID 생성
     */
    private String generateSessionId() {
        return "TIC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 세션 만료 확인
     */
    private boolean isSessionExpired(TravelInfoCollectionState state) {
        return state.getCreatedAt().plusHours(TravelConstants.SESSION_TIMEOUT_HOURS).isBefore(LocalDateTime.now());
    }
}