package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for day trip itinerary generation
 * Covers single-day travel plans without accommodation
 */
@Component
public class DayTripTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        당일치기 여행 일정을 계획해드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 날짜: {{travel_date}}
        - 출발 시간: {{start_time}}
        - 종료 시간: {{end_time}}
        - 여행 스타일: {{travel_style}}
        - 예산: {{budget}}원
        - 동행자: {{companions}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 당일치기 여행 일정
        
        ### 오전 일정 ({{start_time}} - 12:00)
        - 주요 활동 및 관광지 방문
        - 이동 시간 고려
        - 추천 교통수단
        
        ### 점심 (12:00 - 13:30)
        - 현지 맛집 추천
        - 예산에 맞는 메뉴 제안
        
        ### 오후 일정 (13:30 - {{end_time}})
        - 오후 활동 및 관광지
        - 쇼핑이나 카페 방문
        - 여유로운 일정 구성
        
        ### 예상 비용
        - 교통비
        - 식사비
        - 입장료 및 활동비
        - 총 예상 비용
        
        ### 준비물 및 팁
        - 필수 준비물
        - 현지 팁
        - 주의사항
        
        일정은 이동 시간을 충분히 고려하고, 무리하지 않는 선에서 구성해주세요.
        """;
    
    public DayTripTemplate() {
        super(
            "day_trip",
            "당일치기 여행 일정 템플릿",
            TEMPLATE,
            new String[]{"destination", "travel_date", "start_time", "end_time", 
                        "travel_style", "budget", "companions"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("당일치기") || 
               lowerInput.contains("당일 여행") ||
               lowerInput.contains("일일 투어") ||
               lowerInput.contains("day trip") ||
               (lowerInput.contains("하루") && lowerInput.contains("여행"));
    }
}