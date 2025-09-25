package com.compass.domain.chat.parser.impl;

import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.parser.DocumentParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 항공권 파싱
@Slf4j
@Component
public class FlightReservationParser implements DocumentParser {

    // 날짜 패턴: 2024년 12월 25일, 2024-12-25, 25 DEC 2024 등
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{4})[년-](\\d{1,2})[월-](\\d{1,2})|" +
            "(\\d{1,2})\\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE
    );

    // 시간 패턴: 14:30, 2:30 PM 등
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "(\\d{1,2})[:：](\\d{2})\\s*(AM|PM)?|" +
            "(\\d{1,2})[:：](\\d{2})"
    );

    // 항공편 패턴: KE123, OZ456 등
    private static final Pattern FLIGHT_NUMBER = Pattern.compile(
            "\\b([A-Z]{2}\\s*\\d{2,4})\\b"
    );

    // 공항 코드 패턴: ICN, NRT, JFK 등
    private static final Pattern AIRPORT_CODE = Pattern.compile(
            "\\b([A-Z]{3})\\b"
    );

    // 출발/도착 패턴
    private static final Pattern DEPARTURE = Pattern.compile(
            "(?:DEPARTURE|FROM|출발)\\s*[:：]?\\s*([A-Z]{3}|[가-힣]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ARRIVAL = Pattern.compile(
            "(?:ARRIVAL|TO|도착)\\s*[:：]?\\s*([A-Z]{3}|[가-힣]+)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public DocumentType getSupportedType() {
        return DocumentType.FLIGHT_RESERVATION;
    }

    @Override
    public ConfirmedSchedule parse(OCRText ocrText, String imageUrl) {
        String text = ocrText.rawText();
        log.debug("항공권 파싱 시작 - textLength: {}", text.length());

        try {
            // 항공편명 추출
            String flightNumber = extractFlightNumber(text);

            // 날짜 추출
            LocalDate flightDate = extractDate(text);
            if (flightDate == null) {
                flightDate = LocalDate.now().plusDays(1);  // 기본값
                log.warn("날짜를 찾을 수 없음 - 기본값 사용");
            }

            // 시간 추출
            LocalTime departureTime = extractTime(text, "departure");
            LocalTime arrivalTime = extractTime(text, "arrival");

            if (departureTime == null) {
                departureTime = LocalTime.of(9, 0);  // 기본값
            }
            if (arrivalTime == null) {
                arrivalTime = departureTime.plusHours(2);  // 기본값
            }

            // 공항 추출
            String departureAirport = extractAirport(text, true);
            String arrivalAirport = extractAirport(text, false);

            // 일정 생성
            LocalDateTime departure = LocalDateTime.of(flightDate, departureTime);
            LocalDateTime arrival = LocalDateTime.of(flightDate, arrivalTime);

            // 도착이 다음날인 경우 처리
            if (arrival.isBefore(departure)) {
                arrival = arrival.plusDays(1);
            }

            return ConfirmedSchedule.flight(
                    departure,
                    arrival,
                    flightNumber,
                    departureAirport,
                    arrivalAirport,
                    text,
                    imageUrl
            );

        } catch (Exception e) {
            log.error("항공권 파싱 실패", e);
            // 최소 정보로 생성
            return new ConfirmedSchedule(
                    DocumentType.FLIGHT_RESERVATION,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(1).plusHours(2),
                    "Flight",
                    "Airport",
                    "",
                    null,
                    text,
                    imageUrl,
                    true
            );
        }
    }

    private String extractFlightNumber(String text) {
        Matcher matcher = FLIGHT_NUMBER.matcher(text);
        if (matcher.find()) {
            String flight = matcher.group(1).replaceAll("\\s+", "");
            log.debug("항공편 추출: {}", flight);
            return flight;
        }
        return "Unknown";
    }

    private LocalDate extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                if (matcher.group(1) != null) {
                    // YYYY-MM-DD 형식
                    int year = Integer.parseInt(matcher.group(1));
                    int month = Integer.parseInt(matcher.group(2));
                    int day = Integer.parseInt(matcher.group(3));
                    return LocalDate.of(year, month, day);
                } else if (matcher.group(4) != null) {
                    // DD MMM YYYY 형식
                    int day = Integer.parseInt(matcher.group(4));
                    String monthStr = matcher.group(5);
                    int year = Integer.parseInt(matcher.group(6));
                    int month = getMonthNumber(monthStr);
                    return LocalDate.of(year, month, day);
                }
            } catch (Exception e) {
                log.debug("날짜 파싱 실패: {}", e.getMessage());
            }
        }
        return null;
    }

    private LocalTime extractTime(String text, String type) {
        // 출발/도착 키워드 근처의 시간 찾기
        Pattern contextPattern = Pattern.compile(
                type.equalsIgnoreCase("departure") ?
                        "(?:DEPARTURE|출발)[^\\d]*(\\d{1,2}[:：]\\d{2})" :
                        "(?:ARRIVAL|도착)[^\\d]*(\\d{1,2}[:：]\\d{2})",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = contextPattern.matcher(text);
        if (matcher.find()) {
            String timeStr = matcher.group(1);
            try {
                String[] parts = timeStr.split("[:：]");
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                return LocalTime.of(hour, minute);
            } catch (Exception e) {
                log.debug("시간 파싱 실패: {}", e.getMessage());
            }
        }
        return null;
    }

    private String extractAirport(String text, boolean isDeparture) {
        Pattern pattern = isDeparture ? DEPARTURE : ARRIVAL;
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String airport = matcher.group(1);
            log.debug("{} 공항 추출: {}", isDeparture ? "출발" : "도착", airport);
            return airport;
        }

        // 공항 코드만 찾기
        Matcher codeMatcher = AIRPORT_CODE.matcher(text);
        if (isDeparture && codeMatcher.find()) {
            return codeMatcher.group(1);
        } else if (!isDeparture) {
            // 두 번째 공항 코드를 도착지로
            if (codeMatcher.find() && codeMatcher.find()) {
                return codeMatcher.group(1);
            }
        }

        return isDeparture ? "Departure" : "Arrival";
    }

    private int getMonthNumber(String monthStr) {
        return switch (monthStr.toUpperCase()) {
            case "JAN" -> 1;
            case "FEB" -> 2;
            case "MAR" -> 3;
            case "APR" -> 4;
            case "MAY" -> 5;
            case "JUN" -> 6;
            case "JUL" -> 7;
            case "AUG" -> 8;
            case "SEP" -> 9;
            case "OCT" -> 10;
            case "NOV" -> 11;
            case "DEC" -> 12;
            default -> 1;
        };
    }
}