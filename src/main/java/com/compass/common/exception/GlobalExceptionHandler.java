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

    // ================== 공통 에러 응답 객체 ==================
    @Getter
    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
    }

    // ================== 핸들러 ==================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(new ErrorResponse(errorMessage));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        // 실제 운영 환경에서는 로그를 남기는 것이 중요합니다.
        // log.error("Unhandled exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("예상치 못한 오류가 발생했습니다."));
    }
    
    // ================== 선호도 관련 예외 ==================

    @ExceptionHandler({
            InvalidWeightSumException.class,
            DuplicateTravelStyleException.class
    })
    public ResponseEntity<ErrorResponse> handlePreferenceExceptions(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidWeightRangeException.class)
    public ResponseEntity<WeightRangeErrorResponse> handleInvalidWeightRangeException(InvalidWeightRangeException ex) {
        return ResponseEntity.badRequest().body(new WeightRangeErrorResponse(ex.getMessage(), ex.getInvalidWeights()));
    }

    @Getter
    public static class WeightRangeErrorResponse extends ErrorResponse {
        private final java.util.List<InvalidWeightRangeException.InvalidWeight> violations;

        public WeightRangeErrorResponse(String message, java.util.List<InvalidWeightRangeException.InvalidWeight> violations) {
            super(message);
            this.violations = violations;
        }
    }
    
    // ================== 여행 관련 예외 ==================
    
    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTripNotFoundException(TripNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }
}