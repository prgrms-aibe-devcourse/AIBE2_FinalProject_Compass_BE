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
        // DB에서 기존 정보를 찾거나, 없으면 새로 생성합니다. (Upsert 로직)
        TravelInfo travelInfo = travelInfoRepository.findByThreadId(threadId)
                .orElse(TravelInfo.builder().threadId(threadId).userId(info.userId()).build());

        // DTO의 내용으로 엔티티를 업데이트합니다.
        TravelInfo updatedInfo = travelInfo.toBuilder()
                .destinations(info.destinations())
                .departureLocation(info.departureLocation())
                .startDate(info.travelDates() != null ? info.travelDates().startDate() : null)
                .endDate(info.travelDates() != null ? info.travelDates().endDate() : null)
                // [수정] 시간 필드 추가
                .departureTime(info.departureTime())
                .endTime(info.endTime())
                .companions(info.companions())
                .budget(info.budget())
                .travelStyle(info.travelStyle())
                .reservationDocument(info.reservationDocument())
                .build();

        travelInfoRepository.save(updatedInfo);
    }

    @Transactional(readOnly = true)
    public TravelFormSubmitRequest loadTravelInfo(String threadId) {
        return travelInfoRepository.findByThreadId(threadId)
                .map(this::convertToDto)
                // [수정] 새로운 생성자에 맞게 null 인자 추가
                .orElse(new TravelFormSubmitRequest(null, null, null, null, null, null, null, null, null, null)); // 정보가 없으면 빈 DTO 반환
    }

    private TravelFormSubmitRequest convertToDto(TravelInfo entity) {
        TravelFormSubmitRequest.DateRange dateRange = (entity.getStartDate() != null && entity.getEndDate() != null)
                ? new TravelFormSubmitRequest.DateRange(entity.getStartDate(), entity.getEndDate())
                : null;

        // [수정] 새로운 생성자에 맞게 시간 필드 추가
        return new TravelFormSubmitRequest(
                entity.getUserId(),
                entity.getDestinations(),
                entity.getDepartureLocation(),
                dateRange,
                entity.getDepartureTime(),
                entity.getEndTime(),
                entity.getCompanions(),
                entity.getBudget(),
                entity.getTravelStyle(),
                entity.getReservationDocument()
        );
    }
}