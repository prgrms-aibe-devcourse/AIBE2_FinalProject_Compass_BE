package com.compass.domain.chat.collection.service.validator.rule;

import com.compass.domain.chat.collection.service.validator.ValidationRule;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.service.PerplexityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepartureLocationValidationRule implements ValidationRule {

    private final PerplexityClient perplexityClient;

    @Override
    public boolean appliesTo(TravelFormSubmitRequest request) {
        // 출발지 정보가 존재하고, 비어있지 않을 때만 이 규칙을 적용합니다.
        return request.departureLocation() != null && !request.departureLocation().isBlank();
    }

    @Override
    public Optional<String> validate(TravelFormSubmitRequest request) {
        String departure = request.departureLocation();
        try {
            // Perplexity API를 사용하여 장소의 유효성을 검증합니다.
            String prompt = String.format(
                "Is '%s' a real, valid place name on Earth that can be used as a travel departure location? Answer with only 'true' or 'false'.",
                departure
            );
            String response = perplexityClient.search(prompt).trim().toLowerCase();

            // LLM의 응답이 'false'이면 유효하지 않은 장소로 판단합니다.
            if (response.contains("false")) {
                log.warn("유효하지 않은 출발지: '{}'", departure);
                return Optional.of(String.format("'%s'은(는) 유효한 출발지가 아닌 것 같아요. 다시 확인해주시겠어요?", departure));
            }
        } catch (Exception e) {
            log.error("출발지 '{}' 유효성 검증 중 API 오류 발생", departure, e);
            // [핵심 수정] API 호출에 실패하면, 검증을 통과시키는 대신 사용자에게 문제가 발생했음을 알립니다.
            return Optional.of("출발지 유효성을 확인하는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.");
        }
        return Optional.empty();
    }
}