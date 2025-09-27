package com.compass.domain.chat.collection.service.validator.rule;

import com.compass.domain.chat.collection.service.validator.ValidationRule;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.service.PerplexityClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Order(10) // 다른 기본 규칙들 다음에 실행되도록 순서 지정
@Component
public class DestinationsValidityRule implements ValidationRule {

    private final PerplexityClient perplexityClient;

    public DestinationsValidityRule(@Lazy PerplexityClient perplexityClient) {
        this.perplexityClient = perplexityClient;
    }

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

        // 유명 관광지
        "한라산", "설악산", "지리산", "북한산", "남산", "팔공산", "계룡산", "덕유산", "가야산", "오대산",
        "경복궁", "창덕궁", "덕수궁", "창경궁", "경희궁", "종묘", "북촌", "남대문", "동대문", "명동", "강남", "홍대", "이태원", "인사동",

        // 공항/터미널 지역
        "김포", "김포공항", "인천공항", "김해공항", "제주공항", "청주공항", "대구공항", "광주공항", "여수공항",

        // 영어 표기도 허용
        "Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Ulsan", "Sejong", "Jeju", "Gyeonggi",
        "Gangwon", "Chungbuk", "Chungnam", "Jeonbuk", "Jeonnam", "Gyeongbuk", "Gyeongnam"
    );

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

            // 주요 도시는 검증 없이 통과
            String normalizedDestination = destination.trim().replaceAll("\\s+", "");
            boolean isMajorCity = MAJOR_KOREAN_CITIES.stream()
                .anyMatch(city -> city.replaceAll("\\s+", "").equalsIgnoreCase(normalizedDestination));

            if (isMajorCity) {
                log.debug("주요 도시 '{}' 검증 스킵 - 자동 통과", destination);
                continue;
            }

            try {
                // Perplexity API를 사용하여 목적지 유효성을 검증합니다.
                String prompt = String.format(
                    "Is '%s' a real, valid place name on Earth that can be used as a travel destination? Answer with only 'true' or 'false'.",
                    normalizedDestination
                );
                String response = perplexityClient.search(prompt).trim().toLowerCase();

                // LLM의 응답이 'false'이면 유효하지 않은 목적지로 판단합니다.
                if (response.contains("false")) {
                    log.warn("유효하지 않은 목적지: '{}'", destination);
                    return Optional.of(String.format("'%s'은(는) 유효한 목적지가 아닌 것 같아요. 다시 확인해주시겠어요?", destination));
                }
            } catch (Exception e) {
                log.error("목적지 '{}' 유효성 검증 중 API 오류 발생", destination, e);
                // [핵심 수정] API 호출에 실패하면, 검증을 통과시키는 대신 사용자에게 문제가 발생했음을 알립니다.
                return Optional.of("목적지 유효성을 확인하는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.");
            }
        }
        return Optional.empty();
    }
}