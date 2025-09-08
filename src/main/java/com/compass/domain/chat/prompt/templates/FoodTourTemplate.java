package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for food and culinary tour planning
 * Focuses on local cuisine, restaurants, and food experiences
 */
@Component
public class FoodTourTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        미식 여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 음식 선호도: {{food_preferences}}
        - 알러지/제한사항: {{dietary_restrictions}}
        - 예산: {{budget}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 미식 여행 계획
        
        ### 필수 맛집 리스트
        - 미슐랭 레스토랑
        - 현지인 추천 맛집
        - 전통 음식점
        - 숨은 맛집
        
        ### 일정별 식사 계획
        각 날짜별로:
        **아침**
        - 추천 장소
        - 시그니처 메뉴
        - 예상 비용
        
        **점심**
        - 추천 장소
        - 시그니처 메뉴
        - 예상 비용
        
        **저녁**
        - 추천 장소
        - 시그니처 메뉴
        - 예상 비용
        
        ### 푸드 투어 & 체험
        - 시장 투어
        - 쿠킹 클래스
        - 와인/사케 테이스팅
        - 팜투테이블 체험
        
        ### 지역 특산품
        - 제철 음식
        - 지역 특산물
        - 전통 주류
        - 디저트 & 간식
        
        ### 길거리 음식
        - 유명 포장마차
        - 야시장
        - 푸드트럭
        - 간식거리
        
        ### 카페 & 베이커리
        - 유명 카페
        - 현지 베이커리
        - 디저트 전문점
        - 티 하우스
        
        ### 식재료 쇼핑
        - 현지 시장
        - 특산품 상점
        - 식재료 마트
        - 선물용 식품
        
        ### 예상 비용
        - 레스토랑 식사
        - 길거리 음식
        - 체험 프로그램
        - 식재료 구매
        - 총 예상 비용
        
        ### 미식 팁
        - 예약 필수 레스토랑
        - 피크 시간 피하기
        - 팁 문화
        - 현지 식사 예절
        """;
    
    public FoodTourTemplate() {
        super(
            "food_tour",
            "미식 여행 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "food_preferences", 
                        "dietary_restrictions", "budget"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("미식") || 
               lowerInput.contains("맛집") ||
               lowerInput.contains("음식") ||
               lowerInput.contains("요리") ||
               lowerInput.contains("먹방") ||
               lowerInput.contains("food") ||
               lowerInput.contains("culinary") ||
               lowerInput.contains("restaurant") ||
               lowerInput.contains("cuisine");
    }
}