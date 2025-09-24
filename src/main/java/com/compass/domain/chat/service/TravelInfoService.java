package com.compass.domain.chat.service;


import com.compass.domain.chat.entity.TravelInfo;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.repository.TravelInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TravelInfoService {
    private final TravelInfoRepository travelInfoRepository;

    @Transactional
    public void saveTravelInfo(String threadId, TravelFormSubmitRequest info) {
        TravelInfo entity = TravelInfo.builder()
                .threadId(threadId)
                .userId(info.userId())
                .destinations(info.destinations())
                .departureLocation(info.departureLocation())
                .startDate(info.travelDates() != null ? info.travelDates().startDate() : null)
                .endDate(info.travelDates() != null ? info.travelDates().endDate() : null)
                .companions(info.companions())
                .budget(info.budget())
                .travelStyle(info.travelStyle())
                .build();
        travelInfoRepository.save(entity);
    }
}