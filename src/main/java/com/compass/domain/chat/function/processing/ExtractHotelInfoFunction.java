package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.dto.HotelReservation;
import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.service.HotelGeocodingService;
import com.compass.domain.chat.service.HotelReservationService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractHotelInfoFunction implements java.util.function.Function<OCRText, HotelReservation> {

    private static final String DATE_REGEX = "\\d{4}[-/.]\\d{2}[-/.]\\d{2}|\\d{2}/\\d{2}/\\d{4}|\\d{1,2} [A-Za-z]{3,9} \\d{4}|[A-Za-z]{3,9} \\d{1,2}, \\d{4}";

    private static final Pattern HOTEL_NAME_PATTERN = Pattern.compile("HOTEL(?: NAME)?[:\u3002]?\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(?:ADDRESS|LOCATION)[:\u3002]?\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHECK_IN_PATTERN = Pattern.compile("CHECK[- ]?IN(?: DATE)?[:\u3002]?\\s*(" + DATE_REGEX + ")", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHECK_OUT_PATTERN = Pattern.compile("CHECK[- ]?OUT(?: DATE)?[:\u3002]?\\s*(" + DATE_REGEX + ")", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROOM_PATTERN = Pattern.compile("ROOM[:\u3002]?\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GUEST_PATTERN = Pattern.compile("(GUESTS|ADULTS)[^0-9]*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIRMATION_PATTERN = Pattern.compile("(CONFIRMATION|RESERVATION)[^A-Z0-9]*([A-Z0-9]{5,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PATTERN = Pattern.compile("TOTAL[^0-9]*([0-9,.]+)");
    private static final Pattern NIGHTS_PATTERN = Pattern.compile("(\\d+)\\s*nights?", Pattern.CASE_INSENSITIVE);

    private final ProcessOCRFunction processOCRFunction;
    private final HotelReservationService hotelReservationService;
    private final HotelGeocodingService hotelGeocodingService;

    @PostConstruct
    void registerParser() {
        processOCRFunction.registerParser(DocumentType.HOTEL_RESERVATION, this);
    }

    @Override
    public HotelReservation apply(OCRText ocrText) {
        var text = ocrText.rawText();
        var hotelName = extractHotelName(text);
        var address = extractAddress(text);
        var checkIn = parseDate(CHECK_IN_PATTERN.matcher(text));
        var checkOut = parseDate(CHECK_OUT_PATTERN.matcher(text));
        var roomType = findFirst(ROOM_PATTERN.matcher(text), 1);
        var guests = parseInt(GUEST_PATTERN.matcher(text));
        var confirmation = findFirst(CONFIRMATION_PATTERN.matcher(text), 2);
        var totalPrice = parsePrice(PRICE_PATTERN.matcher(text));

        var nights = parseNights(text, checkIn, checkOut);
        if (checkIn != null && checkOut == null && nights != null) {
            checkOut = checkIn.plusDays(nights);
        }
        if (checkIn != null && checkOut != null && nights == null) {
            nights = safeDaysBetween(checkIn, checkOut);
        }

        var coordinates = lookupCoordinates(address, hotelName);

        var reservation = new HotelReservation(
                hotelName,
                address,
                checkIn,
                null,  // checkInTime - default handled by record
                checkOut,
                null,  // checkOutTime - default handled by record
                roomType,
                guests,
                confirmation,
                totalPrice,
                nights,
                coordinates.map(HotelGeocodingService.Coordinates::latitude).orElse(null),
                coordinates.map(HotelGeocodingService.Coordinates::longitude).orElse(null),
                null,  // guestName - not available from regex parsing
                null   // phone - not available from regex parsing
        );
        hotelReservationService.save(ocrText.threadId(), ocrText.userId(), reservation);
        return reservation;
    }

    private Optional<HotelGeocodingService.Coordinates> lookupCoordinates(String address, String hotelName) {
        var query = (address == null || address.isBlank()) ? hotelName : address;
        return hotelGeocodingService.lookup(query);
    }

    private String extractHotelName(String text) {
        var name = findFirst(HOTEL_NAME_PATTERN.matcher(text), 1);
        if (!name.isBlank()) {
            return name;
        }
        for (var line : text.split("\\r?\\n")) {
            var trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.matches("(?i).*(hotel|resort|inn|suites|suite|lodge).*")
                    && !trimmed.matches("(?i).*(check|room|guest|reservation|confirmation).*")
                    && trimmed.length() <= 80) {
                return trimmed;
            }
        }
        return "";
    }

    private String extractAddress(String text) {
        var addr = findFirst(ADDRESS_PATTERN.matcher(text), 1);
        if (!addr.isBlank()) {
            return addr;
        }
        for (var line : text.split("\\r?\\n")) {
            var trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.matches("(?i).*(street|st\\.?|road|rd\\.?|avenue|ave\\.?|boulevard|blvd\\.?|city|district).*")
                    && trimmed.matches(".*\\d+.*")) {
                return trimmed;
            }
        }
        return "";
    }

    private Integer parseNights(String text, LocalDate checkIn, LocalDate checkOut) {
        var matcher = NIGHTS_PATTERN.matcher(text);
        if (matcher.find()) {
            return normalizeNights(Integer.parseInt(matcher.group(1)));
        }
        return safeDaysBetween(checkIn, checkOut);
    }

    private String findFirst(Matcher matcher, int group) {
        return matcher.find() ? matcher.group(group).trim() : "";
    }

    private LocalDate parseDate(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        var raw = matcher.group(1).trim();
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_DATE,
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("yyyy.MM.dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMMM dd, yyyy", java.util.Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.ENGLISH)
        };
        for (var formatter : formatters) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // 다른 포맷 시도
            }
        }
        return null;
    }

    private Integer safeDaysBetween(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (days <= 0 || days > Integer.MAX_VALUE) {
            return null;
        }
        return (int) days;
    }

    private Integer normalizeNights(Integer nights) {
        if (nights == null || nights <= 0) {
            return null;
        }
        return nights;
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
