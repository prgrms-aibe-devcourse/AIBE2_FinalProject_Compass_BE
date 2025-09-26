package com.compass.domain.chat.collection.service.validator.rule;

import com.compass.domain.chat.collection.service.validator.ValidationRule;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.service.PerplexityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepartureLocationValidationRule implements ValidationRule {

    private final ChatModel chatModel;
    private final PerplexityClient perplexityClient;

    // 전국 주요 도시 목록 (검증 없이 통과)
    private static final Set<String> MAJOR_KOREAN_CITIES = Set.of(
        // 수도권
        "서울", "서울특별시", "인천", "인천광역시", "경기", "경기도", "수원", "고양", "용인", "성남", "부천", "안산", "안양", "남양주", "화성", "의정부",

        // 광역시
        "부산", "부산광역시", "대구", "대구광역시", "대전", "대전광역시", "광주", "광주광역시", "울산", "울산광역시",

        // 특별자치시/도
        "세종", "세종특별자치시", "제주", "제주도", "제주특별자치도", "제주시", "서귀포", "서귀포시",

        // 강원도
        "강원", "강원도", "춘천", "원주", "강릉", "속초", "동해", "태백", "삼척", "양양", "평창", "정선",

        // 충청도
        "충북", "충청북도", "청주", "충주", "제천", "단양",
        "충남", "충청남도", "천안", "아산", "공주", "논산", "보령", "서산", "당진", "태안",

        // 전라도
        "전북", "전라북도", "전주", "익산", "군산", "정읍", "남원", "김제", "완주",
        "전남", "전라남도", "목포", "여수", "순천", "나주", "광양", "무안", "담양", "보성", "해남",

        // 경상도
        "경북", "경상북도", "포항", "경주", "구미", "김천", "안동", "영주", "영천", "상주", "문경", "경산", "울진", "울릉도",
        "경남", "경상남도", "창원", "진주", "김해", "양산", "거제", "통영", "사천", "밀양", "함안", "거창", "남해",

        // 공항/터미널 지역
        "김포", "김포공항", "인천공항", "김해공항", "제주공항", "청주공항", "대구공항", "광주공항", "여수공항",

        // 영어 표기도 허용
        "Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan", "Sejong", "Jeju", "Gyeonggi",
        "Gangwon", "Chungbuk", "Chungnam", "Jeonbuk", "Jeonnam", "Gyeongbuk", "Gyeongnam"
    );

    @Override
    public boolean appliesTo(TravelFormSubmitRequest request) {
        // 출발지 정보가 존재하고, 비어있지 않을 때만 이 규칙을 적용합니다.
        return request.departureLocation() != null && !request.departureLocation().isBlank();
    }

    @Override
    public Optional<String> validate(TravelFormSubmitRequest request) {
        String departure = request.departureLocation();
        String normalized = departure != null ? departure.trim() : "";

        if (!normalized.isEmpty() && MAJOR_KOREAN_CITIES.contains(normalized)) {
            return Optional.empty();
        }

        try {
            // Perplexity API를 사용하여 장소의 유효성을 검증합니다.
            String prompt = String.format(
                "Is '%s' a real, valid place name on Earth that can be used as a travel departure location? Answer with only 'true' or 'false'.",
                normalized
            );
            String response = perplexityClient.search(prompt).trim().toLowerCase();

            // LLM의 응답이 'false'이면 유효하지 않은 장소로 판단합니다.
            if (response.contains("false")) {
                log.warn("유효하지 않은 출발지: '{}'", normalized);
                return Optional.of(String.format("'%s'은(는) 유효한 출발지가 아닌 것 같아요. 다시 확인해주시겠어요?", normalized));
            }
        } catch (Exception e) {
            log.error("출발지 '{}' 유효성 검증 중 API 오류 발생", normalized, e);
            // [핵심 수정] API 호출에 실패하면, 검증을 통과시키는 대신 사용자에게 문제가 발생했음을 알립니다.
            return Optional.of("출발지 유효성을 확인하는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.");
        }
        return Optional.empty();
    }
}
