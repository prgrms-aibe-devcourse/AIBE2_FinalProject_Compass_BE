package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.dto.HotelReservation;
import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractHotelInfoFunction implements java.util.function.Function<OCRText, HotelReservation> {

    private static final Pattern HOTEL_NAME_PATTERN = Pattern.compile("HOTEL[:\u3002]?\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("ADDRESS[:\u3002]?\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHECK_IN_PATTERN = Pattern.compile("CHECK[- ]?IN[:\u3002]?\s*(\d{4}-\d{2}-\d{2}|\d{2}/\d{2}/\d{4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHECK_OUT_PATTERN = Pattern.compile("CHECK[- ]?OUT[:\u3002]?\s*(\d{4}-\d{2}-\d{2}|\d{2}/\d{2}/\d{4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROOM_PATTERN = Pattern.compile("ROOM[:\u3002]?\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GUEST_PATTERN = Pattern.compile("(GUESTS|ADULTS)[^0-9]*(\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIRMATION_PATTERN = Pattern.compile("(CONFIRMATION|RESERVATION)[^A-Z0-9]*([A-Z0-9]{5,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PATTERN = Pattern.compile("TOTAL[^0-9]*([0-9,.]+)");

    private final ProcessOCRFunction processOCRFunction;

    @PostConstruct
    void registerParser() {
        processOCRFunction.registerParser(DocumentType.HOTEL_RESERVATION, this);
    }

    @Override
    public HotelReservation apply(OCRText ocrText) {
        var text = ocrText.rawText();
        var hotelName = findFirst(HOTEL_NAME_PATTERN.matcher(text), 1);
        var address = findFirst(ADDRESS_PATTERN.matcher(text), 1);
        var checkIn = parseDate(CHECK_IN_PATTERN.matcher(text));
        var checkOut = parseDate(CHECK_OUT_PATTERN.matcher(text));
        var roomType = findFirst(ROOM_PATTERN.matcher(text), 1);
        var guests = parseInt(GUEST_PATTERN.matcher(text));
        var confirmation = findFirst(CONFIRMATION_PATTERN.matcher(text), 2);
        var totalPrice = parsePrice(PRICE_PATTERN.matcher(text));

        return new HotelReservation(
                hotelName,
                address,
                checkIn,
                checkOut,
                roomType,
                guests,
                confirmation,
                totalPrice,
                null,
                null
        );
    }

    private String findFirst(Matcher matcher, int group) {
        return matcher.find() ? matcher.group(group).trim() : "";
    }

    private LocalDate parseDate(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        var raw = matcher.group(1);
        for (var formatter : new DateTimeFormatter[]{DateTimeFormatter.ISO_DATE, DateTimeFormatter.ofPattern("MM/dd/yyyy")}) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // 다른 포맷 시도
            }
        }
        return null;
    }

    private Integer parseInt(Matcher matcher) {
        return matcher.find() ? Integer.parseInt(matcher.group(2)) : null;
    }

    private Double parsePrice(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        var raw = matcher.group(1).replace(",", "");
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            log.debug("금액 파싱 실패 - raw: {}", raw);
            return null;
        }
    }
}
