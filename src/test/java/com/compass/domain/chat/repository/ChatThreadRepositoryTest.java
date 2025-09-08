package com.compass.domain.chat.repository;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatThreadRepository
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("ChatThreadRepository Unit Tests")
class ChatThreadRepositoryTest {

    @Autowired
    private ChatThreadRepository chatThreadRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = User.builder()
            .email("test@example.com")
            .password("password")
            .nickname("TestUser")
            .role(Role.USER)
            .build();
        testUser = userRepository.save(testUser);

        anotherUser = User.builder()
            .email("another@example.com")
            .password("password")
            .nickname("AnotherUser")
            .role(Role.USER)
            .build();
        anotherUser = userRepository.save(anotherUser);
    }

    @Test
    @DisplayName("Should create and save a chat thread")
    void testCreateChatThread() {
        // Given
        ChatThread thread = ChatThread.builder()
            .user(testUser)
            .title("Test Chat")
            .build();

        // When
        ChatThread savedThread = chatThreadRepository.save(thread);

        // Then
        assertThat(savedThread).isNotNull();
        assertThat(savedThread.getId()).isNotNull();
        assertThat(savedThread.getUser()).isEqualTo(testUser);
        assertThat(savedThread.getTitle()).isEqualTo("Test Chat");
        assertThat(savedThread.getCreatedAt()).isNotNull();
        assertThat(savedThread.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find threads by user ID ordered by last message")
    void testFindByUserIdOrderByLastMessageAtDesc() {
        // Given
        ChatThread thread1 = ChatThread.builder()
            .user(testUser)
            .title("Chat 1")
            .build();
        thread1.setLastMessageAt(LocalDateTime.now().minusHours(2));
        chatThreadRepository.save(thread1);

        ChatThread thread2 = ChatThread.builder()
            .user(testUser)
            .title("Chat 2")
            .build();
        thread2.setLastMessageAt(LocalDateTime.now().minusHours(1));
        chatThreadRepository.save(thread2);

        ChatThread thread3 = ChatThread.builder()
            .user(testUser)
            .title("Chat 3")
            .build();
        thread3.setLastMessageAt(LocalDateTime.now());
        chatThreadRepository.save(thread3);

        // Create thread for another user (should not be included)
        ChatThread otherThread = ChatThread.builder()
            .user(anotherUser)
            .title("Other Chat")
            .build();
        chatThreadRepository.save(otherThread);

        // When
        List<ChatThread> threads = chatThreadRepository.findByUserIdOrderByLastMessageAtDesc(testUser.getId());

        // Then
        assertThat(threads).hasSize(3);
        assertThat(threads.get(0).getTitle()).isEqualTo("Chat 3"); // Most recent
        assertThat(threads.get(1).getTitle()).isEqualTo("Chat 2");
        assertThat(threads.get(2).getTitle()).isEqualTo("Chat 1"); // Oldest
    }

    @Test
    @DisplayName("Should find threads by user ID with pagination")
    void testFindByUserIdWithPagination() {
        // Given
        for (int i = 1; i <= 5; i++) {
            ChatThread thread = ChatThread.builder()
                .user(testUser)
                .title("Chat " + i)
                .build();
            chatThreadRepository.save(thread);
        }

        // When
        PageRequest pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatThread> page = chatThreadRepository.findByUserId(testUser.getId(), pageable);

        // Then
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find thread by ID and user ID")
    void testFindByIdAndUserId() {
        // Given
        ChatThread thread = ChatThread.builder()
            .user(testUser)
            .title("Test Chat")
            .build();
        thread = chatThreadRepository.save(thread);

        // When
        Optional<ChatThread> foundThread = chatThreadRepository.findByIdAndUserId(
            thread.getId(), testUser.getId()
        );

        // Then
        assertThat(foundThread).isPresent();
        assertThat(foundThread.get().getTitle()).isEqualTo("Test Chat");
    }

    @Test
    @DisplayName("Should not find thread with wrong user ID")
    void testFindByIdAndWrongUserId() {
        // Given
        ChatThread thread = ChatThread.builder()
            .user(testUser)
            .title("Test Chat")
            .build();
        thread = chatThreadRepository.save(thread);

        // When
        Optional<ChatThread> foundThread = chatThreadRepository.findByIdAndUserId(
            thread.getId(), anotherUser.getId()
        );

        // Then
        assertThat(foundThread).isEmpty();
    }

    @Test
    @DisplayName("Should check if thread exists for user")
    void testExistsByIdAndUserId() {
        // Given
        ChatThread thread = ChatThread.builder()
            .user(testUser)
            .title("Test Chat")
            .build();
        thread = chatThreadRepository.save(thread);

        // When & Then
        assertThat(chatThreadRepository.existsByIdAndUserId(thread.getId(), testUser.getId())).isTrue();
        assertThat(chatThreadRepository.existsByIdAndUserId(thread.getId(), anotherUser.getId())).isFalse();
        assertThat(chatThreadRepository.existsByIdAndUserId("invalid-id", testUser.getId())).isFalse();
    }

    @Test
    @DisplayName("Should count threads by user ID")
    void testCountByUserId() {
        // Given
        for (int i = 1; i <= 3; i++) {
            ChatThread thread = ChatThread.builder()
                .user(testUser)
                .title("Chat " + i)
                .build();
            chatThreadRepository.save(thread);
        }

        ChatThread otherThread = ChatThread.builder()
            .user(anotherUser)
            .title("Other Chat")
            .build();
        chatThreadRepository.save(otherThread);

        // When
        long count = chatThreadRepository.countByUserId(testUser.getId());

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle threads with null last message time")
    void testThreadsWithNullLastMessageTime() {
        // Given
        ChatThread thread1 = ChatThread.builder()
            .user(testUser)
            .title("Chat with message")
            .build();
        thread1.setLastMessageAt(LocalDateTime.now());
        chatThreadRepository.save(thread1);

        ChatThread thread2 = ChatThread.builder()
            .user(testUser)
            .title("Chat without message")
            .build();
        // lastMessageAt is null
        chatThreadRepository.save(thread2);

        // When
        List<ChatThread> threads = chatThreadRepository.findByUserIdOrderByLastMessageAtDesc(testUser.getId());

        // Then
        assertThat(threads).hasSize(2);
        assertThat(threads.get(0).getTitle()).isEqualTo("Chat with message");
        assertThat(threads.get(1).getTitle()).isEqualTo("Chat without message");
    }
}