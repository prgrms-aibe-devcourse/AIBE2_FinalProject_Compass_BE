package com.compass.domain.media.exception;

import com.compass.common.exception.GlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice(basePackages = "com.compass.domain.media")
@Order(1)
public class MediaExceptionHandler {
    
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleFileValidationException(FileValidationException e) {
        log.warn("파일 검증 실패: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new GlobalExceptionHandler.ErrorResponse(e.getMessage()));
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("파일 크기 초과: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new GlobalExceptionHandler.ErrorResponse("파일 크기가 허용된 최대 크기를 초과합니다. (최대: 10MB)"));
    }
    
    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleS3UploadException(S3UploadException e) {
        log.error("S3 업로드 중 오류 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GlobalExceptionHandler.ErrorResponse("파일 업로드 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }
    
    @ExceptionHandler(OCRProcessingException.class)
    public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleOCRProcessingException(OCRProcessingException e) {
        log.error("OCR 처리 중 오류 발생: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GlobalExceptionHandler.ErrorResponse("OCR 처리 중 오류가 발생했습니다: " + e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleGenericException(Exception e) {
        log.error("미디어 처리 중 예상치 못한 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GlobalExceptionHandler.ErrorResponse("파일 처리 중 오류가 발생했습니다. 관리자에게 문의하세요."));
    }
}