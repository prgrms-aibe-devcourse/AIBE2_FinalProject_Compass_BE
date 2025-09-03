package com.compass.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI Configuration
 * 여행 계획 생성 및 관리 API 문서화
 */
@Configuration
public class SwaggerConfig {
    
    @Value("${spring.application.name:Compass}")
    private String applicationName;
    
    @Bean
    public OpenAPI customOpenAPI() {
        // JWT Security Scheme - 기존 feature/trip1 방식 유지하면서 개선
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");
        
        // Security Requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");
        
        // Server Configuration - develop 브랜치에서 추가
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Development Server");
        
        Server prodServer = new Server()
                .url("https://api.compass.com")
                .description("Production Server");
        
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .description("AI 기반 개인화된 여행 계획 생성 및 관리 API - Updated")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Compass Team")
                                .email("contact@compass.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(localServer, prodServer))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", securityScheme))
                .security(List.of(securityRequirement));
    }
}
