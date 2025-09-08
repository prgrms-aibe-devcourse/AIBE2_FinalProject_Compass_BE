package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for luxury travel planning
 * Focuses on premium experiences and exclusive services
 */
@Component
public class LuxuryTravelTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        럭셔리 여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 예산: {{budget}}
        - 선호 럭셔리 수준: {{luxury_level}}
        - 특별 서비스: {{special_services}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 럭셔리 여행 계획
        
        ### 프리미엄 숙박
        - 5성급 호텔/리조트
        - 스위트룸/빌라 옵션
        - 버틀러 서비스
        - 익스클루시브 어메니티
        
        ### VIP 경험
        각 날짜별로:
        - 프라이빗 투어
        - 헬리콥터/요트 투어
        - 미슐랭 레스토랑
        - 익스클루시브 이벤트
        
        ### 프리미엄 교통
        - 프라이빗 제트/퍼스트 클래스
        - 리무진 서비스
        - 프라이빗 드라이버
        - 럭셔리 렌터카
        
        ### 파인 다이닝
        - 미슐랭 스타 레스토랑
        - 셰프 테이블
        - 와인 페어링
        - 프라이빗 다이닝
        
        ### 웰니스 & 스파
        - 프리미엄 스파 트리트먼트
        - 개인 트레이너
        - 웰니스 프로그램
        - 메디컬 스파
        
        ### 쇼핑 & 엔터테인먼트
        - 럭셔리 브랜드 쇼핑
        - 퍼스널 쇼퍼 서비스
        - VIP 라운지 액세스
        - 프라이빗 쇼/공연
        
        ### 컨시어지 서비스
        - 24시간 컨시어지
        - 예약 대행
        - 특별 요청 처리
        
        ### 예상 비용
        - 숙박비
        - 교통비
        - 식사 및 엔터테인먼트
        - 쇼핑 예산
        - 총 예상 비용
        """;
    
    public LuxuryTravelTemplate() {
        super(
            "luxury_travel",
            "럭셔리 여행 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "budget", 
                        "luxury_level", "special_services"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("럭셔리") || 
               lowerInput.contains("프리미엄") ||
               lowerInput.contains("5성급") ||
               lowerInput.contains("고급") ||
               lowerInput.contains("luxury") ||
               lowerInput.contains("premium") ||
               lowerInput.contains("vip") ||
               lowerInput.contains("first class");
    }
}