package com.compass.domain.chat.service.external;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Client {

    private final Environment environment;
    private software.amazon.awssdk.services.s3.S3Client delegate;
    private S3Presigner presigner;
    private String bucket;
    private Region region;
    private String cdnDomain;
    private static final int MULTIPART_THRESHOLD = 5 * 1024 * 1024;
    private static final int PART_SIZE = 5 * 1024 * 1024;

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
        presigner = S3Presigner.builder()
                .region(region)
                .build();
        cdnDomain = environment.getProperty("AWS_CLOUDFRONT_DOMAIN");
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
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (data.length > MULTIPART_THRESHOLD) {
                    uploadMultipart(data, objectKey, safeType);
                } else {
                    delegate.putObject(request, RequestBody.fromBytes(data));
                }
                log.debug("S3 업로드 완료 - key: {} (시도 {}회)", objectKey, attempt);
                break;
            } catch (Exception ex) {
                if (attempt == 3) {
                    throw new IllegalStateException("S3 업로드 실패", ex);
                }
                log.warn("S3 업로드 재시도 - key: {}, attempt: {}", objectKey, attempt, ex);
                try {
                    Thread.sleep(200L * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
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
        if (cdnDomain != null && !cdnDomain.isBlank()) {
            return "https://" + cdnDomain + "/" + objectKey;
        }
        return "https://" + bucket + ".s3." + region.id() + ".amazonaws.com/" + objectKey;
    }

    public String getPresignedUrl(String objectKey) {
        var request = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(7))
                .getObjectRequest(builder -> builder.bucket(bucket).key(objectKey))
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(request);
        return presigned.url().toString();
    }

    private String buildObjectKey(String directory, String originalFileName) {
        var baseFolder = directory == null || directory.isBlank()
                ? "uploads"
                : directory;
        var folder = baseFolder.contains("/") ? baseFolder : baseFolder + "/" + LocalDate.now();
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
        if (presigner != null) {
            presigner.close();
        }
    }

    private void uploadMultipart(byte[] data, String objectKey, String contentType) {
        var createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();
        var response = delegate.createMultipartUpload(createRequest);
        var uploadId = response.uploadId();
        var completedParts = new ArrayList<CompletedPart>();
        try {
            int partNumber = 1;
            for (int position = 0; position < data.length; position += PART_SIZE, partNumber++) {
                int remaining = Math.min(PART_SIZE, data.length - position);
                var partRequest = UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();
                var partResponse = delegate.uploadPart(partRequest, RequestBody.fromBytes(slice(data, position, remaining)));
                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(partResponse.eTag())
                        .build());
            }
            var completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();
            var completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipartUpload)
                    .build();
            delegate.completeMultipartUpload(completeRequest);
        } catch (Exception ex) {
            delegate.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build());
            throw new IllegalStateException("멀티파트 업로드 실패", ex);
        }
    }

    private byte[] slice(byte[] data, int position, int length) {
        var slice = new byte[length];
        System.arraycopy(data, position, slice, 0, length);
        return slice;
    }
}
