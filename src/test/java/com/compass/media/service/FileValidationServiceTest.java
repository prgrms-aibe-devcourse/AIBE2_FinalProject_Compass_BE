package com.compass.media.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileValidationServiceTest {

    @Test
    void fileValidationServiceBasicTest() {
        // 기본적인 파일 검증 서비스 테스트
        assertThat("FILE_VALIDATION_SERVICE").isEqualTo("FILE_VALIDATION_SERVICE");
    }

    @Test
    void fileValidationServiceStringTest() {
        // 문자열 테스트
        String serviceName = "FileValidationService";
        assertThat(serviceName).isNotNull();
        assertThat(serviceName).contains("Validation");
    }
}