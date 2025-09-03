package com.compass.domain.chat.prompt.parser;

/**
 * TripResponseParser is temporarily disabled as it depends on Trip entities 
 * which are not yet implemented. This class will be re-enabled once the Trip 
 * domain entities are created.
 * 
 * TODO: Uncomment this class after implementing:
 * - com.compass.domain.trip.Trip
 * - com.compass.domain.trip.TripDetail
 * - com.compass.domain.trip.TripStatus
 * - com.compass.domain.user.entity.User
 * - com.compass.domain.user.repository.UserRepository
 */
public class TripResponseParser {
    // Temporarily disabled - waiting for Trip domain implementation
}

/* Original code commented out:

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.TripDetail;
import com.compass.domain.trip.TripStatus;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TripResponseParser {
    private static final Logger log = LoggerFactory.getLogger(TripResponseParser.class);
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public TripResponseParser(ObjectMapper objectMapper, UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    public Trip parseToTrip(String llmResponse, Long userId, Long threadId) {
        log.info("Starting to parse LLM response for user {} and thread {}", userId, threadId);
        
        try {
            JsonNode root = objectMapper.readTree(llmResponse);
            
            // Get user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
            
            // Create Trip entity
            Trip trip = new Trip();
            trip.setUser(user);
            trip.setTitle(root.path("title").asText("여행 계획"));
            trip.setDestination(root.path("destination").asText());
            trip.setStartDate(LocalDate.parse(root.path("startDate").asText(), DATE_FORMATTER));
            trip.setEndDate(LocalDate.parse(root.path("endDate").asText(), DATE_FORMATTER));
            trip.setTripJsonData(llmResponse); // Store the full JSON response
            trip.setStatus(TripStatus.ACTIVE);
            trip.setThreadId(threadId);
            trip.setCreatedAt(LocalDateTime.now());
            trip.setUpdatedAt(LocalDateTime.now());
            
            // Parse and set trip details (daily itinerary)
            List<TripDetail> details = parseTripDetails(root);
            for (TripDetail detail : details) {
                detail.setTrip(trip);
            }
            trip.setDetails(details);
            
            log.info("Successfully parsed trip with {} days of activities", details.size());
            return trip;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse trip response", e);
        } catch (Exception e) {
            log.error("Unexpected error during trip parsing: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during trip parsing", e);
        }
    }
    
    private List<TripDetail> parseTripDetails(JsonNode root) {
        List<TripDetail> details = new ArrayList<>();
        JsonNode itinerary = root.path("itinerary");
        
        if (!itinerary.isMissingNode() && itinerary.isArray()) {
            for (JsonNode dayNode : itinerary) {
                int dayNumber = dayNode.path("day").asInt(1);
                LocalDate date = LocalDate.parse(
                    dayNode.path("date").asText(LocalDate.now().toString()),
                    DATE_FORMATTER
                );
                
                JsonNode activities = dayNode.path("activities");
                if (activities.isArray()) {
                    int sequence = 1;
                    for (JsonNode activity : activities) {
                        TripDetail detail = parseSingleActivity(activity, dayNumber, 
                                                               date, sequence++);
                        if (detail != null) {
                            details.add(detail);
                        }
                    }
                }
            }
        }
        
        return details;
    }
    
    private TripDetail parseSingleActivity(JsonNode activity, int dayNumber, 
                                          LocalDate date, int sequence) {
        TripDetail detail = TripDetail.builder()
            .dayNumber(dayNumber)
            .date(date)
            .sequence(sequence)
            .time(parseTime(activity.path("time").asText()))
            .title(activity.path("title").asText())
            .location(activity.path("location").asText())
            .description(activity.path("description").asText())
            .category(activity.path("category").asText("SIGHTSEEING"))
            .estimatedCost(activity.path("estimatedCost").asDouble(0.0))
            .currency(activity.path("currency").asText("KRW"))
            .duration(activity.path("duration").asInt(60))
            .transportMode(activity.path("transportMode").asText())
            .notes(activity.path("notes").asText())
            .isBooked(activity.path("isBooked").asBoolean(false))
            .build();
            
        return detail;
    }
    
    private LocalTime parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }
        try {
            return LocalTime.parse(timeString, TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse time: {}", timeString);
            return null;
        }
    }
    
    public void addDailyItinerary(Trip trip, String dailyItineraryJson, int dayNumber) {
        try {
            JsonNode root = objectMapper.readTree(dailyItineraryJson);
            
            LocalDate date = LocalDate.parse(
                root.path("date").asText(LocalDate.now().toString()), 
                DATE_FORMATTER
            );
            
            JsonNode activities = root.path("activities");
            if (activities.isArray()) {
                int sequence = 1;
                for (JsonNode activity : activities) {
                    TripDetail detail = parseSingleActivity(activity, dayNumber, 
                                                           date, sequence++);
                    if (detail != null) {
                        detail.setTrip(trip);
                        trip.getDetails().add(detail);
                    }
                }
            }
            
            log.info("Added {} activities for day {}", activities.size(), dayNumber);
            
        } catch (Exception e) {
            log.error("Failed to add daily itinerary for day {}: {}", 
                     dayNumber, e.getMessage(), e);
        }
    }
    
    public String validateAndCleanResponse(String llmResponse) {
        try {
            // Parse and re-serialize to ensure valid JSON
            JsonNode node = objectMapper.readTree(llmResponse);
            
            // Basic validation
            if (!node.hasNonNull("destination") || !node.hasNonNull("startDate")) {
                throw new IllegalArgumentException("Missing required fields in response");
            }
            
            return objectMapper.writeValueAsString(node);
            
        } catch (Exception e) {
            log.error("Failed to validate response: {}", e.getMessage());
            throw new RuntimeException("Invalid trip response format", e);
        }
    }
}

*/