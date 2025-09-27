package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.service.enrichment.EnrichmentUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Gemini를 활용해 장소 설명을 재생성하는 서비스
@Slf4j
@Service
public class GeminiDescriptionService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final ChatModel chatModel;

    public GeminiDescriptionService(
        TravelCandidateRepository travelCandidateRepository,
        @Autowired(required = false) ChatModel chatModel
    ) {
        this.travelCandidateRepository = travelCandidateRepository;
        this.chatModel = chatModel;
    }

    // 장소 설명을 Gemini로 재생성 (기존 내용 포함 덮어쓰기)
    public int regenerateDescriptions(boolean onlyIfEmpty) {
        if (chatModel == null) {
            log.warn("ChatModel이 설정되지 않아 Gemini 설명 재생성 불가");
            return 0;
        }

        List<TravelCandidate> candidates = travelCandidateRepository.findAll();
        if (candidates.isEmpty()) {
            log.info("설명 재생성 대상이 없습니다.");
            return 0;
        }

        AtomicInteger updated = new AtomicInteger(0);

        candidates.forEach(candidate -> {
            try {
                if (onlyIfEmpty && candidate.getDescription() != null && !candidate.getDescription().isBlank()) {
                    return;
                }

                String description = generateDescription(candidate);
                if (description != null && !description.isBlank()) {
                    candidate.setDescription(description);
                    candidate.setAiEnriched(Boolean.TRUE);
                    travelCandidateRepository.save(candidate);
                    updated.incrementAndGet();
                }

                Thread.sleep(100); // API 호출 간 간격 유지

            } catch (Exception e) {
                log.error("Gemini 설명 재생성 실패 - {}: {}", candidate.getName(), e.getMessage());
            }
        });

        log.info("Gemini 설명 재생성 완료 - 업데이트된 장소: {}개", updated.get());
        return updated.get();
    }

    private String generateDescription(TravelCandidate candidate) {
        try {
            String systemPrompt = "당신은 한국 여행 큐레이터입니다. 여행객에게 장소를 친근하게 소개하세요.";
            String userPrompt = String.format("""
                다음 장소를 한국어로 2~3문장으로 소개해주세요.
                - 장소명: %s
                - 지역: %s
                - 카테고리: %s
                - 주소: %s
                방문하면 좋은 이유 두 가지를 포함하되, 과장된 표현은 피하고 누구나 이해하기 쉬운 어조로 작성하세요.
                """,
                safeValue(candidate.getName()),
                safeValue(candidate.getRegion()),
                safeValue(candidate.getCategory()),
                safeValue(candidate.getAddress())
            );

            var prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            ));

            var response = chatModel.call(prompt);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return null;
            }

            String content = response.getResult().getOutput().getContent();
            if (content == null) {
                return null;
            }

            content = content.trim();
            if (content.isEmpty()) {
                return null;
            }

            content = content.replaceAll("^[\"'`]+|[\"'`]+$", "").trim();
            return EnrichmentUtils.truncateString(content, 600);

        } catch (Exception e) {
            log.error("Gemini 설명 생성 실패 - {}: {}", candidate.getName(), e.getMessage());
            return null;
        }
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "정보 없음" : value;
    }
}

