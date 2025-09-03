package com.compass.domain.media.exception;

/**
 * S3 서비스 관련 예외
 * 파일 업로드, 다운로드, 삭제 등 S3 작업 중 발생하는 오류를 처리
 */
public class S3ServiceException extends RuntimeException {

    private final String errorCode;
    private final String s3Key;
    private final String operation;

    public S3ServiceException(String message) {
        super(message);
        this.errorCode = "S3_SERVICE_ERROR";
        this.s3Key = null;
        this.operation = null;
    }

    public S3ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "S3_SERVICE_ERROR";
        this.s3Key = null;
        this.operation = null;
    }

    public S3ServiceException(String errorCode, String message, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.s3Key = null;
        this.operation = operation;
    }

    public S3ServiceException(String errorCode, String message, String s3Key, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.s3Key = s3Key;
        this.operation = operation;
    }

    public S3ServiceException(String errorCode, String message, String s3Key, String operation, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.s3Key = s3Key;
        this.operation = operation;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getOperation() {
        return operation;
    }
}