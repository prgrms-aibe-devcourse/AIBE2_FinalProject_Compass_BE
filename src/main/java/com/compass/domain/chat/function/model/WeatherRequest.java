package com.compass.domain.chat.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.LocalDate;

/**
 * Request model for weather information function
 */
@JsonClassDescription("Get weather information for a specific location")
public record WeatherRequest(
    @JsonProperty(required = true)
    @JsonPropertyDescription("The city or location to get weather for (e.g., 'Seoul', 'Tokyo', 'New York')")
    String location,
    
    @JsonPropertyDescription("The date to get weather for in ISO format (YYYY-MM-DD). If not provided, returns current weather")
    LocalDate date,
    
    @JsonPropertyDescription("Temperature unit: 'celsius' or 'fahrenheit' (default: 'celsius')")
    String unit
) {
    public WeatherRequest {
        // Default values
        if (unit == null) unit = "celsius";
    }
}