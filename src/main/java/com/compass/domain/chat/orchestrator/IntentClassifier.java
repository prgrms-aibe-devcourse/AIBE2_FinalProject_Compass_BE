package com.compass.domain.chat.orchestrator;

import com.compass.domain.chat.model.enums.Intent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Intent 분류기
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentClassifier {

    // 메시지로부터 Intent 분류
    public Intent classify(String message) {
        log.debug("Intent 분류 시작: {}", message);

        // 임시 구현 - 키워드 기반 분류
        if (message == null || message.isEmpty()) {
            return Intent.UNKNOWN;
        }

        var lowerMessage = message.toLowerCase();

        // 키워드 기반 Intent 분류
        if (lowerMessage.contains("여행") || lowerMessage.contains("계획")) {
            return Intent.TRAVEL_PLANNING;
        } else if (lowerMessage.contains("언제") || lowerMessage.contains("어디")) {
            return Intent.INFORMATION_COLLECTION;
        } else if (lowerMessage.contains("이미지") || lowerMessage.contains("사진")) {
            return Intent.IMAGE_UPLOAD;
        } else if (lowerMessage.contains("빠른") || lowerMessage.contains("간단")) {
            return Intent.QUICK_INPUT;
        } else if (lowerMessage.contains("검색") || lowerMessage.contains("찾")) {
            return Intent.DESTINATION_SEARCH;
        } else if (lowerMessage.contains("예약") || lowerMessage.contains("예매")) {
            return Intent.RESERVATION_PROCESSING;
        }

        // 기본값은 일반 질문
        return Intent.GENERAL_QUESTION;
    }
}