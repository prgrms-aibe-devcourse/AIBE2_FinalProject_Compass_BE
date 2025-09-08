package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for 1N2D (1박 2일) trip itinerary generation
 * Covers two-day travel plans with one night accommodation
 */
@Component
public class OneNightTwoDaysTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        1박 2일 여행 일정을 계획해드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 여행 스타일: {{travel_style}}
        - 예산: {{budget}}원
        - 동행자: {{companions}}
        - 숙박 선호: {{accommodation_preference}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 1박 2일 여행 일정
        
        ### Day 1 - {{start_date}}
        
        #### 오전 (09:00 - 12:00)
        - 출발 및 이동
        - 첫 번째 관광지 방문
        - 주요 활동
        
        #### 점심 (12:00 - 13:30)
        - 추천 맛집
        - 지역 특산물 메뉴
        
        #### 오후 (13:30 - 18:00)
        - 핵심 관광지 탐방
        - 체험 활동
        - 사진 명소 방문
        
        #### 저녁 (18:00 - 20:00)
        - 저녁 식사
        - 지역 맛집 또는 특별한 식당
        
        #### 야간 (20:00 - 22:00)
        - 야경 명소
        - 숙소 체크인 및 휴식
        
        ### 숙박
        - 추천 숙소 유형: {{accommodation_preference}}
        - 예상 숙박비: 예산 내 추천
        - 위치: 주요 관광지 접근성 고려
        
        ### Day 2 - {{end_date}}
        
        #### 오전 (09:00 - 12:00)
        - 호텔 조식 또는 현지 아침 식사
        - 오전 관광지 방문
        - 여유로운 일정
        
        #### 점심 (12:00 - 13:30)
        - 마지막 현지 맛집 체험
        - 선물 구매
        
        #### 오후 (13:30 - 17:00)
        - 추가 관광 또는 쇼핑
        - 귀가 준비
        - 여행 마무리
        
        ### 예상 비용 내역
        - 교통비: 왕복 및 현지 이동
        - 숙박비: 1박 기준
        - 식사비: 총 5끼 (점심2, 저녁1, 아침1, 간식)
        - 입장료 및 활동비
        - 기타 비용
        - **총 예상 비용**
        
        ### 준비물 및 팁
        - 1박 2일 필수 준비물
        - 계절별 준비 사항
        - 현지 교통 팁
        - 예약 필요 사항
        
        무리하지 않으면서도 알찬 1박 2일 일정을 구성해주세요.
        """;
    
    public OneNightTwoDaysTemplate() {
        super(
            "one_night_two_days",
            "1박 2일 여행 일정 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "travel_style", 
                        "budget", "companions", "accommodation_preference"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("1박 2일") || 
               lowerInput.contains("1박2일") ||
               lowerInput.contains("일박 이일") ||
               lowerInput.contains("1n2d") ||
               (lowerInput.contains("이틀") && lowerInput.contains("여행"));
    }
}