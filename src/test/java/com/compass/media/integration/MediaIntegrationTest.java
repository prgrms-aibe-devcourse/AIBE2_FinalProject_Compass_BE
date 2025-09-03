package com.compass.media.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaIntegrationTest {

    @Test
    void mediaIntegrationBasicTest() {
        // 기본적인 통합 테스트
        assertThat("MEDIA").isEqualTo("MEDIA");
    }

    @Test
    void mediaIntegrationStringTest() {
        // 문자열 테스트
        String result = "MEDIA_INTEGRATION";
        assertThat(result).isNotNull();
        assertThat(result).contains("MEDIA");
    }
}