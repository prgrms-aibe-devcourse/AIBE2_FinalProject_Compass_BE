package com.compass.domain.chat.orchestrator.persistence;

import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.compass.domain.chat.repository.ChatThreadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

// JPA 기반 Phase 영구 저장소 구현체
@Slf4j
@Component
@RequiredArgsConstructor
public class JpaPhasePersistenceImpl implements PhasePersistence {

    private final ChatThreadRepository chatThreadRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<TravelPhase> findByThreadId(String threadId) {
        return chatThreadRepository.findById(threadId)
                .map(ChatThread::getCurrentPhase)
                .filter(phase -> phase != null && !phase.isEmpty())
                .map(TravelPhase::valueOf);
    }

    @Override
    @Transactional
    public void save(String threadId, TravelPhase phase) {
        chatThreadRepository.findById(threadId).ifPresent(thread -> {
            thread.setCurrentPhase(phase.name());
            thread.setPhaseUpdatedAt(LocalDateTime.now());
            chatThreadRepository.save(thread);
            log.info("DB에 Phase 저장: threadId={}, phase={}", threadId, phase);
        });
    }

    @Override
    @Transactional
    public void deleteByThreadId(String threadId) {
        chatThreadRepository.findById(threadId).ifPresent(thread -> {
            thread.setCurrentPhase(TravelPhase.INITIALIZATION.name());
            thread.setPhaseUpdatedAt(LocalDateTime.now());
            chatThreadRepository.save(thread);
            log.info("Phase 초기화: threadId={}", threadId);
        });
    }
}