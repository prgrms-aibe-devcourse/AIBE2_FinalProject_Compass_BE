package com.compass.domain.media.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "com.compass.domain.media")
public class MediaExceptionHandler {
    
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFileValidationException(FileValidationException e) {
        log.warn("파일 검증 실패: {}", e.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "File Validation Error");
        errorResponse.put("message", e.getMessage());
        errorResponse.put("path", "/api/media/upload");
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("파일 크기 초과: {}", e.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "File Size Exceeded");
        errorResponse.put("message", "파일 크기가 허용된 최대 크기를 초과합니다. (최대: 10MB)");
        errorResponse.put("path", "/api/media/upload");
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("미디어 처리 중 예상치 못한 오류 발생", e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "파일 처리 중 오류가 발생했습니다. 관리자에게 문의하세요.");
        errorResponse.put("path", "/api/media");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}