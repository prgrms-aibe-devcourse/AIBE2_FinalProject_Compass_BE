package com.compass.domain.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Compass AI Travel Assistant API")
                        .version("1.0.0")
                        .description("AI 기반 개인화 여행 계획 서비스 API 문서")
                        .contact(new Contact().name("Team Compass")));
    }
}