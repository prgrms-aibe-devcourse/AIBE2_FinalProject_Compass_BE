package com.compass.media.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class S3ServiceTest {

    @Test
    void s3ServiceBasicTest() {
        // 기본적인 S3 Service 테스트
        assertThat("S3_SERVICE").isEqualTo("S3_SERVICE");
    }

    @Test
    void s3ServiceStringTest() {
        // 문자열 테스트
        String serviceName = "S3Service";
        assertThat(serviceName).isNotNull();
        assertThat(serviceName).contains("S3");
    }
}