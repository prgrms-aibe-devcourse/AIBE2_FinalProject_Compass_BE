package com.compass.domain.trip.service;

import com.compass.domain.trip.Trip;
import com.compass.domain.trip.dto.TripCreate;
import com.compass.domain.trip.dto.TripDetail;
import com.compass.domain.trip.dto.TripList;
import com.compass.domain.trip.exception.TripNotFoundException;
import com.compass.domain.trip.repository.TripRepository;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    @Transactional
    public TripCreate.Response createTrip(TripCreate.Request request) {
        // User 조회
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + request.userId()));
        
        Trip trip = request.toTripEntity();
        trip.setUser(user);
        
        Trip savedTrip = tripRepository.save(trip);
        return TripCreate.Response.from(savedTrip);
    }
    
    /**
     * 특정 여행 계획의 상세 정보를 조회합니다.
     * @param tripId 조회할 여행 계획 ID
     * @return 여행 계획 상세 정보
     * @throws TripNotFoundException 해당 ID의 여행 계획이 존재하지 않을 경우
     */
    public TripDetail.Response getTripById(Long tripId) {
        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));
        return TripDetail.Response.from(trip);
    }
    
    /**
     * 사용자의 여행 계획 목록을 페이징하여 조회합니다.
     * @param userId 조회할 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 여행 계획 목록
     */
    public Page<TripList.Response> getTripsByUserId(Long userId, Pageable pageable) {
        Page<Trip> tripPage = tripRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return tripPage.map(TripList.Response::from);
    }
    
    /**
     * 현재 로그인한 사용자의 여행 계획 목록을 페이징하여 조회합니다.
     * @param userEmail 조회할 사용자 이메일 (JWT에서 추출)
     * @param pageable 페이징 정보
     * @return 페이징된 여행 계획 목록
     */
    public Page<TripList.Response> getTripsByUserEmail(String userEmail, Pageable pageable) {
        Page<Trip> tripPage = tripRepository.findByUserEmailOrderByCreatedAtDesc(userEmail, pageable);
        return tripPage.map(TripList.Response::from);
    }
}
