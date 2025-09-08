package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for backpacking/budget travel planning
 * Focuses on budget optimization and authentic experiences
 */
@Component
public class BackpackingTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        배낭여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 일일 예산: {{daily_budget}}
        - 여행 스타일: {{travel_style}}
        - 숙박 선호도: {{accommodation_preference}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 배낭여행 계획
        
        ### 저렴한 숙소
        - 호스텔/게스트하우스
        - 도미토리 vs 개인실
        - 위치 및 접근성
        - 무료 편의시설
        
        ### 일정별 활동
        각 날짜별로:
        - 무료/저렴한 관광지
        - 현지 체험 활동
        - 도보 투어
        - 자연 명소
        
        ### 현지 교통
        - 대중교통 이용법
        - 교통 패스/카드
        - 도보/자전거 경로
        - 히치하이킹 팁
        
        ### 식사 전략
        - 현지 시장/길거리 음식
        - 자취 가능한 숙소
        - 저렴한 현지 식당
        - 무료 조식 활용
        
        ### 배낭여행 꿀팁
        - 현지인과 교류 방법
        - 무료 와이파이 스팟
        - 환전 및 ATM 위치
        - 짐 보관 서비스
        
        ### 안전 수칙
        - 소지품 관리
        - 안전한 지역/위험 지역
        - 여행자 보험
        - 비상 연락처
        
        ### 예산 관리
        - 일별 지출 계획
        - 예비 자금
        - 절약 팁
        - 총 예상 비용
        
        ### 필수 준비물
        - 배낭 패킹 리스트
        - 다목적 아이템
        - 세탁 용품
        - 의약품
        """;
    
    public BackpackingTemplate() {
        super(
            "backpacking",
            "배낭여행 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "daily_budget", 
                        "travel_style", "accommodation_preference"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("배낭") || 
               lowerInput.contains("백패킹") ||
               lowerInput.contains("저예산") ||
               lowerInput.contains("호스텔") ||
               lowerInput.contains("backpack") ||
               lowerInput.contains("budget travel") ||
               lowerInput.contains("hostel");
    }
}