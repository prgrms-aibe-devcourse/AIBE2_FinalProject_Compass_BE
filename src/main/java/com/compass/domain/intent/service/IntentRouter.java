package com.compass.domain.intent.service;

import com.compass.domain.chat.service.FollowUpQuestionService;
import com.compass.domain.external.service.InformationService;
import com.compass.domain.intent.Intent;
import com.compass.domain.intent.IntentClassification;
import com.compass.domain.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRouter {

    private static final double CONFIDENCE_THRESHOLD = 0.7;

    private final TripService tripService;
    private final InformationService informationService;
    private final FollowUpQuestionService followUpQuestionService;

    public String route(IntentClassification classification, String message) {
        Intent intent = classification.intent();
        boolean isConfident = classification.isConfident(CONFIDENCE_THRESHOLD);

        log.info("[Intent Router] Routing intent: {}, Confident: {}", intent, isConfident);

        if (!isConfident && intent != Intent.UNKNOWN) {
            return followUpQuestionService.askForClarification(message, intent);
        }

        return switch (intent) {
            case TRAVEL -> {
                log.debug("Routing to TripService for travel planning.");
                // TODO: TripService에 planTrip(String message) 메서드 구현 후 연동 필요
                yield "[임시 응답] 여행 계획 기능이 호출되었습니다.";
            }
            case RECOMMENDATION -> {
                log.debug("Routing to TripService for recommendation.");
                // TODO: TripService에 getRecommendation(String message) 메서드 구현 후 연동 필요
                yield "[임시 응답] 추천 기능이 호출되었습니다.";
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
