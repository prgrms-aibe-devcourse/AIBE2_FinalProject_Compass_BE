package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.dto.ChatDtos.MessageCreateDto;
import com.compass.domain.chat.dto.ChatDtos.MessageDto;
import com.compass.domain.chat.dto.ChatDtos.ThreadDto;
import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.FollowUpResponseDto;
import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.engine.TravelQuestionFlowEngine;
import com.compass.domain.chat.exception.ChatThreadNotFoundException;
import com.compass.domain.chat.exception.UnauthorizedThreadAccessException;
import com.compass.domain.chat.repository.ChatMessageRepository;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.repository.TravelInfoCollectionStateRepository;
import com.compass.domain.chat.service.ChatModelService;
import com.compass.domain.chat.service.ChatService;
import com.compass.domain.chat.service.FollowUpQuestionService;
import com.compass.domain.chat.service.SessionManagementService;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatModelService geminiChatService;
    private final ChatModelService openAiChatService;
    private final TravelQuestionFlowEngine flowEngine;
    private final FollowUpQuestionService followUpQuestionService;
    private final SessionManagementService sessionManagementService;
    private final TravelInfoCollectionStateRepository stateRepository;

    public ChatServiceImpl(
            ChatThreadRepository chatThreadRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            @Qualifier("geminiChatService") ChatModelService geminiChatService,
            @Qualifier("openAIChatService") ChatModelService openAiChatService,
            TravelQuestionFlowEngine flowEngine,
            FollowUpQuestionService followUpQuestionService,
            SessionManagementService sessionManagementService,
            TravelInfoCollectionStateRepository stateRepository) {
        this.chatThreadRepository = chatThreadRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.geminiChatService = geminiChatService;
        this.openAiChatService = openAiChatService;
        this.flowEngine = flowEngine;
        this.followUpQuestionService = followUpQuestionService;
        this.sessionManagementService = sessionManagementService;
        this.stateRepository = stateRepository;
    }


    @Override
    @Transactional
    public ThreadDto createThread(String userId) {
        log.debug("Creating new chat thread for user: {}", userId);
        
        // Parse userId to Long and find user
        Long userIdLong = Long.parseLong(userId);
        User user = userRepository.findById(userIdLong)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Create new thread with empty title (will be set on first message)
        ChatThread thread = ChatThread.builder()
            .user(user)
            .title("새 대화") // Default title until first message
            .build();
        
        thread = chatThreadRepository.save(thread);
        log.info("Created new chat thread with ID: {} for user: {}", thread.getId(), userId);
        
        return new ThreadDto(
            thread.getId(),
            userId,
            thread.getCreatedAt(),
            thread.getLatestMessagePreview()
        );
    }

    @Override
    public List<ThreadDto> getUserThreads(String userId, int skip, int limit) {
        log.debug("Getting threads for user: {}, skip: {}, limit: {}", userId, skip, limit);
        
        Long userIdLong = Long.parseLong(userId);
        
        // Use the repository method that properly orders by lastMessageAt with NULLS LAST
        List<ChatThread> threads = chatThreadRepository.findByUserIdOrderByLastMessageAtDesc(userIdLong);
        
        // Apply pagination manually after filtering
        return threads.stream()
            .skip(skip)
            .limit(limit)
            .map(thread -> {
                // Log for debugging
                log.debug("Thread ID: {}, Title: {}, LastMessageAt: {}", 
                         thread.getId(), thread.getTitle(), thread.getLastMessageAt());
                         
                // Use the thread title if it's not the default "새 대화"
                String displayTitle = thread.getTitle();
                if (displayTitle == null || displayTitle.equals("새 대화")) {
                    displayTitle = thread.getLatestMessagePreview();
                }
                
                return new ThreadDto(
                    thread.getId(),
                    userId,
                    thread.getCreatedAt(),
                    displayTitle,
                    thread.getLastMessageAt(),
                    thread.getTitle()
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<MessageDto> addMessageToThread(String threadId, String userId, MessageCreateDto messageDto) {
        log.debug("Adding message to thread: {} for user: {}", threadId, userId);
        
        Long userIdLong = Long.parseLong(userId);
        
        // Verify thread exists and belongs to user
        ChatThread thread = chatThreadRepository.findByIdAndUserId(threadId, userIdLong)
            .orElseThrow(() -> new ChatThreadNotFoundException(threadId, userIdLong));
        
        // Create and save user message
        ChatMessage userMessage = ChatMessage.builder()
            .thread(thread)
            .role("user")
            .content(messageDto.content())
            .timestamp(LocalDateTime.now())
            .build();
        
        userMessage = chatMessageRepository.save(userMessage);
        
        // Update thread title if it's the first user message
        // Check if this is the first user message in the thread
        long userMessageCount = chatMessageRepository.countByThreadId(threadId);
        if (userMessageCount == 1) { // The message we just saved is the first one
            thread.updateTitleFromFirstMessage(messageDto.content());
        }
        
        // Always update lastMessageAt when a message is added
        thread.setLastMessageAt(LocalDateTime.now());
        chatThreadRepository.save(thread);
        log.info("Updated thread - Title: {}, LastMessageAt: {}", thread.getTitle(), thread.getLastMessageAt());
        
        // Get recent messages for context (last 10 messages)
        List<ChatMessage> recentMessages = chatMessageRepository.findLatestMessagesByThreadId(threadId, 10);
        
        // Build context from recent messages
        StringBuilder contextBuilder = new StringBuilder();
        if (!recentMessages.isEmpty()) {
            contextBuilder.append("이전 대화 내용:\n");
            for (int i = recentMessages.size() - 1; i >= 0; i--) {
                ChatMessage msg = recentMessages.get(i);
                contextBuilder.append(msg.getRole().equals("user") ? "사용자: " : "AI: ");
                contextBuilder.append(msg.getContent()).append("\n");
            }
            contextBuilder.append("\n새로운 질문: ");
        }
        contextBuilder.append(messageDto.content());
        
        // Check user intent first
        String aiResponseContent = "";
        FollowUpResponseDto followUpQuestion = null;  // Store follow-up question if exists
        boolean isTravelPlanRequest = checkIfTravelPlanRequest(messageDto.content());
        
        // Check if this is a travel info collection flow
        TravelInfoCollectionState state = stateRepository.findBySessionId(threadId)
            .orElse(null);
        
        if (state == null && isTravelPlanRequest) {
            // Initialize travel info collection state for this thread
            User user = userRepository.findById(userIdLong).orElse(null);
            if (user == null) {
                log.error("User not found: {}", userIdLong);
                aiResponseContent = "사용자를 찾을 수 없습니다.";
            } else {
                // Extract any travel info from initial message
                Map<String, String> extractedInfo = extractTravelInfoFromMessage(messageDto.content());
                
                state = TravelInfoCollectionState.builder()
                    .sessionId(threadId)
                    .user(user)
                    .chatThread(thread)
                    .build();
                
                // Set extracted info if available
                if (extractedInfo.containsKey("destination")) {
                    state.setDestination(extractedInfo.get("destination"));
                    state.setDestinationRaw(extractedInfo.getOrDefault("destinationRaw", messageDto.content()));
                    state.setDestinationCollected(true);
                }
                
                if (extractedInfo.containsKey("dateRaw")) {
                    state.setDatesRaw(extractedInfo.get("dateRaw"));
                }
                
                if (extractedInfo.containsKey("durationNights")) {
                    state.setDurationNights(Integer.parseInt(extractedInfo.get("durationNights")));
                    state.setDurationRaw(extractedInfo.getOrDefault("durationRaw", ""));
                    state.setDurationCollected(true);
                }
                
                if (extractedInfo.containsKey("companionType")) {
                    state.setCompanionType(extractedInfo.get("companionType"));
                    if (extractedInfo.containsKey("numberOfTravelers")) {
                        state.setNumberOfTravelers(Integer.parseInt(extractedInfo.get("numberOfTravelers")));
                    }
                    state.setCompanionsRaw(messageDto.content());
                    state.setCompanionsCollected(true);
                }
                
                // Determine first question based on what we don't have
                // Always start with origin since users rarely provide it
                state.setCurrentStep(TravelInfoCollectionState.CollectionStep.ORIGIN);
                
                state = stateRepository.save(state);
                
                // Generate first question
                FollowUpQuestionDto firstQuestion = flowEngine.generateNextQuestion(state);
                aiResponseContent = firstQuestion.getPrimaryQuestion();
                
                // Convert to FollowUpResponseDto for frontend
                followUpQuestion = FollowUpResponseDto.from(firstQuestion);
                log.info("Started travel info collection with: {}", aiResponseContent);
            }
        } else if (state == null && !isTravelPlanRequest) {
            // Not a travel plan request - use regular Gemini response
            aiResponseContent = geminiChatService.generateResponse(contextBuilder.toString());
        } else if (state != null) {
            // Process user response and generate follow-up question
            state = flowEngine.processResponse(state, messageDto.content());
            stateRepository.save(state);
            
            if (!state.isAllRequiredInfoCollected()) {
                // Generate next follow-up question
                FollowUpQuestionDto nextQuestion = flowEngine.generateNextQuestion(state);
                aiResponseContent = nextQuestion.getPrimaryQuestion();
                
                // Convert to FollowUpResponseDto for frontend
                followUpQuestion = FollowUpResponseDto.from(nextQuestion);
                log.info("Generated follow-up question: {}", aiResponseContent);
            } else {
                // Collection complete, generate travel plan using Gemini
                String travelInfo = buildTravelInfoSummary(state);
                aiResponseContent = geminiChatService.generateResponse(
                    "다음 정보를 바탕으로 여행 일정을 추천해주세요:\n" + travelInfo
                );
                log.info("Travel info collection complete, generated travel plan");
            }
        }
        
        // Create and save AI message
        ChatMessage aiMessage = ChatMessage.builder()
            .thread(thread)
            .role("assistant")
            .content(aiResponseContent)
            .timestamp(LocalDateTime.now())
            .build();
        
        aiMessage = chatMessageRepository.save(aiMessage);
        
        // Update thread's last message time
        thread.setLastMessageAt(aiMessage.getTimestamp());
        chatThreadRepository.save(thread);
        
        // Convert to DTOs
        MessageDto userDto = new MessageDto(
            userMessage.getId().toString(),
            threadId,
            userMessage.getRole(),
            userMessage.getContent(),
            userMessage.getTimestampMillis()
        );
        
        MessageDto aiDto = new MessageDto(
            aiMessage.getId().toString(),
            threadId,
            aiMessage.getRole(),
            aiMessage.getContent(),
            aiMessage.getTimestampMillis(),
            followUpQuestion  // Include follow-up question if exists
        );
        
        return List.of(userDto, aiDto);
    }

    @Override
    public List<MessageDto> getMessages(String threadId, String userId, int limit, Long before) {
        log.debug("Getting messages for thread: {} for user: {}, limit: {}, before: {}", 
                  threadId, userId, limit, before);
        
        Long userIdLong = Long.parseLong(userId);
        
        // Verify thread exists and belongs to user
        if (!chatThreadRepository.existsByIdAndUserId(threadId, userIdLong)) {
            log.warn("Thread not found or unauthorized: {} for user: {}", threadId, userId);
            return null;
        }
        
        List<ChatMessage> messages;
        
        if (before != null) {
            // Convert milliseconds to LocalDateTime
            LocalDateTime beforeTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(before),
                java.time.ZoneId.systemDefault()
            );
            
            PageRequest pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
            messages = chatMessageRepository.findByThreadIdAndTimestampBefore(threadId, beforeTime, pageable)
                .getContent();
        } else {
            // Get latest messages
            messages = chatMessageRepository.findLatestMessagesByThreadId(threadId, limit);
        }
        
        // Convert to DTOs and reverse order (to show oldest first)
        List<MessageDto> messageDtos = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage msg = messages.get(i);
            messageDtos.add(new MessageDto(
                msg.getId().toString(),
                threadId,
                msg.getRole(),
                msg.getContent(),
                msg.getTimestampMillis()
            ));
        }
        
        return messageDtos;
    }
    
    @Override
    public Map<String, Object> chatWithGemini(String message) {
        String response = geminiChatService.generateResponse(message);
        Map<String, Object> result = new HashMap<>();
        result.put("model", geminiChatService.getModelName());
        result.put("message", message);
        result.put("response", response);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @Override
    public Map<String, Object> chatWithOpenAI(String message) {
        String response = openAiChatService.generateResponse(message);
        Map<String, Object> result = new HashMap<>();
        result.put("model", openAiChatService.getModelName());
        result.put("message", message);
        result.put("response", response);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    private String buildTravelInfoSummary(TravelInfoCollectionState state) {
        StringBuilder summary = new StringBuilder();
        
        if (state.getOrigin() != null) {
            summary.append("출발지: ").append(state.getOrigin()).append("\n");
        }
        if (state.getDestination() != null) {
            summary.append("목적지: ").append(state.getDestination()).append("\n");
        }
        if (state.getStartDate() != null) {
            summary.append("출발일: ").append(state.getStartDate()).append("\n");
        }
        if (state.getEndDate() != null) {
            summary.append("종료일: ").append(state.getEndDate()).append("\n");
        }
        if (state.getDurationNights() != null) {
            summary.append("여행 기간: ").append(state.getDurationNights()).append("박\n");
        }
        if (state.getNumberOfTravelers() != null) {
            summary.append("동행 인원: ").append(state.getNumberOfTravelers()).append("명\n");
        }
        if (state.getBudgetPerPerson() != null) {
            summary.append("예산(인당): ").append(state.getBudgetPerPerson()).append("원\n");
        }
        
        return summary.toString();
    }
    
    private boolean checkIfTravelPlanRequest(String message) {
        String lowerMessage = message.toLowerCase();
        
        // 여행 계획 관련 키워드
        boolean hasPlanKeyword = lowerMessage.contains("여행 계획") || 
               lowerMessage.contains("여행계획") ||
               lowerMessage.contains("일정 짜") ||
               lowerMessage.contains("일정 추천") ||
               lowerMessage.contains("여행 일정") ||
               lowerMessage.contains("플랜") ||
               lowerMessage.contains("여행 코스") ||
               lowerMessage.contains("trip plan") ||
               lowerMessage.contains("코스 짜") ||
               lowerMessage.contains("코스 추천");
        
        // 단순 추천과 구분하기 위한 패턴
        boolean isSimpleRecommendation = 
               (lowerMessage.contains("뭐있") || lowerMessage.contains("뭐 있") ||
                lowerMessage.contains("뭐할") || lowerMessage.contains("뭐 할") ||
                lowerMessage.contains("어디가") || lowerMessage.contains("어디 가")) &&
               !hasPlanKeyword;
        
        return hasPlanKeyword && !isSimpleRecommendation;
    }
    
    private Map<String, String> extractTravelInfoFromMessage(String message) {
        Map<String, String> info = new HashMap<>();
        String lowerMessage = message.toLowerCase();
        
        // Extract destination if mentioned (목적지 추출)
        if (message.contains("진접역") || message.contains("진접")) {
            info.put("destination", "진접");
            info.put("destinationRaw", message); // 원문 저장
        } else if (message.contains("제주도") || message.contains("제주")) {
            info.put("destination", "제주도");
            info.put("destinationRaw", message);
        } else if (message.contains("부산")) {
            info.put("destination", "부산");
            info.put("destinationRaw", message);
        } else if (message.contains("강릉")) {
            info.put("destination", "강릉");
            info.put("destinationRaw", message);
        } else if (message.contains("서울")) {
            info.put("destination", "서울");
            info.put("destinationRaw", message);
        } else if (message.contains("경주")) {
            info.put("destination", "경주");
            info.put("destinationRaw", message);
        } else if (message.contains("전주")) {
            info.put("destination", "전주");
            info.put("destinationRaw", message);
        } else if (message.contains("속초")) {
            info.put("destination", "속초");
            info.put("destinationRaw", message);
        } else if (message.contains("여수")) {
            info.put("destination", "여수");
            info.put("destinationRaw", message);
        }
        
        // Extract dates if mentioned (날짜 추출)
        if (message.contains("내일")) {
            info.put("dateRaw", "내일");
        } else if (message.contains("이번 주말") || message.contains("이번주말")) {
            info.put("dateRaw", "이번 주말");
        } else if (message.contains("다음 주말") || message.contains("다음주말")) {
            info.put("dateRaw", "다음 주말");
        } else if (message.contains("이번 달") || message.contains("이번달")) {
            info.put("dateRaw", "이번 달");
        } else if (message.contains("다음 달") || message.contains("다음달")) {
            info.put("dateRaw", "다음 달");
        }
        
        // Extract duration if mentioned (기간 추출)
        if (message.contains("당일치기")) {
            info.put("durationRaw", "당일치기");
            info.put("durationNights", "0");
        } else if (message.contains("1박2일") || message.contains("1박 2일")) {
            info.put("durationRaw", "1박2일");
            info.put("durationNights", "1");
        } else if (message.contains("2박3일") || message.contains("2박 3일")) {
            info.put("durationRaw", "2박3일");
            info.put("durationNights", "2");
        } else if (message.contains("3박4일") || message.contains("3박 4일")) {
            info.put("durationRaw", "3박4일");
            info.put("durationNights", "3");
        }
        
        // Extract companion info if mentioned (동행자 추출)
        if (lowerMessage.contains("혼자")) {
            info.put("companionType", "solo");
            info.put("numberOfTravelers", "1");
        } else if (lowerMessage.contains("친구") || lowerMessage.contains("친구들")) {
            info.put("companionType", "friends");
        } else if (lowerMessage.contains("가족")) {
            info.put("companionType", "family");
        } else if (lowerMessage.contains("연인") || lowerMessage.contains("애인") || 
                   lowerMessage.contains("남자친구") || lowerMessage.contains("여자친구")) {
            info.put("companionType", "couple");
            info.put("numberOfTravelers", "2");
        }
        
        return info;
    }
}
