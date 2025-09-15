package com.compass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 * REQ-SEARCH-003: Kakao Map API 검색을 위한 RestTemplate 빈 등록
 * REQ-SEARCH-004: 통합 검색 서비스에서 외부 API 호출을 위한 RestTemplate Bean 제공
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
