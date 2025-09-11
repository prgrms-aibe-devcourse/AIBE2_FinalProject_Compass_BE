package com.compass.domain.chat.service;

import com.compass.domain.chat.constant.FollowUpConstants;
import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.FollowUpResponseDto;
import com.compass.domain.chat.engine.RefactoredQuestionFlowEngine;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.repository.ChatMessageRepository;
import com.compass.domain.chat.repository.TravelInfoCollectionStateRepository;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import com.compass.domain.chat.util.ProgressCalculator;
import com.compass.domain.chat.util.TravelInfoValidator;
import com.compass.domain.chat.dto.ValidationResult;
import com.compass.domain.chat.exception.FollowUpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 꼬리질문 플로우 통합 서비스
 * REQ-FOLLOW-001 ~ REQ-FOLLOW-006 통합 구현
 * 
 * 프론트엔드와 연동하여 실제 채팅에서 꼬리질문 플로우를 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpQuestionService {
    
    private final RefactoredQuestionFlowEngine flowEngine;
    private final NaturalLanguageParsingService parsingService;
    private final TravelInfoValidator validator;
    private final SessionManagementService sessionService;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageService chatMessageService;
    private final TravelInfoCollectionStateRepository stateRepository;
    private final UserRepository userRepository;
    private final StatePersistenceService persistenceService;
    
    // Constants
    private static final String DEFAULT_TRIP_TITLE = "여행 계획 상담";
    private static final String TRIP_TITLE_SUFFIX = " 여행 계획";
    private static final int MAX_TITLE_LENGTH = 50;
    
    // 중복 요청 추적을 위한 임시 저장소 (Redis 사용 가능 시 Redis로 전환)
    private final Map<String, Long> recentRequests = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long DUPLICATE_REQUEST_WINDOW_MS = 5000; // 5초
    
    /**
     * 중복 요청 체크
     * @param dedupeKey 중복 방지 키
     * @return 중복 요청이면 true
     */
    public boolean isDuplicateRequest(String dedupeKey) {
        if (dedupeKey == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        
        // 오래된 요청 정리
        recentRequests.entrySet().removeIf(entry -> 
            now - entry.getValue() > DUPLICATE_REQUEST_WINDOW_MS);
        
        // 중복 체크
        Long lastRequestTime = recentRequests.get(dedupeKey);
        if (lastRequestTime != null && (now - lastRequestTime) < DUPLICATE_REQUEST_WINDOW_MS) {
            return true;
        }
        
        // 새 요청 기록
        recentRequests.put(dedupeKey, now);
        return false;
    }
    
    /**
     * 사용자 검증 및 조회
     */
    private User validateAndGetUser(String userId) {
        if (userId == null) {
            throw new FollowUpException.InvalidUserIdException("null");
        }
        
        try {
            Long userIdLong = Long.parseLong(userId);
            return userRepository.findById(userIdLong)
                    .orElseThrow(() -> new FollowUpException.UserNotFoundException(userId));
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID format: {}", userId);
            throw new FollowUpException.InvalidUserIdException(userId);
        }
    }
    
    // ChatThread 생성 메서드는 StatePersistenceService로 이동함
    
    /**
     * Thread 제목 생성
     */
    private String generateThreadTitle(String destination, String initialMessage) {
        if (destination != null && !destination.isEmpty()) {
            return destination + TRIP_TITLE_SUFFIX;
        } else if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            return initialMessage.length() > MAX_TITLE_LENGTH ? 
                   initialMessage.substring(0, MAX_TITLE_LENGTH - 3) + "..." : 
                   initialMessage;
        } else {
            return DEFAULT_TRIP_TITLE;
        }
    }
    
    // saveStateToStorage 메서드는 StatePersistenceService로 이동함
    
    /**
     * 새로운 꼬리질문 세션 시작 (이전 버전과의 호환성)
     */
    public FollowUpResponseDto startSession(String userId, String initialMessage) {
        return startSession(userId, initialMessage, null);
    }
    
    /**
     * 새로운 꼬리질문 세션 시작
     * REQ-FOLLOW-001: 질문 플로우 엔진 시작
     * @param userId 사용자 ID
     * @param initialMessage 초기 메시지
     * @param threadId 기존 채팅 쓰레드 ID (옵션)
     */
    @Transactional
    public synchronized FollowUpResponseDto startSession(String userId, String initialMessage, String threadId) {
        // 동일한 사용자의 중복 요청 방지를 위한 동기화 처리
        // TODO: 향후 Redis 분산 락으로 개선 필요
        
        // 세션 ID 생성
        String sessionId = sessionService.generateSessionId();
        log.info("Starting follow-up question session: {} for user: {} with thread: {}", sessionId, userId, threadId);
        
        // 사용자 조회
        User user = validateAndGetUser(userId);
        
        // ChatThread 처리 - 없으면 새로 생성
        ChatThread chatThread = null;
        if (threadId != null && user != null) {
            chatThread = chatThreadRepository.findByIdAndUserId(threadId, user.getId()).orElse(null);
        }
        
        // 초기 상태 생성 (ChatThread 생성 전에 파싱 먼저 수행)
        TravelInfoCollectionState state = new TravelInfoCollectionState();
        state.setSessionId(sessionId);
        state.setUser(user);
        state.setChatThread(null);  // 일단 null로 설정
        
        // 초기 단계 설정 - INITIAL로 시작
        state.setCurrentStep(TravelInfoCollectionState.CollectionStep.INITIAL);
        
        state.setCompleted(false);
        state.setCreatedAt(LocalDateTime.now());
        state.setUpdatedAt(LocalDateTime.now());
        
        // 초기 메시지 파싱 - LLM만 사용 (키워드 감지 제거)
        String destination = null;
        if (initialMessage != null && !initialMessage.trim().isEmpty()) {
            try {
                // REQ-FOLLOW-003: Gemini를 활용한 자연어 파싱
                log.info("Parsing initial message with Gemini: {}", initialMessage);
                
                // 각 필드별 파싱 수행
                String parsedOrigin = parsingService.parseOrigin(initialMessage);
                String parsedDestination = parsingService.parseDestination(initialMessage);
                com.compass.domain.chat.util.TravelParsingUtils.DateRange dateRange = parsingService.parseDates(initialMessage);
                Integer duration = parsingService.parseDuration(initialMessage);
                Map<String, Object> companions = parsingService.parseCompanions(initialMessage);
                Map<String, Object> budget = parsingService.parseBudget(initialMessage);
                
                // 파싱 결과를 state에 적용
                if (parsedOrigin != null) {
                    state.setOrigin(parsedOrigin);
                    state.setOriginRaw(initialMessage);
                    state.setOriginCollected(true);
                    log.info("Parsed origin: {}", parsedOrigin);
                }
                
                if (parsedDestination != null) {
                    state.setDestination(parsedDestination);
                    state.setDestinationRaw(initialMessage);
                    state.setDestinationCollected(true);
                    destination = parsedDestination;
                    log.info("Parsed destination: {}", parsedDestination);
                }
                
                if (dateRange != null) {
                    state.setStartDate(dateRange.startDate());
                    state.setEndDate(dateRange.endDate());
                    state.setDatesRaw(initialMessage);
                    state.setDatesCollected(true);
                    log.info("Parsed dates: {} ~ {}", dateRange.startDate(), dateRange.endDate());
                }
                
                if (duration != null) {
                    state.setDurationNights(duration);
                    state.setDurationRaw(initialMessage);
                    state.setDurationCollected(true);
                    log.info("Parsed duration: {} nights", duration);
                }
                
                if (companions != null && companions.get("numberOfTravelers") != null && 
                    !companions.get("numberOfTravelers").equals(1)) {  // 1명이면 기본값이므로 수집하지 않음
                    state.setNumberOfTravelers((Integer) companions.get("numberOfTravelers"));
                    state.setCompanionType((String) companions.get("groupType"));
                    state.setCompanionsRaw(initialMessage);
                    // 명시적인 동행자 정보가 있을 때만 수집된 것으로 처리
                    if (initialMessage.contains("명") || initialMessage.contains("친구") || 
                        initialMessage.contains("가족") || initialMessage.contains("커플") ||
                        initialMessage.contains("혼자")) {
                        state.setCompanionsCollected(true);
                        log.info("Parsed companions: {} travelers, type: {}", 
                                companions.get("numberOfTravelers"), companions.get("groupType"));
                    } else {
                        state.setCompanionsCollected(false);
                        log.info("No explicit companion info found, will ask later");
                    }
                }
                
                if (budget != null && (budget.get("budgetLevel") != null || budget.get("budgetPerPerson") != null)) {
                    state.setBudgetLevel((String) budget.get("budgetLevel"));
                    state.setBudgetPerPerson((Integer) budget.get("budgetPerPerson"));
                    state.setBudgetCurrency((String) budget.get("currency"));
                    state.setBudgetRaw(initialMessage);
                    // 명시적인 예산 정보가 있을 때만 수집된 것으로 처리
                    if (initialMessage.contains("원") || initialMessage.contains("예산") || 
                        initialMessage.contains("돈") || initialMessage.contains("비용")) {
                        state.setBudgetCollected(true);
                        log.info("Parsed budget: level={}, amount={}", 
                                budget.get("budgetLevel"), budget.get("budgetPerPerson"));
                    } else {
                        state.setBudgetCollected(false);
                        log.info("No explicit budget info found, will ask later");
                    }
                }
                
                // 다음 필요한 단계 결정
                state.setCurrentStep(state.getNextRequiredStep());
                log.info("After parsing, next step: {}, completion: {}%", 
                        state.getCurrentStep(), state.getCompletionPercentage());
                
            } catch (Exception e) {
                // LLM 파싱 실패 시 로그만 남기고 초기 상태로 계속 진행
                log.warn("Failed to parse initial message with Gemini: {}", e.getMessage());
                state.setCurrentStep(TravelInfoCollectionState.CollectionStep.ORIGIN);
            }
        } else {
            // 초기 메시지가 없으면 ORIGIN부터 시작
            state.setCurrentStep(TravelInfoCollectionState.CollectionStep.ORIGIN);
        }
        
        // ChatThread가 없으면 새로 생성 (제목을 동적으로 설정)
        if (chatThread == null && user != null) {
            String title = generateThreadTitle(destination, initialMessage);
            chatThread = persistenceService.persistChatThread(user, title);
        }
        
        // state에 ChatThread 설정
        state.setChatThread(chatThread);
        
        // 상태 저장 (DB 및 Redis) - 독립적 트랜잭션으로 저장
        state = persistenceService.persistState(state);
        
        // ChatThread에 초기 메시지 저장
        if (chatThread != null) {
            chatMessageService.saveFollowUpStartMessages(chatThread, initialMessage, sessionId);
        }
        
        // 첫 번째 질문 생성
        FollowUpQuestionDto question = flowEngine.generateNextQuestion(state);
        
        // 첫 번째 질문 로그
        if (question != null) {
            log.info("First question: {}", question.getPrimaryQuestion());
        }
        
        // 첫 번째 질문도 ChatMessage로 저장
        if (chatThread != null && question != null) {
            chatMessageService.saveFollowUpQuestion(
                chatThread, 
                question.getPrimaryQuestion(), 
                question.getCurrentStep() != null ? question.getCurrentStep().name() : ""
            );
        }
        
        return buildResponse(sessionId, state, question);
    }
    
    /**
     * 사용자 응답 처리 및 다음 질문 생성
     * REQ-FOLLOW-002: 정보 수집 및 파싱
     */
    @Transactional
    public FollowUpResponseDto processUserResponse(String sessionId, String userResponse) {
        log.info("Processing user response for session: {}", sessionId);
        
        // DB에서 상태 로드
        TravelInfoCollectionState state = stateRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new FollowUpException.SessionExpiredException(sessionId));
        
        // ChatThread 처리
        ChatThread chatThread = state.getChatThread();
        if (chatThread != null) {
            ChatMessage userMessage = ChatMessage.builder()
                    .thread(chatThread)
                    .role("user")
                    .content(userResponse)
                    .timestamp(LocalDateTime.now())
                    .metadata(Map.of("type", "follow_up_response", 
                                    "step", state.getCurrentStep() != null ? state.getCurrentStep().name() : ""))
                    .build();
            chatMessageRepository.save(userMessage);
            
            // ChatThread 업데이트
            chatThread.setLastMessageAt(LocalDateTime.now());
            chatThreadRepository.save(chatThread);
        }
        
        // 사용자 응답 처리
        state = flowEngine.processResponse(state, userResponse);
        
        // DB에 상태 저장
        state = stateRepository.save(state);
        log.info("Session saved to DB - SessionId: {}, Origin: {}, Destination: {}", 
                sessionId, state.isOriginCollected(), state.isDestinationCollected());
        
        // 완료 여부 확인
        boolean isComplete = flowEngine.isFlowComplete(state);
        state.setCompleted(isComplete);
        
        // 완료 시 ChatThread와 ChatMessage로 저장
        if (isComplete) {
            saveToChatHistory(state);
        }
        
        // 다음 질문 생성 또는 완료 처리
        FollowUpQuestionDto question = null;
        if (!isComplete) {
            question = flowEngine.generateNextQuestion(state);
            
            // 다음 질문도 ChatMessage로 저장
            if (chatThread != null && question != null) {
                ChatMessage assistantMessage = ChatMessage.builder()
                        .thread(chatThread)
                        .role("assistant")
                        .content(question.getPrimaryQuestion())
                        .timestamp(LocalDateTime.now())
                        .metadata(Map.of("type", "follow_up_question", 
                                        "step", question.getCurrentStep() != null ? question.getCurrentStep().name() : ""))
                        .build();
                chatMessageRepository.save(assistantMessage);
            }
        }
        
        return buildResponse(sessionId, state, question);
    }
    
    /**
     * 현재 세션 상태 조회
     */
    public FollowUpResponseDto getSessionStatus(String sessionId) {
        TravelInfoCollectionState state = sessionService.loadSession(sessionId);
        if (state == null) {
            return FollowUpResponseDto.builder()
                    .sessionId(sessionId)
                    .isExpired(true)
                    .message(FollowUpConstants.SESSION_EXPIRED_MESSAGE)
                    .build();
        }
        
        FollowUpQuestionDto question = null;
        if (!state.isCompleted()) {
            question = flowEngine.generateNextQuestion(state);
        }
        
        return buildResponse(sessionId, state, question);
    }
    
    /**
     * 세션 정보 검증
     * REQ-FOLLOW-005: 완성도 검증
     */
    public ValidationResult validateSession(String sessionId) {
        TravelInfoCollectionState state = sessionService.loadSession(sessionId);
        if (state == null) {
            return ValidationResult.builder()
                    .valid(false)
                    .validationLevel(ValidationResult.ValidationLevel.STANDARD)
                    .userFriendlyMessage(FollowUpConstants.SESSION_EXPIRED_MESSAGE)
                    .build();
        }
        
        return validator.validate(state, ValidationResult.ValidationLevel.STANDARD);
    }
    
    /**
     * 응답 DTO 생성
     */
    private FollowUpResponseDto buildResponse(String sessionId, TravelInfoCollectionState state, FollowUpQuestionDto question) {
        // 진행률 계산
        int progress = ProgressCalculator.calculateProgress(state);
        
        // 여행계획 생성 가능 여부 확인
        boolean canGeneratePlan = ProgressCalculator.canGenerateTravelPlan(state);
        
        FollowUpResponseDto.FollowUpResponseDtoBuilder builder = FollowUpResponseDto.builder()
                .sessionId(sessionId)
                .threadId(state.getChatThread() != null ? state.getChatThread().getId() : null)
                .isComplete(state.isCompleted())
                .progressPercentage(progress)
                .collectedInfo(state.toInfoMap())
                .canGeneratePlan(canGeneratePlan);
        
        if (question != null) {
            builder.questionType(question.isClarification() ? 
                    FollowUpConstants.QUESTION_TYPE_CLARIFICATION : 
                    FollowUpConstants.QUESTION_TYPE_FOLLOW_UP)
                   .question(question.getPrimaryQuestion())
                   .helpText(question.getHelpText())
                   .quickOptions(question.getQuickOptions())
                   .exampleAnswers(question.getExampleAnswers())
                   .inputType(question.getInputType())
                   .currentStep(question.getCurrentStep() != null ? question.getCurrentStep().name() : null);
            
            // UI 타입 힌트 추가
            if ("date-range".equals(question.getInputType())) {
                builder.uiType("calendar");
            } else if ("multi-select".equals(question.getInputType())) {
                builder.uiType("checkbox-group");
            }
        } else if (state.isCompleted()) {
            builder.questionType(FollowUpConstants.QUESTION_TYPE_COMPLETE)
                   .question(FollowUpConstants.COMPLETION_MESSAGE)
                   .message("이제 여행 계획을 생성할 수 있습니다.");
        }
        
        return builder.build();
    }
    
    // applyParsedDataToState 메서드 제거 - 개별 파싱 메서드로 대체
    
    /**
     * 수집된 여행 정보를 ChatThread와 ChatMessage로 저장
     */
    private void saveToChatHistory(TravelInfoCollectionState state) {
        try {
            // ChatThread 생성 또는 가져오기
            ChatThread chatThread = state.getChatThread();
            if (chatThread == null && state.getUser() != null) {
                // 새 ChatThread 생성
                chatThread = ChatThread.builder()
                        .id(UUID.randomUUID().toString())
                        .user(state.getUser())
                        .title("여행 계획: " + state.getDestination())
                        .createdAt(LocalDateTime.now())
                        .build();
                chatThread = chatThreadRepository.save(chatThread);
                
                // state에 ChatThread 연결
                state.setChatThread(chatThread);
                if (state.getId() != null) {
                    stateRepository.save(state);
                }
            }
            
            if (chatThread != null) {
                // 수집된 정보를 요약한 시스템 메시지 생성
                String summary = createTravelInfoSummary(state);
                
                // 시스템 메시지로 저장
                ChatMessage systemMessage = ChatMessage.builder()
                        .thread(chatThread)
                        .role("system")
                        .content("[꼬리질문 완료] 수집된 여행 정보:\n" + summary)
                        .timestamp(LocalDateTime.now())
                        .metadata(createMetadata(state))
                        .build();
                chatMessageRepository.save(systemMessage);
                
                // 사용자 확인 메시지
                ChatMessage userConfirmMessage = ChatMessage.builder()
                        .thread(chatThread)
                        .role("user")
                        .content("여행 정보 입력 완료")
                        .timestamp(LocalDateTime.now())
                        .build();
                chatMessageRepository.save(userConfirmMessage);
                
                // Assistant 응답 메시지
                ChatMessage assistantMessage = ChatMessage.builder()
                        .thread(chatThread)
                        .role("assistant")
                        .content("여행 정보가 성공적으로 수집되었습니다. 이제 맞춤형 여행 계획을 생성할 수 있습니다!\n\n" + summary)
                        .timestamp(LocalDateTime.now())
                        .build();
                chatMessageRepository.save(assistantMessage);
                
                // ChatThread 업데이트
                chatThread.setLastMessageAt(LocalDateTime.now());
                chatThreadRepository.save(chatThread);
                
                log.info("Successfully saved travel info to chat history. Thread ID: {}", chatThread.getId());
            }
        } catch (Exception e) {
            log.error("Error saving travel info to chat history", e);
        }
    }
    
    /**
     * 여행 정보 요약 생성
     */
    private String createTravelInfoSummary(TravelInfoCollectionState state) {
        StringBuilder summary = new StringBuilder();
        
        if (state.getOrigin() != null) {
            summary.append("• 출발지: ").append(state.getOrigin()).append("\n");
        }
        if (state.getDestination() != null) {
            summary.append("• 목적지: ").append(state.getDestination()).append("\n");
        }
        if (state.getStartDate() != null && state.getEndDate() != null) {
            summary.append("• 기간: ").append(state.getStartDate())
                   .append(" ~ ").append(state.getEndDate()).append("\n");
        }
        if (state.getDurationNights() != null) {
            summary.append("• 숙박: ").append(state.getDurationNights()).append("박\n");
        }
        if (state.getNumberOfTravelers() != null) {
            summary.append("• 인원: ").append(state.getNumberOfTravelers()).append("명\n");
        }
        if (state.getCompanionType() != null) {
            summary.append("• 동행: ").append(state.getCompanionType()).append("\n");
        }
        if (state.getBudgetLevel() != null) {
            summary.append("• 예산: ").append(state.getBudgetLevel()).append("\n");
        }
        if (state.getBudgetPerPerson() != null && state.getBudgetCurrency() != null) {
            summary.append("• 1인 예산: ").append(state.getBudgetPerPerson())
                   .append(" ").append(state.getBudgetCurrency()).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 메타데이터 생성
     */
    private Map<String, Object> createMetadata(TravelInfoCollectionState state) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "follow_up_completion");
        metadata.put("sessionId", state.getSessionId());
        metadata.put("collectedInfo", state.toInfoMap());
        metadata.put("completionPercentage", state.getCompletionPercentage());
        metadata.put("completedAt", LocalDateTime.now().toString());
        return metadata;
    }
    
    /**
     * ChatThread 조회
     * @param threadId ChatThread ID
     * @return ChatThread 엔티티
     */
    public ChatThread getChatThreadById(String threadId) {
        return chatThreadRepository.findById(threadId).orElse(null);
    }
    
    /**
     * 여행 계획 정보를 JSON으로 ChatThread에 저장
     * @param threadId ChatThread ID
     * @param travelData 저장할 여행 정보
     * @return 저장 성공 여부
     */
    @Transactional
    public boolean saveTravelPlanJson(String threadId, Map<String, Object> travelData) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonData = objectMapper.writeValueAsString(travelData);
            
            return chatThreadRepository.findById(threadId)
                    .map(thread -> {
                        thread.setTravelPlanData(jsonData);
                        chatThreadRepository.save(thread);
                        log.info("Travel plan JSON saved to ChatThread: {}", threadId);
                        return true;
                    })
                    .orElseGet(() -> {
                        log.warn("ChatThread not found: {}", threadId);
                        return false;
                    });
        } catch (Exception e) {
            log.error("Failed to save travel plan JSON for thread: {}", threadId, e);
            return false;
        }
    }
}