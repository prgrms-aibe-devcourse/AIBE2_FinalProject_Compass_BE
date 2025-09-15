package com.compass.domain.media.exception;

/**
 * OCR 처리 중 발생하는 예외
 */
public class OCRProcessingException extends RuntimeException {
    
    public OCRProcessingException(String message) {
        super(message);
    }
    
    public OCRProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}