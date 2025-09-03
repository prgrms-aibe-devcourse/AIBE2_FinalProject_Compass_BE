package com.compass.domain.trip.service;

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.dto.TripCreate;
import com.compass.domain.trip.repository.TripDetailRepository;
import com.compass.domain.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripDetailRepository tripDetailRepository;

    @Transactional
    public TripCreate.Response createTrip(TripCreate.Request request) {
        Trip trip = request.toTripEntity();
        Trip savedTrip = tripRepository.save(trip);
        return TripCreate.Response.from(savedTrip);
    }
}
