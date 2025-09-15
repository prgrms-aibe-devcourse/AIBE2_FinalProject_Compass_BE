package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.ChatDtos;
import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.engine.TravelQuestionFlowEngine;
import com.compass.domain.chat.repository.ChatMessageRepository;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.repository.TravelInfoCollectionStateRepository;
import com.compass.domain.chat.service.impl.ChatServiceImpl;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatThreadRepository chatThreadRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatModelService geminiChatService;

    @Mock
    private ChatModelService openAiChatService;

    @Mock
    private TravelQuestionFlowEngine flowEngine;

    @Mock
    private FollowUpQuestionService followUpQuestionService;

    @Mock
    private SessionManagementService sessionManagementService;

    @Mock
    private TravelInfoCollectionStateRepository stateRepository;

    private ChatServiceImpl chatService;

    private User testUser;
    private ChatThread testThread;
    private String userId = "1";
    private String threadId = "test-thread-id";

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
            chatThreadRepository,
            chatMessageRepository,
            userRepository,
            geminiChatService,
            openAiChatService,
            flowEngine,
            followUpQuestionService,
            sessionManagementService,
            stateRepository
        );

        testUser = User.builder()
                .email("test@example.com")
                .nickname("TestUser")
                .role(Role.USER)
                .build();

        testThread = ChatThread.builder()
                .user(testUser)
                .title("새 대화")
                .build();
        // onCreate is protected, so we don't call it directly in tests
    }

    @Test
    @DisplayName("새 스레드 생성 시 '새 대화' 제목으로 생성되어야 함")
    void testCreateThread() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatThreadRepository.save(any(ChatThread.class))).thenAnswer(invocation -> {
            ChatThread thread = invocation.getArgument(0);
            // Simulate ID generation
            return thread;
        });

        // When
        ChatDtos.ThreadDto result = chatService.createThread(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.userId());
        verify(chatThreadRepository).save(argThat(thread -> 
            "새 대화".equals(thread.getTitle())
        ));
    }

    @Test
    @DisplayName("첫 번째 메시지 전송 시 스레드 제목이 메시지 내용으로 업데이트되어야 함")
    void testFirstMessageUpdatesThreadTitle() {
        // Given
        String firstMessage = "제주도 3박 4일 여행 추천해주세요";
        ChatDtos.MessageCreateDto messageDto = new ChatDtos.MessageCreateDto(firstMessage);
        
        testThread.setId(threadId);
        
        when(chatThreadRepository.findByIdAndUserId(threadId, 1L))
                .thenReturn(Optional.of(testThread));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> {
                    ChatMessage msg = invocation.getArgument(0);
                    msg.setId(1L); // Set ID to simulate database save
                    return msg;
                });
        when(chatMessageRepository.countByThreadId(threadId))
                .thenReturn(1L); // First message
        when(chatThreadRepository.save(any(ChatThread.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.findLatestMessagesByThreadId(anyString(), anyInt()))
                .thenReturn(java.util.Collections.emptyList());
        when(geminiChatService.generateResponse(anyString()))
                .thenReturn("제주도 여행 추천 답변");

        // When
        chatService.addMessageToThread(threadId, userId, messageDto);

        // Then
        verify(chatThreadRepository, times(2)).save(argThat(thread -> 
            firstMessage.equals(thread.getTitle())
        ));
    }

    @Test
    @DisplayName("두 번째 메시지 전송 시 스레드 제목이 변경되지 않아야 함")
    void testSecondMessageDoesNotUpdateTitle() {
        // Given
        String existingTitle = "첫 번째 질문입니다";
        String secondMessage = "추가 질문입니다";
        ChatDtos.MessageCreateDto messageDto = new ChatDtos.MessageCreateDto(secondMessage);
        
        testThread.setId(threadId);
        testThread.updateTitleFromFirstMessage(existingTitle);
        
        when(chatThreadRepository.findByIdAndUserId(threadId, 1L))
                .thenReturn(Optional.of(testThread));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> {
                    ChatMessage msg = invocation.getArgument(0);
                    msg.setId(1L); // Set ID to simulate database save
                    return msg;
                });
        when(chatMessageRepository.countByThreadId(threadId))
                .thenReturn(3L); // Already has messages (user + assistant + new user message)
        when(chatMessageRepository.findLatestMessagesByThreadId(anyString(), anyInt()))
                .thenReturn(java.util.Collections.emptyList());
        when(geminiChatService.generateResponse(anyString()))
                .thenReturn("답변");

        // When
        chatService.addMessageToThread(threadId, userId, messageDto);

        // Then
        // Thread is saved twice - once for lastMessageAt update, once for assistant message
        verify(chatThreadRepository, atLeastOnce()).save(any(ChatThread.class));
        assertEquals(existingTitle, testThread.getTitle());
    }

    @Test
    @DisplayName("긴 첫 메시지는 50자로 잘려서 제목이 되어야 함")
    void testLongFirstMessageTruncatesTitle() {
        // Given
        String longMessage = "안녕하세요! 저는 다음 달에 제주도로 3박 4일 여행을 계획하고 있는데요, 맛있는 음식점과 경치 좋은 관광지를 추천해주실 수 있나요? 특히 해산물을 좋아합니다.";
        ChatDtos.MessageCreateDto messageDto = new ChatDtos.MessageCreateDto(longMessage);
        
        testThread.setId(threadId);
        
        when(chatThreadRepository.findByIdAndUserId(threadId, 1L))
                .thenReturn(Optional.of(testThread));
        when(chatMessageRepository.save(any(ChatMessage.class)))
                .thenAnswer(invocation -> {
                    ChatMessage msg = invocation.getArgument(0);
                    msg.setId(1L); // Set ID to simulate database save
                    return msg;
                });
        when(chatMessageRepository.countByThreadId(threadId))
                .thenReturn(1L); // First message
        when(chatThreadRepository.save(any(ChatThread.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.findLatestMessagesByThreadId(anyString(), anyInt()))
                .thenReturn(java.util.Collections.emptyList());
        when(geminiChatService.generateResponse(anyString()))
                .thenReturn("답변");

        // When
        chatService.addMessageToThread(threadId, userId, messageDto);

        // Then
        verify(chatThreadRepository, times(2)).save(argThat(thread -> {
            String title = thread.getTitle();
            return title != null && 
                   title.length() == 53 && // 50 chars + "..."
                   title.endsWith("...") &&
                   title.startsWith(longMessage.substring(0, 50));
        }));
    }
}