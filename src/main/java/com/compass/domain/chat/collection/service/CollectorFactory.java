package com.compass.domain.chat.collection.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

// 사용자 선택에 따라 적절한 정보 수집기(Collector)를 생성하는 팩토리
@Component
@RequiredArgsConstructor
public class CollectorFactory {

    // Spring이 Bean 이름을 key로 하여 모든 TravelInfoCollector 구현체를 주입
    private final Map<String, TravelInfoCollector> collectors;

    public TravelInfoCollector getCollector(String type) {
        String beanName = type + "BasedCollector"; // "form" -> "formBasedCollector"
        TravelInfoCollector collector = collectors.get(beanName);

        if (collector == null) {
            throw new IllegalArgumentException("지원하지 않는 수집기 타입입니다: " + type);
        }
        return collector;
    }
}