package com.compass.domain.chat.service;

import com.compass.domain.auth.entity.User;
import com.compass.domain.auth.repository.UserRepository;
import com.compass.domain.chat.entity.ChatMessage;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.repository.ChatMessageRepository;
import com.compass.domain.chat.repository.ChatThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 대화 스레드 관리, 메시지 저장/조회, 상태 관리를 담당하는 서비스
// 워크플로우: 대화 저장 관리자로서 ChatThread 생성/조회/업데이트, 대화 히스토리 관리를 담당.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatThreadService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // --- DTO 정의 ---

    // 메시지 저장 요청 DTO
    public record MessageSaveRequest(String threadId, String sender, String content) {}

    // 스레드 생성 응답 DTO
    public record ThreadResponse(String threadId, LocalDateTime createdAt) {}

    // 메시지 히스토리 응답 DTO
    public record MessageResponse(Long messageId, String sender, String content, LocalDateTime createdAt) {}


    // --- 서비스 메서드 ---

    // 새 대화 스레드 생성
    @Transactional
    public ThreadResponse createThread(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        ChatThread newThread = ChatThread.builder()
                .user(user)
                .build();

        ChatThread savedThread = chatThreadRepository.save(newThread);
        return new ThreadResponse(savedThread.getId(), savedThread.getCreatedAt());
    }

    // 대화 메시지 저장
    @Transactional
    public void saveMessage(MessageSaveRequest request) {
        ChatThread thread = chatThreadRepository.findById(request.threadId())
                .orElseThrow(() -> new IllegalArgumentException("Thread not found with id: " + request.threadId()));

        ChatMessage message = ChatMessage.builder()
                .thread(thread)
                .role(request.sender())
                .content(request.content())
                .build();

        chatMessageRepository.save(message);
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
