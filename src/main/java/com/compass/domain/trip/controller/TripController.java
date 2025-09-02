package com.compass.domain.trip.controller;

import com.compass.domain.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    // TODO: 여행 계획 생성/조회/수정/삭제 등 API 엔드포인트 구현
}
