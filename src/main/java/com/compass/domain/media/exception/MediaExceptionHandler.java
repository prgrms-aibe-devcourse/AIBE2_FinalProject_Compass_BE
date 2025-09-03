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

/**
 * MEDIA 도메인 전역 예외 처리기
 * 미디어 관련 모든 예외를 일관성 있게 처리하고 적절한 HTTP 응답을 생성
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.compass.domain.media")
public class MediaExceptionHandler {

    /**
     * 파일 유효성 검증 예외 처리
     */
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<Map<String, Object>> handleFileValidationException(FileValidationException e) {
        log.warn("파일 유효성 검증 실패: {} - {}", e.getErrorCode(), e.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
                e.getErrorCode(),
                e.getMessage(),
                HttpStatus.BAD_REQUEST,
                "파일 유효성 검증에 실패했습니다."
        );
        
        if (e.getArgs().length > 0) {
            errorResponse.put("details", e.getArgs());
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * S3 서비스 예외 처리
     */
    @ExceptionHandler(S3ServiceException.class)
    public ResponseEntity<Map<String, Object>> handleS3ServiceException(S3ServiceException e) {
        log.error("S3 서비스 오류: {} - 작업: {}, S3 키: {}, 메시지: {}", 
                e.getErrorCode(), e.getOperation(), e.getS3Key(), e.getMessage(), e);
        
        Map<String, Object> errorResponse = createErrorResponse(
                e.getErrorCode(),
                "파일 저장소 처리 중 오류가 발생했습니다.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "파일 업로드 또는 다운로드 중 문제가 발생했습니다."
        );
        
        if (e.getOperation() != null) {
            errorResponse.put("operation", e.getOperation());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * OCR 처리 예외 처리
     */
    @ExceptionHandler(OCRProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleOCRProcessingException(OCRProcessingException e) {
        log.error("OCR 처리 오류: {} - 파일 UUID: {}, 처리 단계: {}, 메시지: {}", 
                e.getErrorCode(), e.getFileUuid(), e.getProcessingStage(), e.getMessage(), e);
        
        Map<String, Object> errorResponse = createErrorResponse(
                e.getErrorCode(),
                "이미지 텍스트 추출 중 오류가 발생했습니다.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "OCR 처리 중 문제가 발생했습니다."
        );
        
        if (e.getFileUuid() != null) {
            errorResponse.put("fileUuid", e.getFileUuid());
        }
        if (e.getProcessingStage() != null) {
            errorResponse.put("processingStage", e.getProcessingStage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 미디어 파일 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMediaNotFoundException(MediaNotFoundException e) {
        log.warn("미디어 파일 찾을 수 없음: {} - 파일 UUID: {}, 사용자 ID: {}, 메시지: {}", 
                e.getErrorCode(), e.getFileUuid(), e.getUserId(), e.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
                e.getErrorCode(),
                e.getMessage(),
                HttpStatus.NOT_FOUND,
                "요청한 파일을 찾을 수 없습니다."
        );
        
        if (e.getFileUuid() != null) {
            errorResponse.put("fileUuid", e.getFileUuid());
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * 파일 크기 초과 예외 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("파일 크기 초과: {}", e.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
                "FILE_SIZE_EXCEEDED",
                "업로드 파일 크기가 허용된 최대 크기를 초과했습니다.",
                HttpStatus.PAYLOAD_TOO_LARGE,
                "파일 크기를 확인하고 다시 시도해주세요."
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 인수: {}", e.getMessage());
        
        Map<String, Object> errorResponse = createErrorResponse(
                "INVALID_ARGUMENT",
                e.getMessage(),
                HttpStatus.BAD_REQUEST,
                "요청 파라미터가 올바르지 않습니다."
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 일반적인 RuntimeException 처리
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("예상치 못한 런타임 오류: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = createErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "잠시 후 다시 시도해주세요."
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 일반적인 Exception 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("예상치 못한 오류: {}", e.getMessage(), e);
        
        Map<String, Object> errorResponse = createErrorResponse(
                "UNKNOWN_ERROR",
                "알 수 없는 오류가 발생했습니다.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "잠시 후 다시 시도해주세요."
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * 표준화된 에러 응답 생성
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param httpStatus HTTP 상태
     * @param userMessage 사용자용 메시지
     * @return 에러 응답 맵
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, 
                                                   HttpStatus httpStatus, String userMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("userMessage", userMessage);
        errorResponse.put("status", httpStatus.value());
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", "/api/media");
        
        return errorResponse;
    }
}