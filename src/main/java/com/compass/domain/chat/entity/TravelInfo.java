package com.compass.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "collected_travel_info")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelInfo{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String threadId;
    private String userId;
    private List<String> destinations;
    private String departureLocation;
    private LocalDate startDate;
    private LocalDate endDate;
    private String companions;
    private Long budget;
    private List<String> travelStyle;
    private String reservationDocument;
}
