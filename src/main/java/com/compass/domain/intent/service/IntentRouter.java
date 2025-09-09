package com.compass.domain.intent.service;

import com.compass.domain.chat.service.FollowUpQuestionService; // 가상 의존성
import com.compass.domain.external.service.InformationService; // 가상 의존성
import com.compass.domain.intent.Intent;
import com.compass.domain.intent.IntentClassification;
import com.compass.domain.trip.service.TripService; // 가상 의존성
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRouter {

    // CHAT1: 처리 플로우 결정을 위한 신뢰도 임계값
    private static final double CONFIDENCE_THRESHOLD = 0.7;

    // 각 도메인 서비스 의존성 주입
    private final TripService tripService;
    private final InformationService informationService;
    private final FollowUpQuestionService followUpQuestionService;

    /**
     * CHAT1: 의도 및 신뢰도 점수에 따라 처리 플로우를 결정하고, 적절한 서비스를 호출합니다.
     *
     * @param classification 의도 분류 결과 객체
     * @param message 원본 사용자 메시지
     * @return 각 도메인 서비스가 처리한 최종 응답 문자열
     */
    public String route(IntentClassification classification, String message) {
        Intent intent = classification.intent();
        boolean isConfident = classification.isConfident(CONFIDENCE_THRESHOLD);

        log.info("[Intent Router] Routing intent: {}, Confident: {}", intent, isConfident);

        if (!isConfident && intent != Intent.UNKNOWN) {
            // 신뢰도가 낮지만, 특정 의도로 추정될 경우 -> 꼬리 질문으로 의도 명확화 (CHAT2)
            log.debug("Confidence score is below threshold. Asking for clarification.");
            return followUpQuestionService.askForClarification(message, intent);
        }

        return switch (intent) {
            case TRAVEL -> {
                log.debug("Routing to TripService for travel planning.");
                yield tripService.planTrip(message);
            }
            case RECOMMENDATION -> {
                log.debug("Routing to TripService for recommendation.");
                yield tripService.getRecommendation(message);
            }
            case GENERAL -> {
                log.debug("Routing to InformationService for general info.");
                yield informationService.getRealtimeInfo(message);
            }
            case UNKNOWN -> {
                log.debug("Intent is UNKNOWN. Asking for clarification.");
                yield followUpQuestionService.askForClarification(message, null);
            }
        };
    }
}
