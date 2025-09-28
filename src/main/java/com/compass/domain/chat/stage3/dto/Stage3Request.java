package com.compass.domain.chat.stage3.dto;

import com.compass.domain.chat.model.TravelPlace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stage3Request {
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String travelStyle;
    private String travelCompanion;
    private String transportMode;
    private List<TravelPlace> userSelectedPlaces;
    private Integer budget;
    private List<String> specialRequirements;
}