package com.compass.domain.chat.function.processing.phase3.date_selection.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.DailyItinerary;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.llm.LLMReviewResponse;
import com.compass.domain.chat.function.processing.phase3.date_selection.util.DistanceUtils;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.orchestrator.ContextManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// LLM 리뷰 전용 서비스 - 단일 책임 원칙
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMReviewService {

    private final ChatModel chatModel;
    private final ContextManager contextManager;
    private final ObjectMapper objectMapper;

    private static final double WARNING_DAILY_DISTANCE = 20.0;

    // LLM 검수가 필요한지 판단
    public boolean needsReview(Map<Integer, DailyItinerary> itineraries) {
        for (DailyItinerary itinerary : itineraries.values()) {
            // 1. 하루 이동거리가 너무 긴 경우
            if (itinerary.totalDistance() > WARNING_DAILY_DISTANCE) {
                log.warn("Day {} 이동거리 {}km - LLM 검수 필요",
                    itinerary.dayNumber(), itinerary.totalDistance());
                return true;
            }

            // 2. 필수 시간 블록이 누락된 경우
            Set<String> timeBlocks = itinerary.places().stream()
                .map(TourPlace::timeBlock)
                .collect(Collectors.toSet());

            if (!timeBlocks.contains("LUNCH") || !timeBlocks.contains("DINNER")) {
                log.warn("Day {} 식사 시간 누락 - LLM 검수 필요",
                    itinerary.dayNumber());
                return true;
            }

            // 3. 장소가 너무 적거나 많은 경우
            int placeCount = itinerary.places().size();
            if (placeCount < 4 || placeCount > 10) {
                log.warn("Day {} 장소 수 {} - LLM 검수 필요",
                    itinerary.dayNumber(), placeCount);
                return true;
            }
        }
        return false;
    }

    // LLM 검수 실행
    public Map<Integer, DailyItinerary> reviewAndAdjust(
            Map<Integer, DailyItinerary> itineraries,
            String threadId) {

        try {
            Optional<TravelContext> contextOpt = contextManager.getContext(threadId, "mockUser");
            TravelContext context = contextOpt.orElseGet(() -> createMockContext(threadId));

            String prompt = buildReviewPrompt(itineraries, context);
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String responseContent = response.getResult().getOutput().getContent();

            LLMReviewResponse reviewResponse = parseResponse(responseContent);

            if (reviewResponse.isNeedsAdjustment()) {
                log.info("LLM 제안 적용: {}", reviewResponse.getReason());
                return applyAdjustments(itineraries, reviewResponse);
            }

            return itineraries;

        } catch (Exception e) {
            log.error("LLM 검수 중 오류 발생, 원본 일정 유지", e);
            return itineraries;
        }
    }

    // LLM 응답 파싱
    private LLMReviewResponse parseResponse(String responseContent) {
        try {
            int startIdx = responseContent.indexOf("{");
            int endIdx = responseContent.lastIndexOf("}") + 1;
            if (startIdx >= 0 && endIdx > startIdx) {
                String jsonPart = responseContent.substring(startIdx, endIdx);
                return objectMapper.readValue(jsonPart, LLMReviewResponse.class);
            }
        } catch (Exception e) {
            log.warn("LLM 응답 파싱 실패: {}", e.getMessage());
        }

        return LLMReviewResponse.builder()
            .needsAdjustment(false)
            .reason("파싱 실패")
            .suggestions(new ArrayList<>())
            .build();
    }

    // LLM 제안 적용
    private Map<Integer, DailyItinerary> applyAdjustments(
            Map<Integer, DailyItinerary> itineraries,
            LLMReviewResponse response) {

        for (LLMReviewResponse.LLMSuggestion suggestion : response.getSuggestions()) {
            DailyItinerary itinerary = itineraries.get(suggestion.getDay());
            if (itinerary == null) continue;

            switch (suggestion.getType()) {
                case "MOVE" -> movePlace(itineraries, suggestion);
                case "REMOVE" -> removePlace(itinerary, suggestion.getPlace());
                case "SWAP" -> swapPlaces(itinerary, suggestion);
            }
        }

        // 조정 후 거리 재계산
        recalculateDistances(itineraries);
        return itineraries;
    }

    // 거리 재계산
    private void recalculateDistances(Map<Integer, DailyItinerary> itineraries) {
        var updatedItineraries = new HashMap<Integer, DailyItinerary>();
        for (var entry : itineraries.entrySet()) {
            var itinerary = entry.getValue();
            double newDistance = DistanceUtils.calculateTotalDistance(itinerary.places());
            var updatedItinerary = itinerary.withTotalDistance(newDistance);
            updatedItineraries.put(entry.getKey(), updatedItinerary);
            log.info("Day {} 조정 완료: 새 이동거리 {}km",
                updatedItinerary.dayNumber(), String.format("%.1f", newDistance));
        }
        itineraries.putAll(updatedItineraries);
    }

    // 프롬프트 생성
    @SuppressWarnings("unchecked")
    private String buildReviewPrompt(Map<Integer, DailyItinerary> itineraries,
                                     TravelContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("여행 일정을 검토하고 문제가 있으면 조정 제안을 해주세요.\n\n");
        prompt.append("[여행 정보]\n");

        List<String> destinations = (List<String>) context.getCollectedInfo()
            .getOrDefault(TravelContext.KEY_DESTINATIONS, List.of("서울"));
        prompt.append("- 목적지: ").append(destinations).append("\n");
        prompt.append("- 기간: ").append(itineraries.size()).append("일\n");

        List<String> travelStyle = (List<String>) context.getCollectedInfo()
            .getOrDefault(TravelContext.KEY_TRAVEL_STYLE, List.of("편안한"));
        prompt.append("- 여행 스타일: ").append(travelStyle).append("\n\n");

        prompt.append("[생성된 일정]\n");
        prompt.append(formatItineraries(itineraries)).append("\n\n");

        prompt.append("[체크 포인트]\n");
        prompt.append("1. 하루 이동거리가 20km를 넘지 않는가?\n");
        prompt.append("2. 점심(12-14시)/저녁(18-20시) 시간에 식당이 있는가?\n");
        prompt.append("3. 같은 카테고리가 연속으로 나오지 않는가?\n");
        prompt.append("4. 실제 동선이 자연스러운가?\n");
        prompt.append("5. 지역 간 이동이 효율적인가?\n\n");

        prompt.append("[응답 형식]\n");
        prompt.append("""
            {
              "needsAdjustment": true/false,
              "reason": "조정이 필요한 이유",
              "suggestions": [
                {
                  "day": 1,
                  "type": "MOVE/REMOVE/SWAP",
                  "place": "장소명",
                  "action": "구체적인 조정 내용"
                }
              ]
            }
            """);

        return prompt.toString();
    }

    // 일정 포맷팅
    private String formatItineraries(Map<Integer, DailyItinerary> itineraries) {
        StringBuilder sb = new StringBuilder();
        for (var entry : itineraries.entrySet()) {
            var itinerary = entry.getValue();
            sb.append("\nDay ").append(itinerary.dayNumber())
              .append(" (이동거리: ").append(String.format("%.1f", itinerary.totalDistance()))
              .append("km):\n");

            for (TourPlace place : itinerary.places()) {
                sb.append("- ").append(place.recommendTime())
                  .append(" [").append(place.timeBlock()).append("] ")
                  .append(place.name())
                  .append(" (").append(place.category()).append(")\n");
            }
        }
        return sb.toString();
    }

    // 장소 이동
    private void movePlace(Map<Integer, DailyItinerary> itineraries,
                          LLMReviewResponse.LLMSuggestion suggestion) {
        var sourceDay = itineraries.get(suggestion.getDay());
        var targetDay = itineraries.get(suggestion.getTargetDay());

        if (sourceDay != null && targetDay != null) {
            var placeToMove = sourceDay.places().stream()
                .filter(p -> p.name().equals(suggestion.getPlace()))
                .findFirst()
                .orElse(null);

            if (placeToMove != null) {
                sourceDay.places().remove(placeToMove);
                targetDay.places().add(placeToMove);
                log.info("장소 이동: {} (Day {} → Day {})",
                    placeToMove.name(), suggestion.getDay(), suggestion.getTargetDay());
            }
        }
    }

    // 장소 제거
    private void removePlace(DailyItinerary itinerary, String placeName) {
        itinerary.places().removeIf(p -> p.name().equals(placeName));
        log.info("장소 제거: {}", placeName);
    }

    // 장소 순서 변경
    private void swapPlaces(DailyItinerary itinerary,
                           LLMReviewResponse.LLMSuggestion suggestion) {
        var places = itinerary.places();
        int idx1 = -1, idx2 = -1;

        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).name().equals(suggestion.getPlace())) idx1 = i;
            if (places.get(i).name().equals(suggestion.getSwapWith())) idx2 = i;
        }

        if (idx1 >= 0 && idx2 >= 0) {
            Collections.swap(places, idx1, idx2);
            log.info("장소 순서 변경: {} <-> {}",
                suggestion.getPlace(), suggestion.getSwapWith());
        }
    }

    // Mock 컨텍스트 생성
    private TravelContext createMockContext(String threadId) {
        var context = TravelContext.builder()
            .threadId(threadId)
            .userId("mockUser")
            .build();

        context.updateCollectedInfo(TravelContext.KEY_DESTINATIONS, List.of("서울"));
        context.updateCollectedInfo(TravelContext.KEY_TRAVEL_STYLE, List.of("편안한", "문화"));
        context.updateCollectedInfo(TravelContext.KEY_COMPANIONS, "친구");

        return context;
    }
}