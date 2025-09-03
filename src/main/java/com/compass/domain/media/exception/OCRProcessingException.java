package com.compass.domain.media.exception;

import java.util.UUID;

/**
 * OCR 처리 관련 예외
 * 이미지 텍스트 추출 과정에서 발생하는 오류를 처리
 */
public class OCRProcessingException extends RuntimeException {

    private final String errorCode;
    private final UUID fileUuid;
    private final String processingStage;

    public OCRProcessingException(String message) {
        super(message);
        this.errorCode = "OCR_PROCESSING_ERROR";
        this.fileUuid = null;
        this.processingStage = null;
    }

    public OCRProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "OCR_PROCESSING_ERROR";
        this.fileUuid = null;
        this.processingStage = null;
    }

    public OCRProcessingException(String errorCode, String message, UUID fileUuid) {
        super(message);
        this.errorCode = errorCode;
        this.fileUuid = fileUuid;
        this.processingStage = null;
    }

    public OCRProcessingException(String errorCode, String message, UUID fileUuid, String processingStage) {
        super(message);
        this.errorCode = errorCode;
        this.fileUuid = fileUuid;
        this.processingStage = processingStage;
    }

    public OCRProcessingException(String errorCode, String message, UUID fileUuid, String processingStage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fileUuid = fileUuid;
        this.processingStage = processingStage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public UUID getFileUuid() {
        return fileUuid;
    }

    public String getProcessingStage() {
        return processingStage;
    }
}