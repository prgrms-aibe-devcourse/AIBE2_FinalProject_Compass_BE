package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for relaxation and wellness travel planning
 * Focuses on rest, spa, and rejuvenation
 */
@Component
public class RelaxationTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        휴양과 힐링 여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 휴양 스타일: {{relaxation_type}}
        - 예산: {{budget}}
        - 숙박 스타일: {{accommodation_style}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 휴양 여행 계획
        
        ### 휴양 숙소
        - 리조트/호텔 추천
        - 스파 시설
        - 프라이빗 공간
        - 조용한 환경
        
        ### 스파 & 웰니스
        - 스파 트리트먼트
        - 마사지 프로그램
        - 온천/사우나
        - 명상/요가
        
        ### 여유로운 일정
        각 날짜별로:
        - 늦은 아침 식사
        - 가벼운 산책
        - 휴식 시간
        - 선셋 감상
        
        ### 힐링 액티비티
        - 요가/명상 클래스
        - 해변 산책
        - 자연 속 휴식
        - 독서/음악 감상
        
        ### 건강한 식사
        - 웰니스 레스토랑
        - 유기농 카페
        - 디톡스 메뉴
        - 룸서비스 옵션
        
        ### 자연 치유
        - 해변/호수
        - 숲속 산책로
        - 정원/공원
        - 전망 좋은 곳
        
        ### 조용한 활동
        - 미술관/갤러리
        - 조용한 카페
        - 북카페
        - 영화관
        
        ### 수면 & 휴식
        - 최적의 수면 환경
        - 낮잠 시간
        - 휴식 공간
        - 디지털 디톡스
        
        ### 예상 비용
        - 숙박비
        - 스파/웰니스
        - 식사비
        - 기타 활동
        - 총 예상 비용
        
        ### 휴양 팁
        - 최적의 시즌
        - 조용한 시간대
        - 예약 권장 사항
        - 휴식 극대화 방법
        """;
    
    public RelaxationTemplate() {
        super(
            "relaxation",
            "휴양 여행 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "relaxation_type", 
                        "budget", "accommodation_style"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("휴양") || 
               lowerInput.contains("힐링") ||
               lowerInput.contains("휴식") ||
               lowerInput.contains("스파") ||
               lowerInput.contains("웰니스") ||
               lowerInput.contains("relaxation") ||
               lowerInput.contains("wellness") ||
               lowerInput.contains("spa") ||
               lowerInput.contains("retreat");
    }
}