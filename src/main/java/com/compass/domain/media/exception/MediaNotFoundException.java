package com.compass.domain.media.exception;

import java.util.UUID;

/**
 * 미디어 파일을 찾을 수 없을 때 발생하는 예외
 * 파일 조회, 수정, 삭제 시 해당 파일이 존재하지 않거나 접근 권한이 없을 때 사용
 */
public class MediaNotFoundException extends RuntimeException {

    private final String errorCode;
    private final UUID fileUuid;
    private final Long userId;

    public MediaNotFoundException(String message) {
        super(message);
        this.errorCode = "MEDIA_NOT_FOUND";
        this.fileUuid = null;
        this.userId = null;
    }

    public MediaNotFoundException(String message, UUID fileUuid) {
        super(message);
        this.errorCode = "MEDIA_NOT_FOUND";
        this.fileUuid = fileUuid;
        this.userId = null;
    }

    public MediaNotFoundException(String message, UUID fileUuid, Long userId) {
        super(message);
        this.errorCode = "MEDIA_NOT_FOUND";
        this.fileUuid = fileUuid;
        this.userId = userId;
    }

    public MediaNotFoundException(String errorCode, String message, UUID fileUuid, Long userId) {
        super(message);
        this.errorCode = errorCode;
        this.fileUuid = fileUuid;
        this.userId = userId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public UUID getFileUuid() {
        return fileUuid;
    }

    public Long getUserId() {
        return userId;
    }
}