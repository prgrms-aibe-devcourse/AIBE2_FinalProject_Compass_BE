package com.compass.domain.trip.service;

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

    // TODO: 여행 계획 생성/조회/수정/삭제 등 비즈니스 로직 구현
}
