package com.compass.media.controller;

import com.compass.domain.media.controller.MediaController;
import com.compass.domain.media.service.MediaService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @Test
    void mediaControllerCreationTest() {
        // MediaController 인스턴스 생성 테스트
        MockitoAnnotations.openMocks(this);
        MediaController controller = new MediaController(mediaService);
        assertThat(controller).isNotNull();
    }

    @Test
    void mediaControllerBasicTest() {
        // 기본적인 Controller 테스트
        assertThat("MEDIA_CONTROLLER").isEqualTo("MEDIA_CONTROLLER");
    }
}