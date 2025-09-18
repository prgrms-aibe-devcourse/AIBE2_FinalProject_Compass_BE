package com.compass.domain.chat.function.processing;

import com.compass.domain.chat.model.dto.FlightReservation;
import com.compass.domain.chat.model.dto.OCRText;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
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
    private static final Pattern AIRPORT_PATTERN = Pattern.compile("(FROM|DEPARTURE|DEP)[:\u3002]?\\s*([A-Z]{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARRIVAL_PATTERN = Pattern.compile("(TO|ARRIVAL|ARR)[:\u3002]?\\s*([A-Z]{3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_PATTERN = Pattern.compile("(?:\\s|\\n|\\r)(\\d{4}-\\d{2}-\\d{2}|\\d{2}/\\d{2}/\\d{4})");
    private static final Pattern PASSENGER_PATTERN = Pattern.compile("PASSENGER[:\u3002]?\\s*(.+)", Pattern.CASE_INSENSITIVE);

    @Override
    public FlightReservation apply(OCRText ocrText) {
        var text = ocrText.rawText();
        var flightNumber = findFirst(FLIGHT_PATTERN.matcher(text), 0);
        var departure = extractCode(AIRPORT_PATTERN.matcher(text));
        var arrival = extractCode(ARRIVAL_PATTERN.matcher(text));
        var dates = DATE_PATTERN.matcher(text);
        var departureDate = parseDate(dates);
        var arrivalDate = parseDate(dates);
        var passenger = findFirst(PASSENGER_PATTERN.matcher(text), 1);
        if (flightNumber.isBlank()) {
            log.debug("항공편 번호를 찾지 못했습니다.");
        }
        return new FlightReservation(
                flightNumber,
                departure,
                arrival,
                departureDate,
                arrivalDate,
                passenger
        );
    }

    private String findFirst(Matcher matcher, int group) {
        return matcher.find() ? matcher.group(group) : "";
    }

    private String extractCode(Matcher matcher) {
        return matcher.find() ? matcher.group(2).toUpperCase(Locale.ROOT) : "";
    }

    private LocalDate parseDate(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        var raw = matcher.group(2);
        for (var formatter : new DateTimeFormatter[]{DateTimeFormatter.ISO_DATE, DateTimeFormatter.ofPattern("MM/dd/yyyy")}) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (DateTimeParseException ignored) {
                // 다른 포맷 시도
            }
        }
        return null;
    }
}
