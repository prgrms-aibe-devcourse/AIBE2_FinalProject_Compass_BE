package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.time.LocalDate;

/**
 * Response model for weather information function
 */
@Builder
public record WeatherResponse(
    String location,
    LocalDate date,
    Double temperature,
    Double feelsLike,
    Integer humidity,
    String description,
    Double windSpeed,
    Double precipitation,
    Integer uvIndex,
    String sunrise,
    String sunset,
    Integer visibility,
    Double pressure,
    String windDirection
) {}