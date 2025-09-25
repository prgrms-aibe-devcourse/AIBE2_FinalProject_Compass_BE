package com.compass.domain.chat.model.dto;

import com.compass.domain.chat.model.enums.DocumentType;
import java.time.LocalDateTime;
import java.util.Map;

// OCR로 확인된 확정 일정 정보
public record ConfirmedSchedule(
        DocumentType documentType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String title,
        String location,
        String address,
        Map<String, String> details,  // 문서 타입별 세부 정보
        String originalText,  // OCR 원본 텍스트
        String imageUrl,
        boolean isFixed  // 변경 불가능 여부
) {
    public ConfirmedSchedule {
        if (documentType == null) {
            documentType = DocumentType.UNKNOWN;
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("일정 제목이 필요합니다.");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("시작 시간이 필요합니다.");
        }
        // 종료 시간이 없으면 시작 시간 + 1시간
        if (endTime == null) {
            endTime = startTime.plusHours(1);
        }
        if (details == null) {
            details = Map.of();
        }
        // 우선순위 1인 문서는 항상 고정
        isFixed = documentType.isFixed();
    }

    // 항공권 생성 헬퍼
    public static ConfirmedSchedule flight(
            LocalDateTime departure,
            LocalDateTime arrival,
            String flightNumber,
            String departureAirport,
            String arrivalAirport,
            String originalText,
            String imageUrl
    ) {
        return new ConfirmedSchedule(
                DocumentType.FLIGHT_RESERVATION,
                departure,
                arrival,
                "Flight " + flightNumber,
                departureAirport + " → " + arrivalAirport,
                departureAirport,
                Map.of(
                        "flightNumber", flightNumber,
                        "departure", departureAirport,
                        "arrival", arrivalAirport
                ),
                originalText,
                imageUrl,
                true
        );
    }

    // 호텔 예약 생성 헬퍼
    public static ConfirmedSchedule hotel(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            String hotelName,
            String address,
            String originalText,
            String imageUrl
    ) {
        return new ConfirmedSchedule(
                DocumentType.HOTEL_RESERVATION,
                checkIn,
                checkOut,
                hotelName,
                hotelName,
                address,
                Map.of("hotelName", hotelName),
                originalText,
                imageUrl,
                true
        );
    }

    // 공연/이벤트 생성 헬퍼
    public static ConfirmedSchedule event(
            LocalDateTime eventTime,
            String eventName,
            String venue,
            String seatInfo,
            String originalText,
            String imageUrl
    ) {
        return new ConfirmedSchedule(
                DocumentType.EVENT_TICKET,
                eventTime,
                eventTime.plusHours(3),  // 기본 3시간
                eventName,
                venue,
                venue,
                Map.of(
                        "venue", venue,
                        "seat", seatInfo != null ? seatInfo : ""
                ),
                originalText,
                imageUrl,
                true
        );
    }

    // 기차표 생성 헬퍼
    public static ConfirmedSchedule train(
            LocalDateTime departure,
            LocalDateTime arrival,
            String trainNumber,
            String departureStation,
            String arrivalStation,
            String seatInfo,
            String originalText,
            String imageUrl
    ) {
        return new ConfirmedSchedule(
                DocumentType.TRAIN_TICKET,
                departure,
                arrival,
                "Train " + trainNumber,
                departureStation + " → " + arrivalStation,
                departureStation,
                Map.of(
                        "trainNumber", trainNumber,
                        "departure", departureStation,
                        "arrival", arrivalStation,
                        "seat", seatInfo != null ? seatInfo : ""
                ),
                originalText,
                imageUrl,
                true
        );
    }

    // Phase 3에서 사용할 시간 충돌 체크
    public boolean hasTimeConflict(ConfirmedSchedule other) {
        if (other == null) {
            return false;
        }
        // 시간이 겹치는지 확인
        return !endTime.isBefore(other.startTime) && !startTime.isAfter(other.endTime);
    }

    // 우선순위 가져오기
    public int getPriority() {
        return documentType.getPriority();
    }
}