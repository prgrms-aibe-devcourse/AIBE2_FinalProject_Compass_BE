package com.compass.domain.media.entity;

/**
 * 파일 상태를 나타내는 Enum
 * 파일 업로드부터 처리 완료까지의 생명주기를 관리
 */
public enum FileStatus {
    /**
     * 업로드 중 - 파일이 업로드되고 있는 상태
     */
    UPLOADING,
    
    /**
     * 업로드 완료 - S3에 성공적으로 업로드된 상태
     */
    UPLOADED,
    
    /**
     * 처리 완료 - OCR 등 후처리가 완료된 상태
     */
    PROCESSED,
    
    /**
     * 업로드 실패 - 업로드 과정에서 오류가 발생한 상태
     */
    UPLOAD_FAILED,
    
    /**
     * 처리 실패 - 후처리 과정에서 오류가 발생한 상태
     */
    PROCESSING_FAILED,
    
    /**
     * 삭제됨 - 논리적으로 삭제된 상태
     */
    DELETED
}