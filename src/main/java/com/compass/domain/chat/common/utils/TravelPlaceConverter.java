package com.compass.domain.chat.common.utils;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.stage2.dto.UserSelectionRequest.SelectedPlace;
import com.compass.domain.chat.stage2.dto.SelectedSchedule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// TravelPlace 변환 유틸리티 클래스
@Component
public class TravelPlaceConverter {

    // TravelCandidate -> TravelPlace 변환
    public TravelPlace fromCandidate(TravelCandidate candidate) {
        if (candidate == null) {
            return null;
        }

        return TravelPlace.builder()
            .placeId(candidate.getPlaceId())
            .name(candidate.getName())
            .category(candidate.getCategory())
            .address(candidate.getAddress())
            .latitude(candidate.getLatitude())
            .longitude(candidate.getLongitude())
            .rating(candidate.getRating())
            .reviewCount(candidate.getReviewCount())
            .priceLevel(candidate.getPriceLevel())
            .photoUrl(candidate.getPhotoUrl())
            .description(candidate.getDescription())
            .openNow(candidate.getOpenNow() != null ? candidate.getOpenNow() : false)
            .phoneNumber(candidate.getPhoneNumber())
            .website(candidate.getWebsite())
            .build();
    }

    // SelectedSchedule -> TravelPlace 변환
    public TravelPlace fromSelectedSchedule(SelectedSchedule schedule) {
        if (schedule == null) {
            return null;
        }

        return TravelPlace.builder()
            .placeId(schedule.placeId())
            .name(schedule.placeName())
            .category(schedule.category())
            .latitude(schedule.latitude())
            .longitude(schedule.longitude())
            .address(schedule.address())
            .rating(schedule.rating())
            .build();
    }

    // SelectedPlace -> TravelPlace 변환
    public TravelPlace fromSelectedPlace(SelectedPlace selectedPlace) {
        if (selectedPlace == null) {
            return null;
        }

        return TravelPlace.builder()
            .placeId(selectedPlace.placeId())
            .name(selectedPlace.name())
            .category(selectedPlace.category())
            .latitude(selectedPlace.latitude())
            .longitude(selectedPlace.longitude())
            .address(selectedPlace.address())
            .rating(selectedPlace.rating())
            .build();
    }

    // TravelCandidate 리스트 -> TravelPlace 리스트 변환
    public List<TravelPlace> fromCandidates(List<TravelCandidate> candidates) {
        if (candidates == null) {
            return List.of();
        }

        return candidates.stream()
            .map(this::fromCandidate)
            .collect(Collectors.toList());
    }

    // SelectedSchedule 리스트 -> TravelPlace 리스트 변환
    public List<TravelPlace> fromSelectedSchedules(List<SelectedSchedule> schedules) {
        if (schedules == null) {
            return List.of();
        }

        return schedules.stream()
            .map(this::fromSelectedSchedule)
            .collect(Collectors.toList());
    }

    // SelectedPlace 리스트 -> TravelPlace 리스트 변환
    public List<TravelPlace> fromSelectedPlaces(List<SelectedPlace> selectedPlaces) {
        if (selectedPlaces == null) {
            return List.of();
        }

        return selectedPlaces.stream()
            .map(this::fromSelectedPlace)
            .collect(Collectors.toList());
    }

    // TravelPlace -> 간단한 설명 문자열 변환
    public String toSimpleString(TravelPlace place) {
        if (place == null) {
            return "Unknown Place";
        }

        return String.format("%s (%s) - %.1f⭐ (%d reviews)",
            place.getName(),
            place.getCategory() != null ? place.getCategory() : "기타",
            place.getRating() != null ? place.getRating() : 0.0,
            place.getReviewCount() != null ? place.getReviewCount() : 0
        );
    }

    // TravelPlace -> 상세 설명 문자열 변환
    public String toDetailString(TravelPlace place) {
        if (place == null) {
            return "Unknown Place";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📍 ").append(place.getName()).append("\n");

        if (place.getCategory() != null) {
            sb.append("  카테고리: ").append(place.getCategory()).append("\n");
        }

        if (place.getAddress() != null) {
            sb.append("  주소: ").append(place.getAddress()).append("\n");
        }

        if (place.getRating() != null && place.getReviewCount() != null) {
            sb.append("  평점: ").append(place.getRating()).append("⭐")
              .append(" (리뷰 ").append(place.getReviewCount()).append("개)\n");
        }

        if (place.getDescription() != null) {
            sb.append("  설명: ").append(place.getDescription()).append("\n");
        }

        return sb.toString();
    }
}