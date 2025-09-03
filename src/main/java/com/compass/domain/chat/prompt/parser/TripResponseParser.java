package com.compass.domain.chat.prompt.parser;

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.TripDetail;
import com.compass.domain.trip.TripStatus;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 응답을 Trip/TripDetail 엔티티로 파싱하는 헬퍼 클래스
 */
@Component
public class TripResponseParser {
    
    private static final Logger logger = LoggerFactory.getLogger(TripResponseParser.class);
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    // Use constructor injection for Spring-managed beans
    public TripResponseParser(ObjectMapper objectMapper, UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }
    
    /**
     * JSON 형식의 LLM 응답을 Trip 엔티티로 변환
     * 
     * @param llmResponse LLM이 생성한 JSON 응답
     * @param userId 사용자 ID
     * @param threadId 채팅 스레드 ID
     * @return Trip 엔티티
     */
    public Trip parseToTrip(String llmResponse, Long userId, Long threadId) {
        try {
            // JSON 응답에서 순수 JSON 부분만 추출 (마크다운 코드 블록 제거)
            String jsonContent = extractJsonFromResponse(llmResponse);
            JsonNode root = objectMapper.readTree(jsonContent);
            
            // Find the User entity by userId
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
            
            // Trip 엔티티 생성
            Trip trip = Trip.builder()
                .user(user) // Pass the User object, not the ID
                .threadId(threadId)
                .title(root.path("title").asText())
                .destination(root.path("destination").asText())
                .startDate(LocalDate.parse(root.path("startDate").asText(), DATE_FORMATTER))
                .endDate(LocalDate.parse(root.path("endDate").asText(), DATE_FORMATTER))
                .numberOfPeople(root.path("numberOfPeople").asInt())
                .totalBudget(root.path("totalBudget").asInt())
                .status(TripStatus.PLANNING) // Use the enum for type safety
                .tripMetadata(root.path("tripMetadata").toString())
                .build();
            
            // TripDetail 엔티티들 파싱 및 추가
            List<TripDetail> details = parseTripDetails(root);
            for (TripDetail detail : details) {
                trip.addDetail(detail);
            }
            
            logger.info("Successfully parsed Trip entity with {} details", details.size());
            return trip;
            
        } catch (JsonProcessingException e) {
            logger.error("Error parsing LLM response to Trip entity", e);
            throw new IllegalArgumentException("Invalid JSON format in LLM response", e);
        } catch (Exception e) {
            logger.error("Unexpected error parsing LLM response", e);
            throw new RuntimeException("Failed to parse LLM response to Trip entity", e);
        }
    }
    
    /**
     * JSON 응답에서 TripDetail 리스트 파싱
     */
    private List<TripDetail> parseTripDetails(JsonNode root) {
        List<TripDetail> details = new ArrayList<>();
        JsonNode itinerary = root.path("itinerary");
        
        if (itinerary.isArray()) {
            for (JsonNode dayNode : itinerary) {
                int dayNumber = dayNode.path("dayNumber").asInt();
                LocalDate activityDate = LocalDate.parse(
                    dayNode.path("activityDate").asText(), 
                    DATE_FORMATTER
                );
                
                JsonNode activities = dayNode.path("activities");
                if (activities.isArray()) {
                    int displayOrder = 1;
                    for (JsonNode activity : activities) {
                        TripDetail detail = parseSingleActivity(
                            activity, 
                            dayNumber, 
                            activityDate, 
                            displayOrder++
                        );
                        details.add(detail);
                    }
                }
            }
        }
        
        return details;
    }
    
    /**
     * 단일 액티비티를 TripDetail로 변환
     */
    private TripDetail parseSingleActivity(JsonNode activity, int dayNumber, 
                                          LocalDate activityDate, int displayOrder) {
        return TripDetail.builder()
            .dayNumber(dayNumber)
            .activityDate(activityDate)
            .activityTime(parseTime(activity.path("activityTime").asText()))
            .placeName(activity.path("placeName").asText())
            .category(activity.path("category").asText())
            .description(activity.path("description").asText())
            .estimatedCost(activity.path("estimatedCost").asInt())
            .address(activity.path("address").asText())
            .latitude(activity.path("latitude").asDouble())
            .longitude(activity.path("longitude").asDouble())
            .tips(activity.path("tips").asText())
            .additionalInfo(activity.path("additionalInfo").toString())
            .displayOrder(displayOrder)
            .build();
    }
    
    /**
     * 시간 문자열을 LocalTime으로 변환
     */
    private LocalTime parseTime(String timeStr) {
        try {
            if (timeStr == null || timeStr.isEmpty()) {
                return null;
            }
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (Exception e) {
            logger.warn("Failed to parse time: {}, returning null", timeStr);
            return null;
        }
    }
    
    /**
     * LLM 응답에서 JSON 부분만 추출 (마크다운 코드 블록 처리)
     */
    private String extractJsonFromResponse(String response) {
        // ```json 으로 시작하는 코드 블록 처리
        if (response.contains("```json")) {
            int startIdx = response.indexOf("```json") + 7;
            int endIdx = response.indexOf("```", startIdx);
            if (endIdx > startIdx) {
                return response.substring(startIdx, endIdx).trim();
            }
        }
        
        // 순수 JSON 찾기 (중괄호로 시작)
        int startIdx = response.indexOf("{");
        int endIdx = response.lastIndexOf("}");
        if (startIdx >= 0 && endIdx > startIdx) {
            return response.substring(startIdx, endIdx + 1);
        }
        
        // 그대로 반환
        return response.trim();
    }
    
    /**
     * 일별 상세 일정을 기존 Trip에 추가
     * 
     * @param trip 기존 Trip 엔티티
     * @param dailyItineraryJson 일별 일정 JSON
     * @param dayNumber 날짜 번호
     */
    public void addDailyItinerary(Trip trip, String dailyItineraryJson, int dayNumber) {
        try {
            String jsonContent = extractJsonFromResponse(dailyItineraryJson);
            JsonNode root = objectMapper.readTree(jsonContent);
            
            LocalDate activityDate = LocalDate.parse(
                root.path("activityDate").asText(), 
                DATE_FORMATTER
            );
            
            JsonNode activities = root.path("activities");
            if (activities.isArray()) {
                int displayOrder = 1;
                for (JsonNode activity : activities) {
                    TripDetail detail = parseSingleActivity(
                        activity, 
                        dayNumber, 
                        activityDate, 
                        displayOrder++
                    );
                    trip.addDetail(detail);
                }
            }
            
            logger.info("Added {} activities for day {}", activities.size(), dayNumber);
            
        } catch (Exception e) {
            logger.error("Error adding daily itinerary to Trip", e);
            throw new RuntimeException("Failed to parse daily itinerary", e);
        }
    }
    
    /**
     * LLM 응답 검증
     * 
     * @param llmResponse 검증할 응답
     * @return 유효한 JSON 형식인지 여부
     */
    public boolean isValidTripResponse(String llmResponse) {
        try {
            String jsonContent = extractJsonFromResponse(llmResponse);
            JsonNode root = objectMapper.readTree(jsonContent);
            
            // 필수 필드 확인
            return root.has("title") && 
                   root.has("destination") && 
                   root.has("startDate") && 
                   root.has("endDate") &&
                   root.has("itinerary");
                   
        } catch (Exception e) {
            logger.debug("Invalid trip response format", e);
            return false;
        }
    }
}