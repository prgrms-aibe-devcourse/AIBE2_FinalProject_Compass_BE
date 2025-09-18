package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.dto.FlightReservation;
import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.enums.DocumentType;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractFlightInfoFunction implements Function<OCRText, FlightReservation> {

    private static final Pattern FLIGHT_PATTERN = Pattern.compile("[A-Z]{2}\\s?\\d{2,4}");
    private static final Pattern DEPARTURE_PATTERN = Pattern.compile("(FROM|DEPARTURE|DEP)[:\u3002]?\s*([A-Z]{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARRIVAL_PATTERN = Pattern.compile("(TO|ARRIVAL|ARR)[:\u3002]?\s*([A-Z]{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROUTE_PATTERN = Pattern.compile("([A-Z]{3})\s?[→-]\s?([A-Z]{3})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?:\\s|\\n|\\r)(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{2}:\\d{2})");
    private static final Pattern PASSENGER_PATTERN = Pattern.compile("PASSENGER[:\u3002]?\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEAT_PATTERN = Pattern.compile("SEAT[:\u3002]?\s*([A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOOKING_PATTERN = Pattern.compile("(PNR|BOOKING|CONFIRMATION)[^A-Z0-9]*([A-Z0-9]{5,})", Pattern.CASE_INSENSITIVE);

    private final ProcessOCRFunction processOCRFunction;
    private final Map<String, String> airportNames = Map.of(
            "ICN", "Incheon International",
            "NRT", "Narita International",
            "GMP", "Gimpo International",
            "KIX", "Kansai International",
            "LAX", "Los Angeles International"
    );

    @PostConstruct
    void registerParser() {
        processOCRFunction.registerParser(DocumentType.FLIGHT_RESERVATION, this);
    }

    @Override
    public FlightReservation apply(OCRText ocrText) {
        var text = ocrText.rawText();
        var flightNumber = findFirst(FLIGHT_PATTERN.matcher(text), 0);
        var departure = resolveAirport(DEPARTURE_PATTERN.matcher(text));
        var arrival = resolveAirport(ARRIVAL_PATTERN.matcher(text));
        if (departure.isBlank() || arrival.isBlank()) {
            var routeMatcher = ROUTE_PATTERN.matcher(text);
            if (routeMatcher.find()) {
                departure = resolveAirportCode(routeMatcher.group(1));
                arrival = resolveAirportCode(routeMatcher.group(2));
            }
        }
        var dateMatcher = DATE_PATTERN.matcher(text);
        var departureDate = parseDate(dateMatcher);
        var departureTime = parseTime(text, departureDate, 1);
        var arrivalTime = parseTime(text, departureDate, 2);
        var passenger = findFirst(PASSENGER_PATTERN.matcher(text), 1);
        var seatNumber = findFirst(SEAT_PATTERN.matcher(text), 1);
        var bookingReference = findFirst(BOOKING_PATTERN.matcher(text), 2);
        if (flightNumber.isBlank()) {
            log.debug("항공편 번호를 찾지 못했습니다.");
        }
        return new FlightReservation(
                flightNumber,
                departure,
                arrival,
                departureTime,
                arrivalTime,
                passenger,
                seatNumber,
                bookingReference
        );
    }

    private String findFirst(Matcher matcher, int group) {
        return matcher.find() ? matcher.group(group) : "";
    }

    private String resolveAirport(Matcher matcher) {
        if (!matcher.find()) {
            return "";
        }
        return resolveAirportCode(matcher.group(2));
    }

    private String resolveAirportCode(String code) {
        return Optional.ofNullable(code)
                .map(c -> c.toUpperCase(Locale.ROOT))
                .map(c -> c + " - " + airportNames.getOrDefault(c, ""))
                .orElse("");
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

    private LocalDateTime parseTime(String text, LocalDate baseDate, int occurrence) {
        if (baseDate == null) {
            return null;
        }
        var matcher = TIME_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
            if (count == occurrence) {
                try {
                    var time = LocalTime.parse(matcher.group(1));
                    return baseDate.atTime(time);
                } catch (DateTimeParseException e) {
                    log.debug("시간 파싱 실패 - raw: {}", matcher.group(1));
                    return baseDate.atStartOfDay();
                }
            }
        }
        return baseDate.atStartOfDay();
    }
}
