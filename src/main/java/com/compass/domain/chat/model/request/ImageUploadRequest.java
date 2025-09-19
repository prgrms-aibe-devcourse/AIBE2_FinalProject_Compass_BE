package com.compass.domain.chat.model.request;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public record ImageUploadRequest(
        String fileName,
        String contentType,
        byte[] data,
        String threadId,
        String userId
) {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public ImageUploadRequest {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 필요합니다.");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("이미지 데이터가 비어 있습니다.");
        }
        contentType = (contentType == null || contentType.isBlank())
                ? DEFAULT_CONTENT_TYPE
                : contentType;
        data = data.clone();
        threadId = threadId == null ? "" : threadId;
        userId = userId == null ? "" : userId;
    }

    public static ImageUploadRequest fromMultipartFile(MultipartFile file, String threadId, String userId) {
        if (file == null) {
            throw new IllegalArgumentException("업로드 파일이 필요합니다.");
        }
        try {
            var bytes = file.getBytes();
            return new ImageUploadRequest(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    bytes,
                    threadId,
                    userId
            );
        } catch (IOException e) {
            throw new IllegalStateException("업로드 파일을 읽지 못했습니다.", e);
        }
    }
}
