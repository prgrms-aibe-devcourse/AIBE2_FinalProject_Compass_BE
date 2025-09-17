package com.compass.domain.chat.service.external;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Client {

    private final Environment environment;
    private software.amazon.awssdk.services.s3.S3Client delegate;
    private String bucket;
    private Region region;

    @PostConstruct
    void init() {
        bucket = environment.getProperty("AWS_S3_BUCKET_NAME");
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 버킷이 설정되지 않았습니다.");
        }
        var regionId = environment.getProperty("AWS_S3_REGION", "ap-northeast-2");
        region = Region.of(regionId);
        // 기본 자격 증명 체인으로 클라이언트 초기화
        delegate = software.amazon.awssdk.services.s3.S3Client.builder()
                .region(region)
                .build();
    }

    public String upload(byte[] data, String directory, String originalFileName, String contentType) {
        var objectKey = buildObjectKey(directory, originalFileName);
        var safeType = contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
        var request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(safeType)
                .build();
        delegate.putObject(request, RequestBody.fromBytes(data));
        log.debug("S3 업로드 완료 - key: {}", objectKey);
        return objectKey;
    }

    public void delete(String objectKey) {
        // 이미지 교체 시 남은 객체 정리
        var request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        delegate.deleteObject(request);
    }

    public String getUrl(String objectKey) {
        return "https://" + bucket + ".s3." + region.id() + ".amazonaws.com/" + objectKey;
    }

    private String buildObjectKey(String directory, String originalFileName) {
        var folder = directory == null || directory.isBlank() ? "uploads" : directory;
        var extension = extractExtension(originalFileName);
        return folder + "/" + UUID.randomUUID() + extension;
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return ".jpg";
        }
        var index = fileName.lastIndexOf('.');
        if (index == -1 || index == fileName.length() - 1) {
            return ".jpg";
        }
        return fileName.substring(index).toLowerCase();
    }

    @PreDestroy
    public void close() {
        if (delegate != null) {
            delegate.close();
        }
    }
}
