package com.compass.domain.chat.parser.impl;

import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.parser.DocumentParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 호텔 예약증 파싱
@Slf4j
@Component
public class HotelReservationParser implements DocumentParser {

    // 체크인/체크아웃 패턴
    private static final Pattern CHECK_IN = Pattern.compile(
            "(?:CHECK[- ]?IN|체크인)\\s*[:：]?\\s*(\\d{4}[년-]\\d{1,2}[월-]\\d{1,2}|\\d{1,2}\\s+[A-Z]{3}\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CHECK_OUT = Pattern.compile(
            "(?:CHECK[- ]?OUT|체크아웃)\\s*[:：]?\\s*(\\d{4}[년-]\\d{1,2}[월-]\\d{1,2}|\\d{1,2}\\s+[A-Z]{3}\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
    );

    // 호텔명 패턴
    private static final Pattern HOTEL_NAME = Pattern.compile(
            "(?:HOTEL|호텔|RESORT|리조트)\\s+([A-Za-z가-힣\\s]+?)(?:\\s*[,\\n]|$)",
            Pattern.CASE_INSENSITIVE
    );

    // 주소 패턴
    private static final Pattern ADDRESS = Pattern.compile(
            "(?:ADDRESS|주소)\\s*[:：]?\\s*([^\\n]+)",
            Pattern.CASE_INSENSITIVE
    );

    // 예약 번호 패턴
    private static final Pattern CONFIRMATION = Pattern.compile(
            "(?:CONFIRMATION|RESERVATION|예약번호)\\s*[:：]?\\s*([A-Z0-9]+)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public DocumentType getSupportedType() {
        return DocumentType.HOTEL_RESERVATION;
    }

    @Override
    public ConfirmedSchedule parse(OCRText ocrText, String imageUrl) {
        String text = ocrText.rawText();
        log.debug("호텔 예약증 파싱 시작 - textLength: {}", text.length());

        try {
            // 호텔명 추출
            String hotelName = extractHotelName(text);

            // 체크인/체크아웃 날짜 추출
            LocalDate checkInDate = extractDate(text, true);
            LocalDate checkOutDate = extractDate(text, false);

            if (checkInDate == null) {
                checkInDate = LocalDate.now().plusDays(1);
                log.warn("체크인 날짜를 찾을 수 없음 - 기본값 사용");
            }

            if (checkOutDate == null) {
                checkOutDate = checkInDate.plusDays(2);
                log.warn("체크아웃 날짜를 찾을 수 없음 - 기본값 사용");
            }

            // 체크인/아웃 시간 (일반적으로 15:00 / 11:00)
            LocalDateTime checkIn = LocalDateTime.of(checkInDate, LocalTime.of(15, 0));
            LocalDateTime checkOut = LocalDateTime.of(checkOutDate, LocalTime.of(11, 0));

            // 주소 추출
            String address = extractAddress(text);

            // 예약 번호 추출
            String confirmationNumber = extractConfirmation(text);

            // 일정 생성
            var schedule = ConfirmedSchedule.hotel(
                    checkIn,
                    checkOut,
                    hotelName,
                    address,
                    text,
                    imageUrl
            );

            // 예약 번호가 있으면 details에 추가
            if (confirmationNumber != null) {
                schedule.details().put("confirmationNumber", confirmationNumber);
            }

            return schedule;

        } catch (Exception e) {
            log.error("호텔 예약증 파싱 실패", e);
            // 최소 정보로 생성
            return new ConfirmedSchedule(
                    DocumentType.HOTEL_RESERVATION,
                    LocalDateTime.now().plusDays(1).withHour(15),
                    LocalDateTime.now().plusDays(3).withHour(11),
                    "Hotel",
                    "Hotel",
                    "",
                    null,
                    text,
                    imageUrl,
                    true
            );
        }
    }

    private String extractHotelName(String text) {
        // 먼저 호텔 키워드와 함께 찾기
        Matcher matcher = HOTEL_NAME.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            log.debug("호텔명 추출: {}", name);
            return name;
        }

        // 대문자로 된 호텔명 찾기 (예: HILTON, MARRIOTT 등)
        Pattern uppercasePattern = Pattern.compile(
                "\\b(HILTON|MARRIOTT|HYATT|SHERATON|INTERCONTINENTAL|WESTIN|" +
                "FOUR SEASONS|RITZ[- ]?CARLTON|CONRAD|W HOTEL)[^\\n]*",
                Pattern.CASE_INSENSITIVE
        );
        matcher = uppercasePattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }

        return "Hotel";
    }

    private LocalDate extractDate(String text, boolean isCheckIn) {
        Pattern pattern = isCheckIn ? CHECK_IN : CHECK_OUT;
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String dateStr = matcher.group(1);
            log.debug("{} 날짜 추출: {}", isCheckIn ? "체크인" : "체크아웃", dateStr);
            return parseDate(dateStr);
        }

        // 날짜만 찾기 (컨텍스트 없이)
        Pattern dateOnly = Pattern.compile(
                "(\\d{4})[년-](\\d{1,2})[월-](\\d{1,2})|" +
                "(\\d{1,2})\\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)\\s+(\\d{4})"
        );
        matcher = dateOnly.matcher(text);
        int count = 0;
        LocalDate date = null;
        while (matcher.find()) {
            if (isCheckIn && count == 0) {
                date = parseDate(matcher.group());
                break;
            } else if (!isCheckIn && count == 1) {
                date = parseDate(matcher.group());
                break;
            }
            count++;
        }

        return date;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            // YYYY-MM-DD 형식
            Pattern pattern1 = Pattern.compile("(\\d{4})[년-](\\d{1,2})[월-](\\d{1,2})");
            Matcher matcher = pattern1.matcher(dateStr);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            }

            // DD MMM YYYY 형식
            Pattern pattern2 = Pattern.compile("(\\d{1,2})\\s+([A-Z]{3})\\s+(\\d{4})");
            matcher = pattern2.matcher(dateStr.toUpperCase());
            if (matcher.find()) {
                int day = Integer.parseInt(matcher.group(1));
                int month = getMonthNumber(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            log.debug("날짜 파싱 실패: {} - {}", dateStr, e.getMessage());
        }
        return null;
    }

    private String extractAddress(String text) {
        Matcher matcher = ADDRESS.matcher(text);
        if (matcher.find()) {
            String address = matcher.group(1).trim();
            log.debug("주소 추출: {}", address);
            return address;
        }

        // 도시명이나 국가명 찾기
        Pattern cityPattern = Pattern.compile(
                "(?:Tokyo|Seoul|Bangkok|Singapore|Hong Kong|Shanghai|Beijing|" +
                "도쿄|서울|방콕|싱가포르|홍콩|상하이|베이징)",
                Pattern.CASE_INSENSITIVE
        );
        matcher = cityPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }

        return "";
    }

    private String extractConfirmation(String text) {
        Matcher matcher = CONFIRMATION.matcher(text);
        if (matcher.find()) {
            String confirmation = matcher.group(1);
            log.debug("예약번호 추출: {}", confirmation);
            return confirmation;
        }
        return null;
    }

    private int getMonthNumber(String monthStr) {
        return switch (monthStr) {
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