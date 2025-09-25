package com.compass.domain.chat.function.processing.phase3.date_selection.service;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.DailyItinerary;
import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// OCR 확정 일정 통합 서비스
@Slf4j
@Service
public class OCRScheduleIntegrationService {

    // OCR 확정 일정을 날짜별 일정에 통합
    public Map<Integer, DailyItinerary> integrateOCRSchedules(
            Map<Integer, DailyItinerary> dailyItineraries,
            List<ConfirmedSchedule> ocrSchedules,
            LocalDate tripStartDate) {

        if (ocrSchedules == null || ocrSchedules.isEmpty()) {
            log.info("OCR 확정 일정 없음");
            return dailyItineraries;
        }

        log.info("OCR 확정 일정 {} 개 통합 시작", ocrSchedules.size());

        // 1. OCR 일정을 날짜별로 그룹화
        Map<Integer, List<ConfirmedSchedule>> ocrByDay = groupOCRByDay(ocrSchedules, tripStartDate);

        // 2. 각 날짜별로 OCR 일정 통합
        for (Map.Entry<Integer, List<ConfirmedSchedule>> entry : ocrByDay.entrySet()) {
            int day = entry.getKey();
            List<ConfirmedSchedule> dayOcrSchedules = entry.getValue();

            if (!dailyItineraries.containsKey(day)) {
                log.warn("Day {} 일정이 없어 OCR 일정 통합 불가", day);
                continue;
            }

            DailyItinerary itinerary = dailyItineraries.get(day);
            DailyItinerary updatedItinerary = integrateOCRIntoDay(itinerary, dayOcrSchedules);
            dailyItineraries.put(day, updatedItinerary);

            log.info("Day {}: OCR 일정 {} 개 통합 완료", day, dayOcrSchedules.size());
        }

        return dailyItineraries;
    }

    // OCR 일정을 날짜별로 그룹화
    private Map<Integer, List<ConfirmedSchedule>> groupOCRByDay(
            List<ConfirmedSchedule> ocrSchedules, LocalDate tripStartDate) {

        Map<Integer, List<ConfirmedSchedule>> ocrByDay = new HashMap<>();

        for (ConfirmedSchedule schedule : ocrSchedules) {
            LocalDate scheduleDate = schedule.startTime().toLocalDate();

            // 여행 시작일로부터 며칠째인지 계산
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(tripStartDate, scheduleDate);
            int dayNumber = (int) daysDiff + 1;

            if (dayNumber < 1) {
                log.warn("OCR 일정이 여행 시작 전: {}", schedule.title());
                continue;
            }

            ocrByDay.computeIfAbsent(dayNumber, k -> new ArrayList<>()).add(schedule);
        }

        return ocrByDay;
    }

    // 특정 날짜에 OCR 일정 통합
    private DailyItinerary integrateOCRIntoDay(
            DailyItinerary itinerary, List<ConfirmedSchedule> ocrSchedules) {

        List<TourPlace> places = new ArrayList<>(itinerary.places());

        // 1. OCR 일정을 TourPlace로 변환
        List<TourPlace> ocrPlaces = convertOCRToTourPlaces(ocrSchedules, itinerary.dayNumber());

        // 2. 시간 충돌 제거
        places = removeConflictingPlaces(places, ocrPlaces);

        // 3. OCR 일정 추가 (최우선순위)
        places.addAll(0, ocrPlaces);  // 맨 앞에 추가

        // 4. 시간대별로 정렬
        places = sortByTimeBlock(places);

        return itinerary.withPlaces(places);
    }

    // OCR 일정을 TourPlace로 변환
    private List<TourPlace> convertOCRToTourPlaces(
            List<ConfirmedSchedule> ocrSchedules, int dayNumber) {

        return ocrSchedules.stream()
            .map(schedule -> TourPlace.builder()
                .id("ocr_" + schedule.hashCode())
                .name(schedule.title())
                .timeBlock(determineTimeBlock(schedule.startTime()))
                .day(dayNumber)
                .recommendTime(formatTime(schedule.startTime(), schedule.endTime()))
                .latitude(null)  // OCR에서는 좌표 정보 없을 수 있음
                .longitude(null)
                .address(schedule.address() != null ? schedule.address() : schedule.location())
                .category(mapDocumentTypeToCategory(schedule.documentType()))
                .operatingHours("확정")
                .closedDays("N/A")
                .petAllowed(null)
                .parkingAvailable(null)
                .rating(5.0)  // OCR 확정 일정은 최고 우선순위
                .priceLevel("확정")
                .isTrendy(false)
                .build())
            .collect(Collectors.toList());
    }

    // 시간대로 TimeBlock 결정
    private String determineTimeBlock(LocalDateTime dateTime) {
        int hour = dateTime.getHour();

        if (hour >= 6 && hour < 10) return "BREAKFAST";
        else if (hour >= 10 && hour < 12) return "MORNING_ACTIVITY";
        else if (hour >= 12 && hour < 14) return "LUNCH";
        else if (hour >= 14 && hour < 16) return "CAFE";
        else if (hour >= 16 && hour < 18) return "AFTERNOON_ACTIVITY";
        else if (hour >= 18 && hour < 20) return "DINNER";
        else if (hour >= 20 && hour < 23) return "EVENING_ACTIVITY";
        else return "LATE_ACTIVITY";
    }

    // 시간 형식 지정
    private String formatTime(LocalDateTime start, LocalDateTime end) {
        return String.format("%02d:%02d-%02d:%02d",
            start.getHour(), start.getMinute(),
            end.getHour(), end.getMinute());
    }

    // DocumentType을 카테고리로 매핑
    private String mapDocumentTypeToCategory(com.compass.domain.chat.model.enums.DocumentType type) {
        return switch (type) {
            case FLIGHT_RESERVATION -> "교통(항공)";
            case HOTEL_RESERVATION -> "숙박";
            case TRAIN_TICKET -> "교통(기차)";
            case EVENT_TICKET -> "공연/이벤트";
            case RESTAURANT_RESERVATION -> "맛집(예약)";
            case ATTRACTION_TICKET -> "관광지(예약)";
            case CAR_RENTAL -> "교통(렌터카)";
            default -> "기타(확정)";
        };
    }

    // OCR 일정과 충돌하는 장소 제거
    private List<TourPlace> removeConflictingPlaces(
            List<TourPlace> places, List<TourPlace> ocrPlaces) {

        Set<String> ocrTimeBlocks = ocrPlaces.stream()
            .map(TourPlace::timeBlock)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // OCR이 차지한 시간대의 일반 장소 제거
        List<TourPlace> filtered = places.stream()
            .filter(place -> {
                // OCR 시간대와 충돌하면 제거
                if (ocrTimeBlocks.contains(place.timeBlock())) {
                    log.debug("시간 충돌로 제거: {} ({})", place.name(), place.timeBlock());
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        log.info("OCR 시간 충돌로 {} 개 장소 제거", places.size() - filtered.size());
        return filtered;
    }

    // 시간대별로 정렬
    private List<TourPlace> sortByTimeBlock(List<TourPlace> places) {
        Map<String, Integer> timeOrder = Map.of(
            "BREAKFAST", 1,
            "MORNING_ACTIVITY", 2,
            "LUNCH", 3,
            "CAFE", 4,
            "AFTERNOON_ACTIVITY", 5,
            "DINNER", 6,
            "EVENING_ACTIVITY", 7,
            "LATE_ACTIVITY", 8
        );

        return places.stream()
            .sorted((p1, p2) -> {
                Integer order1 = timeOrder.getOrDefault(p1.timeBlock(), 99);
                Integer order2 = timeOrder.getOrDefault(p2.timeBlock(), 99);
                return order1.compareTo(order2);
            })
            .collect(Collectors.toList());
    }

    // OCR 일정이 있는 시간대 확인
    public Set<String> getOCRTimeBlocks(
            List<ConfirmedSchedule> ocrSchedules, int dayNumber, LocalDate tripStartDate) {

        return ocrSchedules.stream()
            .filter(schedule -> {
                long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(
                    tripStartDate, schedule.startTime().toLocalDate());
                return (int) daysDiff + 1 == dayNumber;
            })
            .map(schedule -> determineTimeBlock(schedule.startTime()))
            .collect(Collectors.toSet());
    }
}