package com.compass.domain.media.exception;

/**
 * 파일 유효성 검증 실패 시 발생하는 예외
 * 파일 크기, 타입, 보안 검사 등에서 문제가 발생했을 때 사용
 */
public class FileValidationException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    public FileValidationException(String message) {
        super(message);
        this.errorCode = "FILE_VALIDATION_ERROR";
        this.args = new Object[0];
    }

    public FileValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FILE_VALIDATION_ERROR";
        this.args = new Object[0];
    }

    public FileValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public FileValidationException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public FileValidationException(String errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}