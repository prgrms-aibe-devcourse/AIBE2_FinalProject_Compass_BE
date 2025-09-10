package com.compass.config;

import com.compass.domain.chat.dto.ChatDtos.MessageDto;
import com.compass.domain.chat.dto.ChatDtos.ThreadDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class InMemoryStorageConfig {

    @Bean(name = "threadsDb")
    public Map<String, ThreadDto> threadsDb() {
        return new ConcurrentHashMap<>();
    }

    @Bean(name = "messagesDb")
    public Map<String, List<MessageDto>> messagesDb() {
        return new ConcurrentHashMap<>();
    }
}
