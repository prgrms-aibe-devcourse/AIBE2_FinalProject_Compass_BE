package com.compass.domain.media.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
public class S3Configuration {

    @Value("${aws.access-key-id:dummy-access-key}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:dummy-secret-key}")
    private String secretAccessKey;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Bean
    public S3Client s3Client() {
        log.info("S3Client 초기화 - 리전: {}", region);
        
        // 더미 자격증명인 경우 기본 자격증명 공급자 사용 (로컬 개발 환경)
        if (accessKeyId == null || secretAccessKey == null || 
            "dummy-access-key".equals(accessKeyId) || "dummy-secret-key".equals(secretAccessKey)) {
            log.warn("AWS 자격증명이 설정되지 않았거나 더미 값입니다. 기본 자격증명 공급자를 사용합니다.");
            return S3Client.builder()
                    .region(Region.of(region))
                    .build();
        }
        
        // 명시적 자격증명 사용
        AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
    
    @Bean
    public S3Presigner s3Presigner() {
        log.info("S3Presigner 초기화 - 리전: {}", region);
        
        // 더미 자격증명인 경우 기본 자격증명 공급자 사용
        if (accessKeyId == null || secretAccessKey == null || 
            "dummy-access-key".equals(accessKeyId) || "dummy-secret-key".equals(secretAccessKey)) {
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .build();
        }
        
        // 명시적 자격증명 사용
        AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}