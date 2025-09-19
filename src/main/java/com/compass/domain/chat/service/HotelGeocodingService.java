package com.compass.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelGeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${GOOGLE_MAPS_API_KEY:}")
    private String apiKey;

    public Optional<Coordinates> lookup(String query) {
        if (apiKey == null || apiKey.isBlank() || query == null || query.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("address", query)
                    .queryParam("key", apiKey)
                    .build(true)
                    .toUri();
            var response = restTemplate.getForObject(uri, String.class);
            if (response == null) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");
            if (!results.isArray() || results.isEmpty()) {
                log.debug("지오코딩 결과 없음 - query: {}", query);
                return Optional.empty();
            }
            JsonNode location = results.get(0).path("geometry").path("location");
            if (!location.has("lat") || !location.has("lng")) {
                return Optional.empty();
            }
            double lat = location.get("lat").asDouble();
            double lng = location.get("lng").asDouble();
            return Optional.of(new Coordinates(lat, lng));
        } catch (RestClientException | IOException e) {
            log.warn("Google Maps 지오코딩 실패 - query: {}", query, e);
            return Optional.empty();
        }
    }

    public record Coordinates(Double latitude, Double longitude) {}
}
