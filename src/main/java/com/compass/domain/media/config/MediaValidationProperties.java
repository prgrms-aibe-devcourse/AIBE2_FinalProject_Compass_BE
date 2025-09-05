package com.compass.domain.media.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "media.validation")
public class MediaValidationProperties {
    
    /**
     * 지원되는 파일 확장자 목록
     */
    private Set<String> supportedExtensions = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    
    /**
     * 지원되는 MIME 타입 목록
     */
    private Set<String> supportedMimeTypes = Set.of(
        "image/jpeg", 
        "image/png", 
        "image/webp", 
        "image/gif"
    );
    
    /**
     * 최대 파일 크기 (바이트)
     */
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    
    /**
     * 악성 파일 시그니처 목록 (16진수 문자열)
     */
    private List<String> maliciousSignatures = List.of(
        "4D5A",     // PE 실행 파일 시그니처
        "7F454C46", // ELF 실행 파일 시그니처  
        "3C73637269707424", // 스크립트 태그
        "3C3F706870" // PHP 태그
    );
    
    /**
     * 파일명에서 금지된 문자들
     */
    private Set<String> forbiddenChars = Set.of("<", ">", ":", "\"", "/", "\\", "|", "?", "*");
    
    /**
     * 이미지 파일로 인식할 MIME 타입들
     */
    private Set<String> imageMimeTypes = Set.of(
        "image/jpeg",
        "image/png", 
        "image/webp",
        "image/gif"
    );
}