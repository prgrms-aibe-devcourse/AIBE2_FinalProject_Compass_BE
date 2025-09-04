package com.compass.common.exception;

import com.compass.domain.trip.exception.DuplicateTravelStyleException;
import com.compass.domain.trip.exception.InvalidWeightRangeException;
import com.compass.domain.trip.exception.InvalidWeightSumException;
import com.compass.domain.trip.exception.TripNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<TripErrorResponse> handleTripNotFoundException(TripNotFoundException ex) {
        TripErrorResponse response = new TripErrorResponse("TRIP_NOT_FOUND", ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidWeightSumException.class)
    public ResponseEntity<PreferenceErrorResponse> handleInvalidWeightSumException(InvalidWeightSumException ex) {
        PreferenceErrorResponse response = new PreferenceErrorResponse(
            "INVALID_WEIGHT_SUM", 
            ex.getMessage(), 
            LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidWeightRangeException.class)
    public ResponseEntity<WeightRangeErrorResponse> handleInvalidWeightRangeException(InvalidWeightRangeException ex) {
        WeightRangeErrorResponse response = new WeightRangeErrorResponse(
            "INVALID_WEIGHT_RANGE", 
            ex.getMessage(), 
            ex.getInvalidWeights(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateTravelStyleException.class)
    public ResponseEntity<TripErrorResponse> handleDuplicateTravelStyleException(DuplicateTravelStyleException ex) {
        TripErrorResponse response = new TripErrorResponse("DUPLICATE_TRAVEL_STYLE", ex.getMessage(), LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        // 간단하게 첫 번째 에러 메시지만 반환
        String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
    }

    @Getter
    @AllArgsConstructor
    public static class TripErrorResponse {
        private String error;
        private String message;
        private LocalDateTime timestamp;
    }

    @Getter
    @AllArgsConstructor
    public static class PreferenceErrorResponse {
        private String error;
        private String message;
        private LocalDateTime timestamp;
    }

    @Getter
    @AllArgsConstructor
    public static class WeightRangeErrorResponse {
        private String error;
        private String message;
        private java.util.List<InvalidWeightRangeException.InvalidWeight> invalidWeights;
        private LocalDateTime timestamp;
    }
}