package com.compass.domain.trip.exception;

public class TripNotFoundException extends RuntimeException {
    
    public TripNotFoundException(String message) {
        super(message);
    }
    
    public TripNotFoundException(Long tripId) {
        super("해당 여행 계획을 찾을 수 없습니다. ID: " + tripId);
    }
}
