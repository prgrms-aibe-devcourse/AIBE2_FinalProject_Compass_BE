package com.compass.domain.chat.stage3.model;

import com.compass.domain.chat.model.context.TravelContext;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stage3Request {
    private String threadId;
    private String userId;
    private Map<String, Object> stage2Data;
    private TravelContext travelContext;
    private List<Map<String, Object>> selectedPlaces;
    private String optimizationMode; // BALANCED, TIME_EFFICIENT, EXPERIENCE_FOCUSED
}