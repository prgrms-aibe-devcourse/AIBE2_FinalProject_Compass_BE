package com.compass.domain.chat.service;

import com.compass.domain.auth.entity.User;
import com.compass.domain.auth.repository.UserRepository;
import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.repository.ChatMessageRepository;
import com.compass.domain.chat.repository.ChatThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 대화 스레드 관리, 메시지 저장/조회, 상태 관리를 담당하는 서비스
// 워크플로우: 대화 저장 관리자로서 ChatThread 생성/조회/업데이트, 대화 히스토리 관리를 담당.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatThreadService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // --- DTO 정의 ---

    // 메시지 저장 요청 DTO
    public record MessageSaveRequest(String threadId, String sender, String content) {}

    // 메시지 히스토리 응답 DTO
    public record MessageResponse(Long messageId, String sender, String content, LocalDateTime createdAt) {}


    // --- 서비스 메서드 ---

    // 새 대화 스레드 생성
    @Transactional
    public ChatThread createThread(Long userId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        ChatThread newThread = ChatThread.builder()
                .user(user)
                .title(title != null && !title.isBlank() ? title : "새 대화")
                .currentPhase("INITIALIZATION")
                .build();

        return chatThreadRepository.save(newThread);
    }

    // ChatThread 존재 확인 및 생성 (첫 대화 시작 시점에 호출)
    @Transactional
    public void ensureThreadExists(String threadId, String userIdOrEmail) {
        // Thread가 이미 존재하는지 확인
        if (chatThreadRepository.findById(threadId).isPresent()) {
            log.debug("✅ [TX] Thread already exists: {}", threadId);
            return;
        }

        log.info("🔧 [TX] Creating new thread: threadId={}, userIdOrEmail={}", threadId, userIdOrEmail);

        // 사용자 조회 - userId(숫자)로 먼저 시도, 실패하면 email로 시도
        User user = null;

        // 숫자 ID인지 확인
        try {
            Long userId = Long.parseLong(userIdOrEmail);
            user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                log.debug("✅ [TX] User found by ID: {}", userId);
            }
        } catch (NumberFormatException e) {
            // 숫자가 아니면 이메일로 간주
            log.debug("📧 [TX] Not a numeric ID, trying as email: {}", userIdOrEmail);
        }

        // ID로 못 찾았으면 email로 시도
        if (user == null) {
            user = userRepository.findByEmail(userIdOrEmail)
                    .orElseGet(() -> {
                        // 테스트 또는 개발 환경에서 기본 사용자 사용
                        log.warn("⚠️ [TX] User not found with ID/email: {}, using default user", userIdOrEmail);
                        return userRepository.findByEmail("testac@test.com")
                                .orElse(userRepository.findByEmail("test-user@test.com")
                                        .orElse(userRepository.findAll().stream().findFirst()
                                                .orElseThrow(() -> new IllegalStateException("No users found in database"))));
                    });
        }

        // 새 Thread 생성
        ChatThread newThread = ChatThread.builder()
                .id(threadId)
                .user(user)
                .title("새 대화")
                .currentPhase("INITIALIZATION")
                .build();

        ChatThread savedThread = chatThreadRepository.save(newThread);
        chatThreadRepository.flush(); // 강제로 DB에 즉시 반영
        log.info("✅ [TX] ChatThread created and flushed: threadId={}, userId={}, userEmail={}",
            savedThread.getId(), user.getId(), user.getEmail());
    }

    // 대화 메시지 저장 (Thread 없으면 자동 생성)
    @Transactional
    public ChatMessage saveMessage(MessageSaveRequest request) {
        log.debug("💾 [TX] saveMessage 시작 - threadId: {}, sender: {}", request.threadId(), request.sender());

        // Thread가 없으면 자동 생성 (UUID를 ID로 사용)
        ChatThread thread = chatThreadRepository.findById(request.threadId())
                .orElseGet(() -> {
                    log.warn("⚠️ [TX] Thread 없음, 자동 생성 시도 - threadId: {}", request.threadId());
                    // Thread ID로 사용자 정보 추출이 어려우므로 임시 처리
                    // 실제로는 request에 userId를 포함시키거나 SecurityContext에서 가져와야 함
                    User defaultUser = userRepository.findByEmail("test-user@test.com")
                            .orElse(userRepository.findAll().stream().findFirst()
                                    .orElseThrow(() -> new IllegalStateException("No users found in database")));

                    ChatThread newThread = ChatThread.builder()
                            .id(request.threadId())  // UUID를 그대로 ID로 사용
                            .user(defaultUser)
                            .title("새 대화")
                            .currentPhase("INITIALIZATION")
                            .build();

                    ChatThread saved = chatThreadRepository.save(newThread);
                    log.info("✅ [TX] Thread 자동 생성 완료 - threadId: {}", saved.getId());
                    return saved;
                });

        boolean hadMessages = thread.getMessages() != null && !thread.getMessages().isEmpty();

        ChatMessage message = ChatMessage.builder()
                .thread(thread)
                .role(request.sender())
                .content(request.content())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.debug("✅ [TX] 메시지 저장 완료 - messageId: {}, role: {}", savedMessage.getId(), savedMessage.getRole());

        thread.addMessage(savedMessage);

        if (!hadMessages && "user".equals(request.sender())) {
            thread.updateTitleFromFirstMessage(request.content());
            log.debug("📝 [TX] 제목 업데이트 - title: {}", thread.getTitle());
        }

        chatThreadRepository.save(thread);
        log.debug("✅ [TX] Thread 업데이트 완료 - threadId: {}, messageCount: {}",
            thread.getId(), thread.getMessages().size());

        return savedMessage;
    }

    // 특정 스레드의 대화 기록 조회
    public List<MessageResponse> getHistory(String threadId) {
        // findByThreadIdOrderByTimestampDesc는 최신순으로 가져오므로, 서비스에서는 시간순으로 변환하여 반환할 수 있습니다.
        // 또는 리포지토리에 시간 오름차순 메서드를 추가할 수 있습니다. 여기서는 stream().sorted()를 사용합니다.
        List<ChatMessage> messages = chatMessageRepository.findByThreadIdOrderByTimestampDesc(threadId);

        return messages.stream()
                .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp())) // 시간 오름차순으로 정렬
                .map(msg -> new MessageResponse(msg.getId(), msg.getRole(), msg.getContent(), msg.getTimestamp()))
                .collect(Collectors.toList());
    }
}
