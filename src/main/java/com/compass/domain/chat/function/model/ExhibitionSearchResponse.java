package com.compass.domain.chat.function.model;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

/**
 * Response model for exhibition search function
 */
@Builder
public record ExhibitionSearchResponse(
    String location,
    List<Exhibition> exhibitions
) {
    @Builder
    public record Exhibition(
        String name,
        String type,
        String venue,
        String venueType,
        Double rating,
        String description,
        List<String> artists,
        List<String> artStyles,
        String curator,
        LocalDate startDate,
        LocalDate endDate,
        String openingHours,
        List<String> closedDays,
        Integer entryFee,
        String currency,
        String ticketTypes,
        Boolean advanceBookingRequired,
        Boolean audioGuideAvailable,
        List<String> languagesAvailable,
        Boolean photographyAllowed,
        Boolean interactiveElements,
        List<String> highlights,
        List<String> featuredWorks,
        String exhibitionSize,
        Integer estimatedDuration,
        List<String> accessibilityFeatures,
        List<String> facilities,
        List<String> nearbyAttractions,
        List<String> educationalPrograms,
        List<String> workshops,
        String giftShop,
        String cafe,
        String parking,
        String address,
        String transportation,
        List<String> images,
        Double distance,
        String distanceUnit,
        Integer reviewCount,
        Integer visitorCount,
        String phoneNumber,
        String website,
        String bookingUrl,
        List<String> reviews,
        String specialNotes
    ) {}
}