package com.compass.domain.media.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * S3Service - AWS S3 연동 서비스
 * REQ-MEDIA-002, REQ-MEDIA-003, REQ-MEDIA-004 요구사항 구현
 */
@Service
public class S3Service {
    
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    @Value("${aws.s3.bucket-name:compass-media-bucket}")
    private String bucketName;
    
    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;
    
    @Value("${aws.s3.access-key:}")
    private String accessKey;
    
    @Value("${aws.s3.secret-key:}")
    private String secretKey;
    
    @Value("${aws.s3.url-expiration-minutes:60}")
    private int urlExpirationMinutes;
    
    private S3Client s3Client;
    private S3Presigner s3Presigner;
    
    @PostConstruct
    public void initializeS3Client() {
        try {
            // AWS 자격증명 설정
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            
            // S3 클라이언트 초기화
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
            
            // S3 Presigner 초기화 (서명된 URL 생성용)
            this.s3Presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
            
            logger.info("S3 클라이언트 초기화 완료 - Bucket: {}, Region: {}", bucketName, region);
            
            // 버킷 존재 여부 확인 및 생성
            ensureBucketExists();
            
        } catch (Exception e) {
            logger.error("S3 클라이언트 초기화 실패", e);
            throw new RuntimeException("S3 서비스 초기화에 실패했습니다.", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }
    
    /**
     * 버킷 존재 여부 확인 및 생성
     */
    private void ensureBucketExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(headBucketRequest);
            logger.info("S3 버킷 확인 완료: {}", bucketName);
            
        } catch (NoSuchBucketException e) {
            logger.info("S3 버킷이 존재하지 않아 생성합니다: {}", bucketName);
            createBucket();
        } catch (Exception e) {
            logger.warn("S3 버킷 확인 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * S3 버킷 생성
     */
    private void createBucket() {
        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .createBucketConfiguration(CreateBucketConfiguration.builder()
                            .locationConstraint(BucketLocationConstraint.fromValue(region))
                            .build())
                    .build();
            
            s3Client.createBucket(createBucketRequest);
            logger.info("S3 버킷 생성 완료: {}", bucketName);
            
        } catch (Exception e) {
            logger.error("S3 버킷 생성 실패: {}", bucketName, e);
            throw new RuntimeException("S3 버킷 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 파일을 S3에 업로드
     * 
     * @param file 업로드할 파일
     * @param userId 사용자 ID
     * @return S3 키 (파일 경로)
     */
    public String uploadFile(MultipartFile file, Long userId) {
        try {
            String s3Key = generateS3Key(file.getOriginalFilename(), userId);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(java.util.Map.of(
                            "original-filename", file.getOriginalFilename(),
                            "user-id", userId.toString(),
                            "upload-timestamp", LocalDateTime.now().toString()
                    ))
                    .build();
            
            RequestBody requestBody = RequestBody.fromInputStream(
                    file.getInputStream(), 
                    file.getSize()
            );
            
            PutObjectResponse response = s3Client.putObject(putObjectRequest, requestBody);
            
            logger.info("파일 업로드 성공 - S3 Key: {}, ETag: {}", s3Key, response.eTag());
            return s3Key;
            
        } catch (IOException e) {
            logger.error("파일 읽기 오류", e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            logger.error("S3 업로드 실패", e);
            throw new RuntimeException("S3 파일 업로드에 실패했습니다.", e);
        }
    }
    
    /**
     * S3에서 파일 삭제
     * 
     * @param s3Key 삭제할 파일의 S3 키
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            logger.info("파일 삭제 완료 - S3 Key: {}", s3Key);
            
        } catch (Exception e) {
            logger.error("S3 파일 삭제 실패 - S3 Key: {}", s3Key, e);
            throw new RuntimeException("S3 파일 삭제에 실패했습니다.", e);
        }
    }
    
    /**
     * 서명된 URL 생성 (파일 다운로드용)
     * 
     * @param s3Key S3 키
     * @return 서명된 URL
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(urlExpirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();
            
            logger.debug("서명된 URL 생성 완료 - S3 Key: {}", s3Key);
            return presignedUrl;
            
        } catch (Exception e) {
            logger.error("서명된 URL 생성 실패 - S3 Key: {}", s3Key, e);
            throw new RuntimeException("서명된 URL 생성에 실패했습니다.", e);
        }
    }
    
    /**
     * 파일 존재 여부 확인
     * 
     * @param s3Key S3 키
     * @return 파일 존재 여부
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("파일 존재 여부 확인 실패 - S3 Key: {}", s3Key, e);
            return false;
        }
    }
    
    /**
     * 파일의 메타데이터 조회
     * 
     * @param s3Key S3 키
     * @return 파일 메타데이터
     */
    public HeadObjectResponse getFileMetadata(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();
            
            return s3Client.headObject(headObjectRequest);
            
        } catch (Exception e) {
            logger.error("파일 메타데이터 조회 실패 - S3 Key: {}", s3Key, e);
            throw new RuntimeException("파일 메타데이터 조회에 실패했습니다.", e);
        }
    }
    
    /**
     * S3 키 생성 (파일 경로)
     * 형식: media/{userId}/{year}/{month}/{day}/{uuid}_{originalFilename}
     */
    private String generateS3Key(String originalFilename, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString();
        
        // 파일명에서 특수문자 제거 및 공백을 언더스코어로 변경
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        return String.format("media/%d/%s/%s_%s", userId, datePath, uuid, sanitizedFilename);
    }
    
    /**
     * 공개 URL 생성 (버킷이 공개 설정된 경우)
     */
    public String generatePublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }
    
    // Getter methods
    public String getBucketName() {
        return bucketName;
    }
    
    public String getRegion() {
        return region;
    }
}