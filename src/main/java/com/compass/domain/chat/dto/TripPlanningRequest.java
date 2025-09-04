package com.compass.domain.chat.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request DTO for comprehensive trip planning with function calling
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripPlanningRequest {
    
    @NotBlank(message = "Destination is required")
    private String destination;
    
    @NotBlank(message = "Origin location is required")
    private String origin;
    
    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;
    
    private Integer numberOfTravelers = 1;
    
    private String travelStyle; // luxury, budget, moderate
    
    private Map<String, Object> preferences;
    
    private String[] interests; // culture, food, adventure, shopping, nature
    
    private Integer budgetPerPerson;
    
    private String currency = "USD";
}