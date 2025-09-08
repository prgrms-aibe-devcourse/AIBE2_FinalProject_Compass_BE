package com.compass.domain.chat.entity;

import com.compass.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ChatThreadTest {

    private User testUser;
    private ChatThread thread;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .nickname("TestUser")
                .role(com.compass.domain.user.enums.Role.USER)
                .build();
    }

    @Test
    @DisplayName("새 스레드 생성 시 기본 제목은 '새 대화'여야 함")
    void testDefaultTitleIsKorean() {
        // Given & When
        thread = ChatThread.builder()
                .user(testUser)
                .build();
        thread.onCreate(); // Simulate @PrePersist

        // Then
        assertEquals("새 대화", thread.getTitle());
    }

    @Test
    @DisplayName("getLatestMessagePreview는 '새 대화' 제목일 때 '새 대화'를 반환해야 함")
    void testLatestMessagePreviewWithDefaultTitle() {
        // Given
        thread = ChatThread.builder()
                .user(testUser)
                .title("새 대화")
                .build();

        // When
        String preview = thread.getLatestMessagePreview();

        // Then
        assertEquals("새 대화", preview);
    }

    @Test
    @DisplayName("getLatestMessagePreview는 커스텀 제목이 있을 때 그 제목을 반환해야 함")
    void testLatestMessagePreviewWithCustomTitle() {
        // Given
        thread = ChatThread.builder()
                .user(testUser)
                .title("여행 계획 문의")
                .build();

        // When
        String preview = thread.getLatestMessagePreview();

        // Then
        assertEquals("여행 계획 문의", preview);
    }

    @Test
    @DisplayName("첫 메시지로 제목 업데이트 시 메시지 내용이 제목이 되어야 함")
    void testUpdateTitleFromFirstMessage() {
        // Given
        thread = ChatThread.builder()
                .user(testUser)
                .title("새 대화")
                .build();
        
        String firstMessage = "제주도 3박 4일 여행 추천해주세요";

        // When
        thread.updateTitleFromFirstMessage(firstMessage);

        // Then
        assertEquals(firstMessage, thread.getTitle());
    }

    @Test
    @DisplayName("긴 첫 메시지는 50자로 잘리고 '...'이 추가되어야 함")
    void testUpdateTitleFromLongFirstMessage() {
        // Given
        thread = ChatThread.builder()
                .user(testUser)
                .title("새 대화")
                .build();
        
        String longMessage = "안녕하세요! 저는 다음 달에 제주도로 3박 4일 여행을 계획하고 있는데요, 맛있는 음식점과 경치 좋은 관광지를 추천해주실 수 있나요? 특히 해산물을 좋아합니다.";

        // When
        thread.updateTitleFromFirstMessage(longMessage);

        // Then
        assertEquals(53, thread.getTitle().length()); // 50 chars + "..."
        assertTrue(thread.getTitle().endsWith("..."));
        assertEquals(longMessage.substring(0, 50), thread.getTitle().substring(0, 50));
    }

    @Test
    @DisplayName("빈 메시지로 제목 업데이트 시 제목이 변경되지 않아야 함")
    void testUpdateTitleWithEmptyMessage() {
        // Given
        thread = ChatThread.builder()
                .user(testUser)
                .title("새 대화")
                .build();

        // When
        thread.updateTitleFromFirstMessage("");
        
        // Then
        assertEquals("새 대화", thread.getTitle());
    }

    @Test
    @DisplayName("null 메시지로 제목 업데이트 시 제목이 변경되지 않아야 함")
    void testUpdateTitleWithNullMessage() {
        // Given
        thread = ChatThread.builder()
                .user(testUser)
                .title("새 대화")
                .build();

        // When
        thread.updateTitleFromFirstMessage(null);
        
        // Then
        assertEquals("새 대화", thread.getTitle());
    }
}