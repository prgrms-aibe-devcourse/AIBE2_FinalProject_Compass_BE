package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatMessageRepository
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test-no-redis")
@Tag("unit")
@DisplayName("ChatMessageRepository Unit Tests")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatThreadRepository chatThreadRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private ChatThread testThread;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.builder()
            .email("test@example.com")
            .password("password")
            .nickname("TestUser")
            .role(Role.USER)
            .build();
        testUser = userRepository.save(testUser);

        // Create test thread
        testThread = ChatThread.builder()
            .user(testUser)
            .title("Test Chat")
            .build();
        testThread = chatThreadRepository.save(testThread);
    }

    @Test
    @DisplayName("Should create and save a chat message")
    void testCreateChatMessage() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", "gemini");
        metadata.put("version", "2.0");

        ChatMessage message = ChatMessage.builder()
            .thread(testThread)
            .role("user")
            .content("Hello, AI!")
            .tokenCount(3)
            .metadata(metadata)
            .build();

        // When
        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Then
        assertThat(savedMessage).isNotNull();
        assertThat(savedMessage.getId()).isNotNull();
        assertThat(savedMessage.getThread()).isEqualTo(testThread);
        assertThat(savedMessage.getRole()).isEqualTo("user");
        assertThat(savedMessage.getContent()).isEqualTo("Hello, AI!");
        assertThat(savedMessage.getTokenCount()).isEqualTo(3);
        assertThat(savedMessage.getMetadata()).containsEntry("model", "gemini");
        assertThat(savedMessage.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should find messages by thread ID ordered by timestamp DESC")
    void testFindByThreadIdOrderByTimestampDesc() {
        // Given
        for (int i = 1; i <= 3; i++) {
            ChatMessage message = ChatMessage.builder()
                .thread(testThread)
                .role(i % 2 == 1 ? "user" : "assistant")
                .content("Message " + i)
                .timestamp(LocalDateTime.now().plusMinutes(i))
                .build();
            chatMessageRepository.save(message);
        }

        // When
        List<ChatMessage> messages = chatMessageRepository.findByThreadIdOrderByTimestampDesc(testThread.getId());

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getContent()).isEqualTo("Message 3"); // Most recent
        assertThat(messages.get(1).getContent()).isEqualTo("Message 2");
        assertThat(messages.get(2).getContent()).isEqualTo("Message 1"); // Oldest
    }

    @Test
    @DisplayName("Should find messages with pagination")
    void testFindByThreadIdWithPagination() {
        // Given
        for (int i = 1; i <= 5; i++) {
            ChatMessage message = ChatMessage.builder()
                .thread(testThread)
                .role("user")
                .content("Message " + i)
                .timestamp(LocalDateTime.now().plusMinutes(i))
                .build();
            chatMessageRepository.save(message);
        }

        // When
        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> page = chatMessageRepository.findByThreadId(testThread.getId(), pageable);

        // Then
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Message 5");
        assertThat(page.getContent().get(1).getContent()).isEqualTo("Message 4");
    }

    @Test
    @DisplayName("Should find messages before specific timestamp")
    void testFindByThreadIdAndTimestampBefore() {
        // Given
        LocalDateTime baseTime = LocalDateTime.now();
        for (int i = 1; i <= 5; i++) {
            ChatMessage message = ChatMessage.builder()
                .thread(testThread)
                .role("user")
                .content("Message " + i)
                .timestamp(baseTime.plusMinutes(i))
                .build();
            chatMessageRepository.save(message);
        }

        // When
        LocalDateTime beforeTime = baseTime.plusMinutes(4);
        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> page = chatMessageRepository.findByThreadIdAndTimestampBefore(
            testThread.getId(), beforeTime, pageable
        );

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("Message 3");
        assertThat(page.getContent().get(1).getContent()).isEqualTo("Message 2");
    }

    @Test
    @DisplayName("Should find latest N messages")
    void testFindLatestMessagesByThreadId() {
        // Given
        for (int i = 1; i <= 5; i++) {
            ChatMessage message = ChatMessage.builder()
                .thread(testThread)
                .role("user")
                .content("Message " + i)
                .timestamp(LocalDateTime.now().plusMinutes(i))
                .build();
            chatMessageRepository.save(message);
        }

        // When
        List<ChatMessage> messages = chatMessageRepository.findLatestMessagesByThreadId(testThread.getId(), 3);

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getContent()).isEqualTo("Message 5");
        assertThat(messages.get(1).getContent()).isEqualTo("Message 4");
        assertThat(messages.get(2).getContent()).isEqualTo("Message 3");
    }

    @Test
    @DisplayName("Should count messages by thread ID")
    void testCountByThreadId() {
        // Given
        for (int i = 1; i <= 4; i++) {
            ChatMessage message = ChatMessage.builder()
                .thread(testThread)
                .role(i % 2 == 1 ? "user" : "assistant")
                .content("Message " + i)
                .build();
            chatMessageRepository.save(message);
        }

        // When
        long count = chatMessageRepository.countByThreadId(testThread.getId());

        // Then
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("Should find messages by role")
    void testFindByThreadIdAndRole() {
        // Given
        for (int i = 1; i <= 5; i++) {
            ChatMessage message = ChatMessage.builder()
                .thread(testThread)
                .role(i % 2 == 1 ? "user" : "assistant")
                .content("Message " + i)
                .timestamp(LocalDateTime.now().plusMinutes(i))
                .build();
            chatMessageRepository.save(message);
        }

        // When
        List<ChatMessage> userMessages = chatMessageRepository.findByThreadIdAndRole(testThread.getId(), "user");
        List<ChatMessage> assistantMessages = chatMessageRepository.findByThreadIdAndRole(testThread.getId(), "assistant");

        // Then
        assertThat(userMessages).hasSize(3);
        assertThat(assistantMessages).hasSize(2);
        assertThat(userMessages.get(0).getContent()).isEqualTo("Message 5");
        assertThat(assistantMessages.get(0).getContent()).isEqualTo("Message 4");
    }

    @Test
    @DisplayName("Should get timestamp in milliseconds")
    void testGetTimestampMillis() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = ChatMessage.builder()
            .thread(testThread)
            .role("user")
            .content("Test message")
            .timestamp(now)
            .build();
        message = chatMessageRepository.save(message);

        // When
        long timestampMillis = message.getTimestampMillis();

        // Then
        assertThat(timestampMillis).isGreaterThan(0);
        assertThat(timestampMillis).isLessThanOrEqualTo(System.currentTimeMillis());
    }

    @Test
    @DisplayName("Should handle JSONB metadata correctly")
    void testJsonbMetadata() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("model", "gemini");
        metadata.put("temperature", 0.7);
        metadata.put("maxTokens", 1000);
        
        Map<String, String> nestedData = new HashMap<>();
        nestedData.put("key1", "value1");
        nestedData.put("key2", "value2");
        metadata.put("nested", nestedData);

        ChatMessage message = ChatMessage.builder()
            .thread(testThread)
            .role("assistant")
            .content("AI response")
            .metadata(metadata)
            .build();

        // When
        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessage foundMessage = chatMessageRepository.findById(savedMessage.getId()).orElse(null);

        // Then
        assertThat(foundMessage).isNotNull();
        assertThat(foundMessage.getMetadata()).isNotNull();
        assertThat(foundMessage.getMetadata()).containsEntry("model", "gemini");
        assertThat(foundMessage.getMetadata()).containsEntry("temperature", 0.7);
        assertThat(foundMessage.getMetadata()).containsKey("nested");
    }
}