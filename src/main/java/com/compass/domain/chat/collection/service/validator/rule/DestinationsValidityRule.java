package com.compass.domain.chat.collection.service.validator.rule;

import com.compass.domain.chat.collection.service.validator.ValidationRule;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.service.PerplexityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Optional;

@Slf4j
@Order(10) // 다른 기본 규칙들 다음에 실행되도록 순서 지정
@Component
@RequiredArgsConstructor
public class DestinationsValidityRule implements ValidationRule {

    private final PerplexityClient perplexityClient;

    @Override
    public boolean appliesTo(TravelFormSubmitRequest request) {
        // 목적지 정보가 존재하고, 비어있지 않을 때만 이 규칙을 적용합니다.
        return !CollectionUtils.isEmpty(request.destinations());
    }

    @Override
    public Optional<String> validate(TravelFormSubmitRequest request) {
        for (String destination : request.destinations()) {
            if (destination == null || destination.isBlank() || "목적지 미정".equalsIgnoreCase(destination)) {
                continue; // "목적지 미정"이나 빈 값은 검사에서 제외
            }
            try {
                String prompt = String.format("Is '%s' a real, valid place name on Earth that can be used as a travel destination? Answer with only 'true' or 'false'.", destination);
                String response = perplexityClient.search(prompt).trim().toLowerCase();
                if (response.contains("false")) {
                    log.warn("유효하지 않은 목적지: '{}'", destination);
                    return Optional.of(String.format("'%s'은(는) 유효한 목적지가 아닌 것 같아요. 다시 확인해주시겠어요?", destination));
                }
            } catch (Exception e) {
                log.error("목적지 '{}' 유효성 검증 중 API 오류 발생", destination, e);
                return Optional.of("목적지 유효성을 확인하는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.");
            }
        }
        return Optional.empty();
    }
}