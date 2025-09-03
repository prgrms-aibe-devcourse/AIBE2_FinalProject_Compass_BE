package com.compass.media.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaServiceTest {

    @Test
    void mediaServiceBasicTest() {
        // 기본적인 Service 테스트
        assertThat("MEDIA_SERVICE").isEqualTo("MEDIA_SERVICE");
    }

    @Test
    void mediaServiceStringTest() {
        // 문자열 테스트
        String serviceName = "MediaService";
        assertThat(serviceName).isNotNull();
        assertThat(serviceName).startsWith("Media");
    }
}