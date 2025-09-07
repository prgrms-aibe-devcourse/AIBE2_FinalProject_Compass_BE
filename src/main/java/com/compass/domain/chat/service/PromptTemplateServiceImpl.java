package com.compass.domain.chat.service;

import com.compass.domain.chat.detector.SimpleKeywordDetector;
import com.compass.domain.chat.dto.PromptRequest;
import com.compass.domain.chat.dto.PromptResponse;
import com.compass.domain.trip.entity.UserContext;
import com.compass.domain.trip.entity.TravelHistory;
import com.compass.domain.trip.repository.UserContextRepository;
import com.compass.domain.trip.repository.TravelHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PromptTemplateService 구현체
 * REQ-PROMPT-001, REQ-PROMPT-002, REQ-PROMPT-003 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {
    
    private final SimpleKeywordDetector keywordDetector;
    private final UserContextRepository userContextRepository;
    private final TravelHistoryRepository travelHistoryRepository;
    
    // 템플릿 정의
    private static final Map<String, String> TEMPLATES = new HashMap<>();
    
    static {
        TEMPLATES.put("daily_itinerary", 
            "당신은 전문 여행 플래너입니다.\n" +
            "목적지: {destination}\n" +
            "기간: {duration}\n" +
            "여행 유형: {travelType}\n" +
            "예산: {budget}\n" +
            "사용자 선호도: {preferences}\n" +
            "과거 여행 경험: {travelHistory}\n\n" +
            "위 정보를 바탕으로 일별 상세 여행 일정을 작성해주세요.");
            
        TEMPLATES.put("budget_optimization",
            "당신은 예산 최적화 여행 전문가입니다.\n" +
            "목적지: {destination}\n" +
            "예산: {budget}\n" +
            "기간: {duration}\n" +
            "우선순위: {priorities}\n\n" +
            "주어진 예산 내에서 최적의 가성비 여행 계획을 제안해주세요.");
            
        TEMPLATES.put("local_experience",
            "당신은 현지 문화 전문가입니다.\n" +
            "목적지: {destination}\n" +
            "관심사: {interests}\n" +
            "기간: {duration}\n\n" +
            "현지인들이 추천하는 장소와 경험을 중심으로 여행 계획을 제안해주세요.");
            
        TEMPLATES.put("destination_discovery",
            "당신은 여행지 추천 전문가입니다.\n" +
            "선호 여행 스타일: {travelStyle}\n" +
            "계절: {season}\n" +
            "예산: {budget}\n" +
            "관심사: {interests}\n\n" +
            "위 조건에 맞는 최적의 여행지를 추천해주세요.");
            
        TEMPLATES.put("travel_recommendation",
            "당신은 맞춤형 여행 추천 AI입니다.\n" +
            "사용자 정보: {userProfile}\n" +
            "과거 여행 경험: {travelHistory}\n" +
            "선호도: {preferences}\n\n" +
            "개인화된 여행 추천을 제공해주세요.");
            
        TEMPLATES.put("travel_planning",
            "당신은 종합 여행 계획 도우미입니다.\n" +
            "사용자 요청: {userQuery}\n" +
            "컨텍스트: {context}\n\n" +
            "포괄적인 여행 계획을 제공해주세요.");
    }
    
    @Override
    public String buildPrompt(String templateName, Map<String, Object> parameters) {
        String template = TEMPLATES.get(templateName);
        if (template == null) {
            log.warn("Template not found: {}, using default", templateName);
            template = TEMPLATES.get("travel_planning");
        }
        
        String prompt = template;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            prompt = prompt.replace(placeholder, value);
        }
        
        // 남은 플레이스홀더 제거
        prompt = prompt.replaceAll("\\{[^}]+\\}", "정보 없음");
        
        return prompt;
    }
    
    @Override
    public PromptResponse buildEnrichedPrompt(PromptRequest request) {
        // 사용자 컨텍스트 조회
        Long userId = request.getUserId() != null ? Long.parseLong(request.getUserId()) : null;
        Map<String, Object> enrichedParams = new HashMap<>(request.getParameters());
        
        if (userId != null) {
            // 사용자 컨텍스트 추가
            userContextRepository.findByUserId(userId).ifPresent(context -> {
                enrichedParams.put("preferences", formatPreferences(context));
                enrichedParams.put("userProfile", formatUserProfile(context));
            });
            
            // 여행 히스토리 추가
            List<TravelHistory> histories = travelHistoryRepository.findByUserId(userId, 
                org.springframework.data.domain.PageRequest.of(0, 3)).getContent();
            if (!histories.isEmpty()) {
                enrichedParams.put("travelHistory", formatTravelHistory(histories));
            }
        }
        
        // 프롬프트 생성
        String prompt = buildPrompt(request.getTemplateName(), enrichedParams);
        
        // 응답 생성
        PromptResponse response = new PromptResponse();
        response.setPrompt(prompt);
        response.setTemplateName(request.getTemplateName());
        response.setParameters(enrichedParams);
        response.setMetadata(Map.of(
            "timestamp", System.currentTimeMillis(),
            "enriched", true,
            "userId", userId != null ? userId : "anonymous"
        ));
        
        return response;
    }
    
    @Override
    public Set<String> getAvailableTemplates() {
        return TEMPLATES.keySet();
    }
    
    @Override
    public Map<String, Object> getTemplateDetails(String templateName) {
        Map<String, Object> details = new HashMap<>();
        details.put("name", templateName);
        details.put("template", TEMPLATES.get(templateName));
        
        // 템플릿에서 필수 파라미터 추출
        String template = TEMPLATES.get(templateName);
        if (template != null) {
            Set<String> parameters = extractTemplateParameters(template);
            details.put("requiredParameters", parameters);
        }
        
        return details;
    }
    
    @Override
    public String selectTemplate(String userQuery, Map<String, Object> context) {
        // SimpleKeywordDetector 사용
        return keywordDetector.detectTemplate(userQuery);
    }
    
    @Override
    public Map<String, Object> extractParameters(String templateName, String userInput, Map<String, Object> context) {
        Map<String, Object> parameters = new HashMap<>();
        
        // SimpleKeywordDetector로 기본 정보 추출
        Map<String, Object> detectedInfo = keywordDetector.extractAllInfo(userInput);
        
        // 감지된 정보를 파라미터로 변환
        if (detectedInfo.get("destination") != null) {
            parameters.put("destination", convertDestination((String) detectedInfo.get("destination")));
        }
        
        if (detectedInfo.get("duration") != null) {
            parameters.put("duration", convertDuration((String) detectedInfo.get("duration")));
        }
        
        if (detectedInfo.get("travelType") != null) {
            parameters.put("travelType", convertTravelType((String) detectedInfo.get("travelType")));
        }
        
        // 컨텍스트에서 추가 정보 병합
        if (context != null) {
            parameters.putAll(context);
        }
        
        // 원본 쿼리 추가
        parameters.put("userQuery", userInput);
        
        return parameters;
    }
    
    // Helper methods
    private Set<String> extractTemplateParameters(String template) {
        Set<String> parameters = new HashSet<>();
        int start = 0;
        while ((start = template.indexOf("{", start)) != -1) {
            int end = template.indexOf("}", start);
            if (end != -1) {
                parameters.add(template.substring(start + 1, end));
                start = end + 1;
            } else {
                break;
            }
        }
        return parameters;
    }
    
    private String formatPreferences(UserContext context) {
        StringBuilder sb = new StringBuilder();
        // TODO: Add specific preference fields once UserContext is updated
        sb.append("사용자 선호도 정보");
        return sb.toString();
    }
    
    private String formatUserProfile(UserContext context) {
        StringBuilder sb = new StringBuilder();
        if (context.getTravelCompanion() != null) {
            sb.append("동행 유형: ").append(context.getTravelCompanion()).append(", ");
        }
        // TODO: Add dietary and accessibility fields once UserContext is updated
        if (context.getTravelCompanion() != null) {
            sb.append(", 동행: ").append(context.getTravelCompanion());
        }
        return sb.toString();
    }
    
    private String formatTravelHistory(List<TravelHistory> histories) {
        return histories.stream()
            .limit(3) // 최근 3개만
            .map(h -> String.format("%s (%s, %d일, 평점: %d/5)",
                h.getDestination(),
                h.getTravelType(),
                h.getTripDuration(),
                h.getRating() != null ? h.getRating() : 0))
            .collect(Collectors.joining("; "));
    }
    
    private String convertDestination(String detected) {
        Map<String, String> destinationMap = Map.of(
            "seoul", "서울",
            "busan", "부산",
            "jeju", "제주도",
            "gangneung", "강릉",
            "gyeongju", "경주"
        );
        return destinationMap.getOrDefault(detected, detected);
    }
    
    private String convertDuration(String detected) {
        Map<String, String> durationMap = Map.of(
            "day_trip", "당일치기",
            "1night2days", "1박2일",
            "2nights3days", "2박3일",
            "3nights4days", "3박4일",
            "week", "일주일"
        );
        return durationMap.getOrDefault(detected, detected);
    }
    
    private String convertTravelType(String detected) {
        Map<String, String> typeMap = Map.of(
            "family", "가족 여행",
            "couple", "커플 여행",
            "solo", "혼자 여행",
            "friends", "친구와 여행",
            "business", "비즈니스 출장"
        );
        return typeMap.getOrDefault(detected, detected);
    }
}