package com.compass.domain.collection.dto;

import java.time.LocalDate;
import java.util.List;

// 수집된 여행 정보를 담는 불변 데이터 객체(DTO)
public record TravelInfo(
    List<String> destinations,
    LocalDate startDate,
    LocalDate endDate,
    Integer budget,
    String companions,
    String travelStyle
) {}
