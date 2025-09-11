package com.compass.domain.media.service;

import com.compass.domain.media.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name:compass-media-bucket}")
    private String bucketName;

    @Value("${aws.s3.base-url:https://compass-media-bucket.s3.ap-northeast-2.amazonaws.com}")
    private String s3BaseUrl;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    /**
     * S3에 파일을 업로드합니다.
     * 
     * @param file 업로드할 파일
     * @param userId 사용자 ID
     * @param storedFilename 저장될 파일명
     * @return S3에 업로드된 파일의 URL
     */
    public String uploadFile(MultipartFile file, String userId, String storedFilename) {
        try {
            String s3Key = generateS3Key(userId, storedFilename);
            
            log.info("S3 파일 업로드 시작 - 버킷: {}, 키: {}", bucketName, s3Key);
            
            // 파일 메타데이터 설정
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(java.util.Map.of(
                            "original-filename", file.getOriginalFilename(),
                            "user-id", userId,
                            "upload-timestamp", LocalDateTime.now().toString()
                    ))
                    .build();
            
            // S3에 파일 업로드
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            String s3Url = generateS3Url(s3Key);
            log.info("S3 파일 업로드 완료 - URL: {}", s3Url);
            
            return s3Url;
            
        } catch (IOException e) {
            log.error("파일 읽기 중 오류 발생: {}", e.getMessage(), e);
            throw new S3UploadException("파일을 읽는 중 오류가 발생했습니다.", e);
        } catch (S3Exception e) {
            log.error("S3 업로드 중 오류 발생: {}", e.getMessage(), e);
            throw new S3UploadException("S3 업로드 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new S3UploadException("파일 업로드 중 예상치 못한 오류가 발생했습니다.", e);
        }
    }

    /**
     * S3에 썸네일을 업로드합니다.
     * REQ-MEDIA-007: 300x300 WebP 포맷 썸네일 업로드
     * 
     * @param thumbnailBytes 썸네일 바이트 배열
     * @param userId 사용자 ID
     * @param thumbnailFilename 썸네일 파일명
     * @return S3에 업로드된 썸네일의 URL
     */
    public String uploadThumbnail(byte[] thumbnailBytes, String userId, String thumbnailFilename) {
        try {
            String s3Key = generateThumbnailS3Key(userId, thumbnailFilename);
            
            log.info("S3 썸네일 업로드 시작 - 버킷: {}, 키: {}", bucketName, s3Key);
            
            // 썸네일 메타데이터 설정
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("image/webp")
                    .contentLength((long) thumbnailBytes.length)
                    .metadata(java.util.Map.of(
                            "thumbnail-type", "300x300",
                            "user-id", userId,
                            "upload-timestamp", LocalDateTime.now().toString(),
                            "generated-by", "compass-thumbnail-service"
                    ))
                    .build();
            
            // S3에 썸네일 업로드
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(thumbnailBytes));
            
            String thumbnailUrl = generateS3Url(s3Key);
            log.info("S3 썸네일 업로드 완료 - URL: {}", thumbnailUrl);
            
            return thumbnailUrl;
            
        } catch (S3Exception e) {
            log.error("S3 썸네일 업로드 중 오류 발생: {}", e.getMessage(), e);
            throw new S3UploadException("S3 썸네일 업로드 중 오류가 발생했습니다: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("썸네일 업로드 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new S3UploadException("썸네일 업로드 중 예상치 못한 오류가 발생했습니다.", e);
        }
    }

    /**
     * S3에서 파일을 삭제합니다.
     * 
     * @param s3Url 삭제할 파일의 S3 URL
     */
    public void deleteFile(String s3Url) {
        try {
            String s3Key = extractS3KeyFromUrl(s3Url);
            
            log.info("S3 파일 삭제 시작 - 버킷: {}, 키: {}", bucketName, s3Key);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            log.info("S3 파일 삭제 완료 - 키: {}", s3Key);
            
        } catch (S3Exception e) {
            log.error("S3 파일 삭제 중 오류 발생: {}", e.getMessage(), e);
            throw new S3UploadException("S3 파일 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 서명된 URL을 생성합니다 (임시 접근용).
     * 
     * @param s3Url 원본 S3 URL
     * @param expiration 만료 시간 (분)
     * @return 기본 S3 URL (presigner 미구현)
     */
    public String generatePresignedUrl(String s3Url, int expiration) {
        log.info("기본 S3 URL 반환 - presigner 기능 미구현");
        return s3Url;
    }

    /**
     * S3 키를 생성합니다.
     * 형식: media/{userId}/{year}/{month}/{day}/{filename}
     */
    private String generateS3Key(String userId, String filename) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("media/%s/%s/%s", userId, datePath, filename);
    }

    /**
     * 썸네일용 S3 키를 생성합니다.
     * 형식: thumbnails/{userId}/{year}/{month}/{day}/{filename}
     */
    private String generateThumbnailS3Key(String userId, String filename) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("thumbnails/%s/%s/%s", userId, datePath, filename);
    }

    /**
     * S3 키로부터 전체 URL을 생성합니다.
     */
    private String generateS3Url(String s3Key) {
        return String.format("%s/%s", s3BaseUrl, s3Key);
    }

    /**
     * S3 URL에서 키를 추출합니다.
     */
    private String extractS3KeyFromUrl(String s3Url) {
        if (s3Url.startsWith(s3BaseUrl)) {
            return s3Url.substring(s3BaseUrl.length() + 1); // +1 for the '/'
        }
        throw new IllegalArgumentException("유효하지 않은 S3 URL입니다: " + s3Url);
    }

    /**
     * S3 버킷이 존재하는지 확인합니다.
     */
    public boolean bucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
        } catch (NoSuchBucketException e) {
            log.warn("S3 버킷이 존재하지 않습니다: {}", bucketName);
            return false;
        } catch (S3Exception e) {
            log.error("S3 버킷 확인 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
}