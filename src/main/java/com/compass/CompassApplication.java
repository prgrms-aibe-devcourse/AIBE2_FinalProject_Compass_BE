package com.compass;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {OpenAiAutoConfiguration.class})
public class CompassApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompassApplication.class, args);
    }
}