package com.compass.domain.chat.entity;

import com.compass.domain.chat.service.PlaceDeduplicator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Stage 1 결과 저장 Entity - 가이드대로 간단하게
 * 
 * threadId, places, createdAt만 저장
 */
@Entity
@Table(name = "stage1_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stage1ResultEntity {
    
    @Id
    @Column(name = "thread_id")
    private String threadId;
    
    @Column(name = "places_json", columnDefinition = "text")
    private String placesJson;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // JSON 직렬화/역직렬화를 위한 ObjectMapper
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * TourPlace 리스트를 JSON 문자열로 변환하여 저장
     */
    public void setPlaces(List<PlaceDeduplicator.TourPlace> places) {
        try {
            this.placesJson = objectMapper.writeValueAsString(places);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패", e);
        }
    }
    
    /**
     * JSON 문자열을 TourPlace 리스트로 변환하여 반환
     */
    public List<PlaceDeduplicator.TourPlace> getPlaces() {
        try {
            if (placesJson == null || placesJson.trim().isEmpty()) {
                return List.of();
            }
            return objectMapper.readValue(placesJson, new TypeReference<List<PlaceDeduplicator.TourPlace>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 역직렬화 실패", e);
        }
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}