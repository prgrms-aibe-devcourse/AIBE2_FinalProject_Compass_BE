package com.compass.domain.chat.detector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 간단한 키워드 감지 시스템
 * REQ-PROMPT-002: SimpleKeywordDetector 구현
 * 사용자 메시지에서 키워드를 감지하여 적절한 템플릿 선택
 */
@Slf4j
@Component
public class SimpleKeywordDetector {
    
    private final Map<String, List<String>> templateKeywords;
    private final Map<String, List<String>> destinationKeywords;
    private final Map<String, List<String>> durationKeywords;
    private final Map<String, List<String>> travelTypeKeywords;
    
    public SimpleKeywordDetector() {
        this.templateKeywords = initializeTemplateKeywords();
        this.destinationKeywords = initializeDestinationKeywords();
        this.durationKeywords = initializeDurationKeywords();
        this.travelTypeKeywords = initializeTravelTypeKeywords();
    }
    
    /**
     * 템플릿별 키워드 초기화
     */
    private Map<String, List<String>> initializeTemplateKeywords() {
        Map<String, List<String>> keywords = new HashMap<>();
        
        // 일정 계획 관련
        keywords.put("daily_itinerary", Arrays.asList(
            "일정", "스케줄", "일차", "일차별", "상세일정", "시간표", 
            "itinerary", "schedule", "day by day", "daily plan"
        ));
        
        // 예산 최적화 관련
        keywords.put("budget_optimization", Arrays.asList(
            "예산", "저렴", "가성비", "절약", "비용", "얼마",
            "budget", "cheap", "affordable", "cost", "save money"
        ));
        
        // 현지 체험 관련
        keywords.put("local_experience", Arrays.asList(
            "현지", "로컬", "체험", "문화", "전통", "맛집", "숨은",
            "local", "authentic", "experience", "culture", "traditional"
        ));
        
        // 목적지 탐색 관련
        keywords.put("destination_discovery", Arrays.asList(
            "어디", "추천", "좋은곳", "명소", "관광지", "가볼만한",
            "where", "recommend", "suggest", "destination", "places to visit"
        ));
        
        // 여행 추천 관련
        keywords.put("travel_recommendation", Arrays.asList(
            "추천", "제안", "알려줘", "좋은", "베스트", "인기",
            "recommend", "suggest", "best", "popular", "top"
        ));
        
        // 기본 여행 계획
        keywords.put("travel_planning", Arrays.asList(
            "계획", "여행", "준비", "플랜", "일정짜기",
            "plan", "travel", "trip", "prepare", "planning"
        ));
        
        return keywords;
    }
    
    /**
     * 목적지 키워드 초기화
     */
    private Map<String, List<String>> initializeDestinationKeywords() {
        Map<String, List<String>> keywords = new HashMap<>();
        
        keywords.put("seoul", Arrays.asList("서울", "경복궁", "명동", "강남", "홍대", "Seoul"));
        keywords.put("busan", Arrays.asList("부산", "해운대", "광안리", "자갈치", "Busan"));
        keywords.put("jeju", Arrays.asList("제주", "제주도", "한라산", "성산", "Jeju"));
        keywords.put("gyeongju", Arrays.asList("경주", "불국사", "첨성대", "Gyeongju"));
        keywords.put("gangneung", Arrays.asList("강릉", "경포대", "정동진", "Gangneung"));
        keywords.put("jeonju", Arrays.asList("전주", "한옥마을", "Jeonju"));
        
        return keywords;
    }
    
    /**
     * 여행 기간 키워드 초기화
     */
    private Map<String, List<String>> initializeDurationKeywords() {
        Map<String, List<String>> keywords = new HashMap<>();
        
        keywords.put("day_trip", Arrays.asList("당일", "당일치기", "하루", "day trip", "one day"));
        keywords.put("1night2days", Arrays.asList("1박2일", "1박", "이틀", "overnight"));
        keywords.put("2nights3days", Arrays.asList("2박3일", "2박", "사흘", "3일"));
        keywords.put("3nights4days", Arrays.asList("3박4일", "3박", "나흘", "4일"));
        keywords.put("week", Arrays.asList("일주일", "7일", "주간", "week", "7 days"));
        
        return keywords;
    }
    
    /**
     * 여행 유형 키워드 초기화
     */
    private Map<String, List<String>> initializeTravelTypeKeywords() {
        Map<String, List<String>> keywords = new HashMap<>();
        
        keywords.put("family", Arrays.asList(
            "가족", "아이", "부모님", "가족여행", "온가족",
            "family", "kids", "children", "parents"
        ));
        
        keywords.put("couple", Arrays.asList(
            "커플", "연인", "둘이", "데이트", "로맨틱", "허니문",
            "couple", "romantic", "honeymoon", "date"
        ));
        
        keywords.put("solo", Arrays.asList(
            "혼자", "나홀로", "솔로", "혼행", "자유여행",
            "solo", "alone", "single", "myself"
        ));
        
        keywords.put("friends", Arrays.asList(
            "친구", "우정여행", "친구들", "동료",
            "friends", "buddy", "group"
        ));
        
        keywords.put("business", Arrays.asList(
            "출장", "비즈니스", "업무", "회의",
            "business", "work", "conference"
        ));
        
        return keywords;
    }
    
    /**
     * 사용자 메시지에서 템플릿 유형 감지
     */
    public String detectTemplate(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "travel_planning"; // 기본값
        }
        
        String lowerMessage = userMessage.toLowerCase();
        Map<String, Integer> scores = new HashMap<>();
        
        // 템플릿 우선순위 정의 (더 구체적인 템플릿이 높은 우선순위)
        Map<String, Integer> templatePriority = new HashMap<>();
        templatePriority.put("daily_itinerary", 6);
        templatePriority.put("budget_optimization", 5);
        templatePriority.put("local_experience", 4);
        templatePriority.put("destination_discovery", 3);
        templatePriority.put("travel_recommendation", 2);
        templatePriority.put("travel_planning", 1);
        
        // 각 템플릿에 대한 점수 계산
        for (Map.Entry<String, List<String>> entry : templateKeywords.entrySet()) {
            int score = 0;
            int matchCount = 0;
            
            for (String keyword : entry.getValue()) {
                if (lowerMessage.contains(keyword.toLowerCase())) {
                    // 키워드 매칭 점수 (길이 대신 매칭 횟수와 우선순위 고려)
                    score += 10; // 기본 점수
                    matchCount++;
                    
                    // 특정 키워드에 대한 추가 가중치
                    if (keyword.equals("예산") || keyword.equals("가성비") || 
                        keyword.equals("budget") || keyword.equals("cheap")) {
                        score += 20; // 예산 관련 키워드는 추가 점수
                    }
                }
            }
            
            // 우선순위 보너스 적용
            if (score > 0) {
                int priority = templatePriority.getOrDefault(entry.getKey(), 0);
                score = score * matchCount + (priority * 5);
                scores.put(entry.getKey(), score);
            }
        }
        
        // 가장 높은 점수의 템플릿 선택
        String selectedTemplate = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("travel_planning");
        
        log.debug("Detected template: {} for message: {}", selectedTemplate, userMessage);
        return selectedTemplate;
    }
    
    /**
     * 사용자 메시지에서 목적지 추출
     */
    public Optional<String> detectDestination(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return Optional.empty();
        }
        
        String lowerMessage = userMessage.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : destinationKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerMessage.contains(keyword.toLowerCase())) {
                    log.debug("Detected destination: {}", entry.getKey());
                    return Optional.of(entry.getKey());
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 사용자 메시지에서 여행 기간 추출
     */
    public Optional<String> detectDuration(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return Optional.empty();
        }
        
        String lowerMessage = userMessage.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : durationKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerMessage.contains(keyword.toLowerCase())) {
                    log.debug("Detected duration: {}", entry.getKey());
                    return Optional.of(entry.getKey());
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 사용자 메시지에서 여행 유형 추출
     */
    public Optional<String> detectTravelType(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return Optional.empty();
        }
        
        String lowerMessage = userMessage.toLowerCase();
        
        for (Map.Entry<String, List<String>> entry : travelTypeKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerMessage.contains(keyword.toLowerCase())) {
                    log.debug("Detected travel type: {}", entry.getKey());
                    return Optional.of(entry.getKey());
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * 사용자 메시지에서 모든 여행 관련 정보 추출
     */
    public Map<String, Object> extractAllInfo(String userMessage) {
        Map<String, Object> extractedInfo = new HashMap<>();
        
        // 템플릿 유형
        extractedInfo.put("template", detectTemplate(userMessage));
        
        // 목적지
        detectDestination(userMessage).ifPresent(dest -> 
            extractedInfo.put("destination", dest));
        
        // 여행 기간
        detectDuration(userMessage).ifPresent(duration -> 
            extractedInfo.put("duration", duration));
        
        // 여행 유형
        detectTravelType(userMessage).ifPresent(type -> 
            extractedInfo.put("travelType", type));
        
        // 원본 메시지
        extractedInfo.put("originalMessage", userMessage);
        
        log.info("Extracted info from message: {}", extractedInfo);
        return extractedInfo;
    }
}