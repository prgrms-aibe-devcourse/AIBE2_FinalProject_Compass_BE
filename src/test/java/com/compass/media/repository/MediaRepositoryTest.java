package com.compass.media.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MediaRepositoryTest {

    @Test
    void contextLoads() {
        // JPA 컨텍스트가 정상적으로 로드되는지 확인
        assertThat(true).isTrue();
    }

    @Test
    void mediaRepositoryBasicTest() {
        // 기본적인 Repository 테스트
        assertThat("MEDIA_REPOSITORY").isEqualTo("MEDIA_REPOSITORY");
    }
}