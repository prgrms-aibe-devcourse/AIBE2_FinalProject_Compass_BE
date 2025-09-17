package com.compass.domain.chat.model.request;

public record ImageUploadRequest(
        String fileName,
        String contentType,
        byte[] data,
        String threadId,
        String userId
) {
    public ImageUploadRequest {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 필요합니다.");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("이미지 데이터가 비어 있습니다.");
        }
        contentType = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType;
        data = data.clone();
        threadId = threadId == null ? "" : threadId;
        userId = userId == null ? "" : userId;
    }
}
