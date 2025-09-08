package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for couple/romantic travel planning
 * Focuses on romantic activities and special occasions
 */
@Component
public class CoupleTripTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        커플을 위한 로맨틱한 여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 예산: {{budget}}
        - 특별한 기념일: {{occasion}}
        - 로맨스 수준: {{romance_level}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 커플 여행 계획
        
        ### 로맨틱한 숙소
        - 프라이빗한 공간
        - 특별한 뷰가 있는 객실
        - 커플 패키지 옵션
        - 스파/자쿠지 시설
        
        ### 로맨틱한 활동
        각 날짜별로:
        - 일출/일몰 명소
        - 프라이빗 투어
        - 커플 액티비티
        - 사진 명소
        
        ### 특별한 식사
        - 로맨틱한 레스토랑
        - 프라이빗 다이닝
        - 와인 & 디저트
        - 현지 특별 요리
        
        ### 스페셜 이벤트
        - 서프라이즈 이벤트
        - 기념일 축하
        - 특별한 선물 아이디어
        
        ### 커플 스파 & 휴식
        - 커플 마사지
        - 프라이빗 스파
        - 휴식 공간
        
        ### 예상 비용
        - 숙박비
        - 식사비
        - 활동비
        - 특별 이벤트 비용
        - 총 예상 비용
        
        ### 로맨틱 팁
        - 현지 로맨틱 문화
        - 추천 시간대
        - 분위기 연출 팁
        """;
    
    public CoupleTripTemplate() {
        super(
            "couple_trip",
            "커플 여행 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "budget", 
                        "occasion", "romance_level"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("커플") || 
               lowerInput.contains("연인") ||
               lowerInput.contains("신혼") ||
               lowerInput.contains("로맨틱") ||
               lowerInput.contains("둘이") ||
               lowerInput.contains("couple") ||
               lowerInput.contains("romantic") ||
               lowerInput.contains("honeymoon");
    }
}