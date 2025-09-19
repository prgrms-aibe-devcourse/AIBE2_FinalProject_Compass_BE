package com.compass.domain.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IataCodeService {

    private final ObjectMapper objectMapper;
    private Map<String, String> airportMap = Collections.emptyMap();

    @PostConstruct
    void load() {
        try (InputStream inputStream = new ClassPathResource("data/iata-airports.json").getInputStream()) {
            airportMap = objectMapper.readValue(inputStream, new TypeReference<Map<String, String>>() {});
            log.info("IATA 공항 코드 {}건 로드", airportMap.size());
        } catch (IOException e) {
            log.warn("IATA 공항 코드 로드 실패 - 기본 매핑 사용", e);
            airportMap = Map.of();
        }
    }

    public String lookup(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        var upper = code.toUpperCase(Locale.ROOT);
        return airportMap.getOrDefault(upper, "");
    }
}
