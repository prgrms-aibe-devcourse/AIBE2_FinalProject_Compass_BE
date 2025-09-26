package com.compass.domain.chat.function.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

// Google Places Text Search Function
@Slf4j
@Component
public class SearchGooglePlacesFunction implements Function<SearchGooglePlacesFunction.Query, List<SearchGooglePlacesFunction.Place>> {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.places.api.key:${GOOGLE_PLACES_API_KEY:}}")
    private String googleApiKey;

    @Value("${google.places.search.url:https://maps.googleapis.com/maps/api/place/textsearch/json}")
    private String textSearchUrl;

    @Override
    public List<Place> apply(Query query) {
        log.info("Google Places 검색 시작: {}", query.keyword());

        if (googleApiKey == null || googleApiKey.isBlank()) {
            log.error("Google Places API 키가 설정되지 않았습니다");
            return List.of();
        }

        try {
            String builtQuery = buildQuery(query);
            String url = UriComponentsBuilder.fromHttpUrl(textSearchUrl)
                .queryParam("query", builtQuery)
                .queryParam("language", "ko")
                .queryParam("region", "kr")
                .queryParam("key", googleApiKey)
                .build()
                .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return parseResponse(response.getBody());

        } catch (Exception e) {
            log.error("Google Places 검색 실패: {}", query.keyword(), e);
            return List.of();
        }
    }

    public CompletableFuture<List<Place>> searchAsync(Query query) {
        return CompletableFuture.supplyAsync(() -> apply(query));
    }

    private String buildQuery(Query query) {
        StringBuilder builder = new StringBuilder();
        if (query.region() != null && !query.region().isBlank()) {
            builder.append(query.region()).append(' ');
        }
        if (query.keyword() != null && !query.keyword().isBlank()) {
            builder.append(query.keyword()).append(' ');
        }
        if (query.category() != null && !query.category().isBlank()) {
            builder.append(query.category());
        }
        return builder.toString().trim();
    }

    private List<Place> parseResponse(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            String status = root.path("status").asText();
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                log.warn("Google Places 응답 상태: {}", status);
            }

            JsonNode results = root.path("results");
            if (!results.isArray()) {
                return List.of();
            }

            List<Place> places = new ArrayList<>();
            for (JsonNode item : results) {
                String name = item.path("name").asText("");
                if (name.isEmpty()) {
                    continue;
                }

                String address = item.path("formatted_address").asText("");
                String placeId = item.path("place_id").asText("");
                Double rating = item.has("rating") ? item.get("rating").asDouble() : null;
                String category = mapTypesToCategory(item.path("types"));

                places.add(new Place(name, address, category, rating, placeId));
            }

            log.info("Google Places 검색 결과: {}건", places.size());
            return places;

        } catch (Exception e) {
            log.error("Google Places 응답 파싱 실패", e);
            return List.of();
        }
    }

    private String mapTypesToCategory(JsonNode typesNode) {
        if (typesNode == null || !typesNode.isArray() || typesNode.size() == 0) {
            return "기타";
        }

        for (JsonNode typeNode : typesNode) {
            String type = typeNode.asText("");
            if (type.isEmpty()) {
                continue;
            }

            String normalized = type.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "tourist_attraction", "museum", "art_gallery", "park", "zoo", "aquarium" -> "관광지";
                case "restaurant" -> "맛집";
                case "cafe", "bakery" -> "카페";
                case "shopping_mall", "department_store" -> "쇼핑";
                case "lodging", "hotel" -> "숙박";
                case "amusement_park", "spa" -> "액티비티";
                case "night_club", "bar" -> "나이트라이프";
                default -> "기타";
            };
        }

        return "기타";
    }

    public record Query(
        String keyword,
        String region,
        String category
    ) {}

    public record Place(
        String name,
        String address,
        String category,
        Double rating,
        String placeId
    ) {}
}

