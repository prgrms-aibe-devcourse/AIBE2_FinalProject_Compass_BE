package com.compass.domain.chat.stage3.service;

import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.route_optimization.entity.TravelItinerary;
import com.compass.domain.chat.route_optimization.repository.TravelItineraryRepository;
import com.compass.domain.chat.stage3.dto.DailyItinerary;
import com.compass.domain.chat.stage3.dto.Stage3Output;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class Stage3PersistenceService {

    private final TravelItineraryRepository itineraryRepository;
    private final ChatThreadRepository threadRepository;

    @Transactional
    public TravelItinerary saveItinerary(TravelContext context, Stage3Output stage3Output) {
        log.info("💾 Stage 3 일정 저장 시작");

        // ChatThread 조회
        String threadId = context.getThreadId();
        ChatThread thread = threadRepository.findById(threadId)
            .orElseThrow(() -> new IllegalArgumentException("ChatThread를 찾을 수 없습니다: " + threadId));

        // 기존 활성 일정 비활성화
        itineraryRepository.findLatestActiveByThreadId(threadId)
            .ifPresent(existing -> {
                existing.deactivate();
                log.info("기존 일정 비활성화: id={}", existing.getId());
            });

        // 날짜 추출
        LocalDate startDate = extractStartDate(context);
        LocalDate endDate = extractEndDate(context, stage3Output);

        // 전체 여행 기간 계산
        int totalDays = stage3Output.getDailyItineraries() != null ?
            stage3Output.getDailyItineraries().size() : 0;

        // 이동수단 추출
        String transportMode = (String) context.getCollectedInfo().get(TravelContext.KEY_TRANSPORTATION_TYPE);

        // 새 일정 생성
        TravelItinerary itinerary = TravelItinerary.builder()
            .thread(thread)
            .sessionId(System.currentTimeMillis()) // 임시 sessionId
            .startDate(startDate)
            .endDate(endDate)
            .totalDays(totalDays)
            .transportMode(transportMode)
            .totalDistance(stage3Output.getTotalDistance())
            .totalDuration((int) stage3Output.getTotalDuration())
            .isFinal(false)
            .isActive(true)
            .build();

        // 일정의 장소들 미리 추가 (cascade 작동을 위해)
        addPlacesToItinerary(itinerary, stage3Output.getDailyItineraries());

        // 한 번에 저장 (cascade로 places도 함께 저장됨)
        itinerary = itineraryRepository.save(itinerary);
        log.info("💾 Stage 3 일정 저장 완료 - Itinerary ID: {}, 총 {}일, 장소 {}개",
                itinerary.getId(), totalDays, itinerary.getPlaces().size());

        return itinerary;
    }

    private void addPlacesToItinerary(TravelItinerary itinerary, List<DailyItinerary> dailyItineraries) {
        if (dailyItineraries == null || dailyItineraries.isEmpty()) {
            return;
        }

        int totalPlaces = 0;

        for (DailyItinerary daily : dailyItineraries) {
            int dayNumber = daily.getDayNumber();

            // 시간 블록별 장소 저장
            if (daily.getTimeBlocks() != null && !daily.getTimeBlocks().isEmpty()) {
                int visitOrder = 1;

                for (Map.Entry<String, List<TravelPlace>> entry : daily.getTimeBlocks().entrySet()) {
                    String timeBlock = entry.getKey();
                    List<TravelPlace> places = entry.getValue();

                    for (TravelPlace place : places) {
                        com.compass.domain.chat.route_optimization.entity.TravelPlace entity =
                            convertToEntity(place, dayNumber, timeBlock, visitOrder++);

                        itinerary.addPlace(entity);
                        totalPlaces++;
                    }
                }
            } else if (daily.getPlaces() != null) {
                // places 리스트가 있으면 사용
                int visitOrder = 1;
                for (TravelPlace place : daily.getPlaces()) {
                    com.compass.domain.chat.route_optimization.entity.TravelPlace entity =
                        convertToEntity(place, dayNumber, null, visitOrder++);

                    itinerary.addPlace(entity);
                    totalPlaces++;
                }
            }
        }

        log.info("💾 총 {}개 장소 추가 완료 (cascade로 저장 대기 중)", totalPlaces);
    }

    private com.compass.domain.chat.route_optimization.entity.TravelPlace convertToEntity(
            TravelPlace place,
            int dayNumber,
            String timeBlock,
            int visitOrder) {

        // priceLevel 변환 (Integer → String)
        String priceLevelStr = null;
        if (place.getPriceLevel() != null) {
            int level = place.getPriceLevel();
            priceLevelStr = "$".repeat(Math.max(1, Math.min(level, 4)));
        }

        return com.compass.domain.chat.route_optimization.entity.TravelPlace.builder()
            .placeId(place.getPlaceId())
            .name(place.getName())
            .timeBlock(timeBlock)
            .category(place.getCategory())
            .address(place.getAddress())
            .latitude(place.getLatitude())
            .longitude(place.getLongitude())
            .rating(place.getRating())
            .priceLevel(priceLevelStr)
            .dayNumber(dayNumber)
            .visitOrder(visitOrder)
            .isSelected(true)
            .isFixed(place.getIsFixed() != null && place.getIsFixed())
            .isFromOcr(false)
            .build();
    }

    private LocalDate extractStartDate(TravelContext context) {
        Object startDateObj = context.getCollectedInfo().get(TravelContext.KEY_START_DATE);
        if (startDateObj instanceof LocalDate) {
            return (LocalDate) startDateObj;
        }
        if (startDateObj instanceof String) {
            try {
                return LocalDate.parse((String) startDateObj);
            } catch (Exception e) {
                log.warn("Failed to parse start date: {}", startDateObj);
            }
        }
        return LocalDate.now();
    }

    private LocalDate extractEndDate(TravelContext context, Stage3Output stage3Output) {
        Object endDateObj = context.getCollectedInfo().get(TravelContext.KEY_END_DATE);
        if (endDateObj instanceof LocalDate) {
            return (LocalDate) endDateObj;
        }
        if (endDateObj instanceof String) {
            try {
                return LocalDate.parse((String) endDateObj);
            } catch (Exception e) {
                log.warn("Failed to parse end date: {}", endDateObj);
            }
        }

        // 종료일을 계산할 수 없으면 시작일 + 일정 수로 계산
        LocalDate startDate = extractStartDate(context);
        int totalDays = stage3Output.getDailyItineraries() != null ?
            stage3Output.getDailyItineraries().size() : 1;
        return startDate.plusDays(totalDays - 1);
    }
}