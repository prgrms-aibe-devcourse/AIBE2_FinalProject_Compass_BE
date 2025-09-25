package com.compass.domain.chat.route_optimization.service;

import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.route_optimization.entity.TravelItinerary;
import com.compass.domain.chat.route_optimization.entity.TravelPlace;
import com.compass.domain.chat.route_optimization.entity.TravelPlaceCandidate;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationRequest;
import com.compass.domain.chat.route_optimization.model.RouteOptimizationResponse;
import com.compass.domain.chat.route_optimization.repository.TravelItineraryRepository;
import com.compass.domain.chat.route_optimization.repository.TravelPlaceCandidateRepository;
import com.compass.domain.chat.route_optimization.repository.TravelPlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryPersistenceService {

    private final TravelItineraryRepository itineraryRepository;
    private final TravelPlaceRepository placeRepository;
    private final TravelPlaceCandidateRepository candidateRepository;
    private final ChatThreadRepository threadRepository;

    @Transactional
    public TravelItinerary saveItinerary(
        Long sessionId,
        RouteOptimizationRequest request,
        RouteOptimizationResponse response
    ) {
        log.info("일정 저장 시작: sessionId={}", sessionId);

        // 기존 활성 일정 비활성화
        itineraryRepository.findBySessionIdAndIsActiveTrue(sessionId)
            .ifPresent(existing -> {
                existing.deactivate();
                log.info("기존 일정 비활성화: id={}", existing.getId());
            });

        // ChatThread 조회 또는 생성
        ChatThread thread = findOrCreateThread(request.threadId());

        // 새 일정 생성
        TravelItinerary itinerary = TravelItinerary.builder()
            .thread(thread)
            .sessionId(sessionId)
            .startDate(request.startDate() != null ? request.startDate() : LocalDate.now())
            .endDate(calculateEndDate(request.startDate(), response.statistics().totalDays()))
            .totalDays(response.statistics().totalDays())
            .optimizationStrategy(request.optimizationStrategy())
            .transportMode(request.transportMode())
            .accommodationAddress(request.accommodationAddress())
            .totalDistance(response.statistics().totalDistance())
            .totalDuration(response.statistics().totalDuration())
            .isFinal(false)
            .isActive(true)
            .build();

        itinerary = itineraryRepository.save(itinerary);
        log.info("새 일정 생성: id={}", itinerary.getId());

        // AI 추천 장소 저장
        saveRecommendedPlaces(itinerary, response.aiRecommendedItinerary(), request);

        // 후보 장소 저장
        saveCandidatePlaces(itinerary, response.allCandidatePlaces());

        // 경로 정보 업데이트
        updateRouteInfo(itinerary, response.dailyRoutes());

        return itinerary;
    }

    @Transactional
    public TravelItinerary customizeItinerary(
        Long itineraryId,
        Map<Integer, List<String>> selectedPlaces,
        String transportMode
    ) {
        log.info("일정 커스터마이징: itineraryId={}", itineraryId);

        TravelItinerary itinerary = itineraryRepository.findById(itineraryId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다: " + itineraryId));

        // 기존 선택 장소 비활성화
        List<TravelPlace> existingPlaces = placeRepository.findSelectedPlacesByItineraryId(itineraryId);
        existingPlaces.forEach(place -> place.setIsSelected(false));

        // 새로 선택된 장소 활성화
        for (var entry : selectedPlaces.entrySet()) {
            Integer day = entry.getKey();
            List<String> placeNames = entry.getValue();

            // 후보에서 선택된 장소 찾기
            List<TravelPlaceCandidate> candidates = candidateRepository
                .findByItineraryIdAndDayNumber(itineraryId, day);

            int order = 1;
            for (String placeName : placeNames) {
                int finalOrder = order++;
                candidates.stream()
                    .filter(c -> c.getName().equals(placeName))
                    .findFirst()
                    .ifPresent(candidate -> {
                        // 후보를 실제 장소로 변환
                        TravelPlace place = candidate.toTravelPlace();
                        place.setVisitOrder(finalOrder);
                        place.setIsSelected(true);
                        // 헬퍼 메서드로 양방향 관계 설정 및 저장
                        itinerary.addPlace(place);
                        log.debug("장소 추가: day={}, name={}", day, placeName);
                    });
            }
        }

        // 이동수단 업데이트
        if (transportMode != null) {
            itinerary.setTransportMode(transportMode);
        }

        return itineraryRepository.save(itinerary);
    }

    @Transactional
    public void finalizeItinerary(Long itineraryId) {
        log.info("일정 확정: itineraryId={}", itineraryId);

        TravelItinerary itinerary = itineraryRepository.findById(itineraryId)
            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다: " + itineraryId));

        itinerary.finalize();
        itineraryRepository.save(itinerary);
    }

    private ChatThread findOrCreateThread(String threadId) {
        if (threadId == null) {
            // 새 스레드 생성
            ChatThread thread = new ChatThread();
            thread.setTitle("RouteOptimization Itinerary");
            return threadRepository.save(thread);
        }

        return threadRepository.findById(threadId)
            .orElseGet(() -> {
                ChatThread thread = new ChatThread();
                thread.setId(threadId);
                thread.setTitle("RouteOptimization Itinerary");
                return threadRepository.save(thread);
            });
    }

    private void saveRecommendedPlaces(
        TravelItinerary itinerary,
        Map<Integer, List<TourPlace>> recommendedPlaces,
        RouteOptimizationRequest request
    ) {
        for (var entry : recommendedPlaces.entrySet()) {
            Integer day = entry.getKey();
            List<TourPlace> places = entry.getValue();

            int order = 1;
            for (TourPlace tourPlace : places) {
                TravelPlace place = convertToEntity(tourPlace, null, day, order++);

                // OCR 확정 일정 체크
                boolean isOcrPlace = tourPlace.id() != null && tourPlace.id().startsWith("ocr_");
                if (isOcrPlace) {
                    place.setIsFixed(true);
                    place.setIsFromOcr(true);
                }

                // 헬퍼 메서드로 양방향 관계 설정 및 cascade 저장
                itinerary.addPlace(place);
            }
        }
    }

    private void saveCandidatePlaces(
        TravelItinerary itinerary,
        Map<Integer, List<TourPlace>> candidatePlaces
    ) {
        for (var entry : candidatePlaces.entrySet()) {
            Integer day = entry.getKey();
            List<TourPlace> places = entry.getValue();

            for (TourPlace tourPlace : places) {
                TravelPlaceCandidate candidate = convertToCandidate(tourPlace, null, day);
                // 헬퍼 메서드로 양방향 관계 설정 및 cascade 저장
                itinerary.addCandidate(candidate);
            }
        }
    }

    private void updateRouteInfo(
        TravelItinerary itinerary,
        Map<Integer, RouteOptimizationResponse.RouteInfo> routes
    ) {
        for (var entry : routes.entrySet()) {
            Integer day = entry.getKey();
            RouteOptimizationResponse.RouteInfo route = entry.getValue();

            List<TravelPlace> dayPlaces = placeRepository
                .findByItineraryIdAndDayNumber(itinerary.getId(), day);

            // 경로 세그먼트 정보 업데이트
            for (int i = 0; i < route.segments().size() && i < dayPlaces.size() - 1; i++) {
                RouteOptimizationResponse.RouteInfo.Segment segment = route.segments().get(i);
                TravelPlace place = dayPlaces.get(i + 1);

                place.setDistanceFromPrevious(segment.distance());
                place.setDurationFromPrevious(segment.duration());
                place.setTransportMode(segment.transport());
                placeRepository.save(place);
            }
        }
    }

    private TravelPlace convertToEntity(
        TourPlace tourPlace,
        TravelItinerary itinerary,  // 파라미터는 유지하지만 사용하지 않음 (backward compatibility)
        Integer dayNumber,
        Integer visitOrder
    ) {
        return TravelPlace.builder()
            // itinerary는 헬퍼 메서드에서 설정됨
            .placeId(tourPlace.id())
            .name(tourPlace.name())
            .timeBlock(tourPlace.timeBlock())
            .category(tourPlace.category())
            .address(tourPlace.address())
            .latitude(tourPlace.latitude())
            .longitude(tourPlace.longitude())
            .rating(tourPlace.rating())
            .priceLevel(tourPlace.priceLevel())
            .isTrendy(tourPlace.isTrendy())
            .petAllowed(tourPlace.petAllowed())
            .parkingAvailable(tourPlace.parkingAvailable())
            .dayNumber(dayNumber)
            .visitOrder(visitOrder)
            .isSelected(true)
            .build();
    }

    private TravelPlaceCandidate convertToCandidate(
        TourPlace tourPlace,
        TravelItinerary itinerary,  // 파라미터는 유지하지만 사용하지 않음 (backward compatibility)
        Integer dayNumber
    ) {
        return TravelPlaceCandidate.builder()
            // itinerary는 헬퍼 메서드에서 설정됨
            .placeId(tourPlace.id())
            .name(tourPlace.name())
            .timeBlock(tourPlace.timeBlock())
            .category(tourPlace.category())
            .address(tourPlace.address())
            .latitude(tourPlace.latitude())
            .longitude(tourPlace.longitude())
            .rating(tourPlace.rating())
            .priceLevel(tourPlace.priceLevel())
            .isTrendy(tourPlace.isTrendy())
            .petAllowed(tourPlace.petAllowed())
            .parkingAvailable(tourPlace.parkingAvailable())
            .dayNumber(dayNumber)
            .matchScore(tourPlace.rating())  // 평점을 매칭 점수로 사용
            .build();
    }

    private LocalDate calculateEndDate(LocalDate startDate, int totalDays) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        return startDate.plusDays(totalDays - 1);
    }
}