package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for family trip planning
 * Focuses on kid-friendly activities and family accommodations
 */
@Component
public class FamilyTripTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        가족 여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 가족 구성원: {{family_members}}명
        - 아이들 연령: {{children_ages}}
        - 예산: {{budget}}
        - 숙박 타입: {{accommodation_type}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 가족 여행 계획
        
        ### 숙박 정보
        - 가족 친화적인 숙소 추천
        - 패밀리룸 또는 커넥팅룸 옵션
        - 아이들을 위한 편의시설
        
        ### 일정별 활동
        각 날짜별로:
        - 아이들이 즐길 수 있는 활동
        - 교육적 가치가 있는 체험
        - 가족 모두가 함께할 수 있는 프로그램
        - 휴식 시간 고려
        
        ### 식사 계획
        - 아이 친화적인 레스토랑
        - 현지 음식 체험
        - 간식 및 음료 준비
        
        ### 이동 계획
        - 가족 단위 이동 수단
        - 카시트 필요 여부
        - 유모차 접근성
        
        ### 예상 비용
        - 숙박비
        - 식사비
        - 활동비
        - 교통비
        - 총 예상 비용
        
        ### 준비물
        - 아이들 필수품
        - 의약품
        - 여행 서류
        
        ### 안전 수칙
        - 비상 연락처
        - 의료 시설 위치
        - 아이 미아 방지 대책
        """;
    
    public FamilyTripTemplate() {
        super(
            "family_trip",
            "가족 여행 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "family_members", 
                        "children_ages", "budget", "accommodation_type"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("가족") || 
               lowerInput.contains("아이") ||
               lowerInput.contains("애들") ||
               lowerInput.contains("자녀") ||
               lowerInput.contains("family") ||
               lowerInput.contains("kids") ||
               lowerInput.contains("children");
    }
}