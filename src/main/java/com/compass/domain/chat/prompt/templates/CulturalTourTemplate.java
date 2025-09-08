package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for cultural and historical tour planning
 * Focuses on museums, heritage sites, and cultural experiences
 */
@Component
public class CulturalTourTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        문화 탐방 여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 관심 분야: {{interests}}
        - 예산: {{budget}}
        - 가이드 선호도: {{guide_preference}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 문화 탐방 계획
        
        ### 주요 문화 명소
        - UNESCO 세계문화유산
        - 역사적 건축물
        - 중요 박물관
        - 미술관 및 갤러리
        
        ### 일정별 문화 체험
        각 날짜별로:
        - 문화 유적지 방문
        - 박물관/미술관 투어
        - 전통 공연 관람
        - 현지 문화 체험
        
        ### 현지 전통 체험
        - 전통 공예 워크샵
        - 전통 의상 체험
        - 전통 차/술 시음
        - 명상/템플스테이
        
        ### 문화 공연
        - 전통 음악/무용
        - 오페라/연극
        - 현지 축제
        - 거리 공연
        
        ### 역사 투어
        - 가이드 투어 일정
        - 오디오 가이드 정보
        - 도보 역사 투어
        - 테마별 투어
        
        ### 현지 문화 교류
        - 언어 교환
        - 홈스테이
        - 현지인과의 만남
        - 문화 교류 프로그램
        
        ### 전통 음식 체험
        - 전통 시장 투어
        - 쿠킹 클래스
        - 전통 레스토랑
        - 로컬 음식 투어
        
        ### 예상 비용
        - 입장료
        - 가이드 비용
        - 체험 프로그램
        - 식사 및 숙박
        - 총 예상 비용
        
        ### 문화 에티켓
        - 복장 규정
        - 사진 촬영 규칙
        - 종교 시설 예절
        - 현지 관습
        """;
    
    public CulturalTourTemplate() {
        super(
            "cultural_tour",
            "문화 탐방 여행 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "interests", 
                        "budget", "guide_preference"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("문화") || 
               lowerInput.contains("역사") ||
               lowerInput.contains("박물관") ||
               lowerInput.contains("미술관") ||
               lowerInput.contains("유적") ||
               lowerInput.contains("cultural") ||
               lowerInput.contains("history") ||
               lowerInput.contains("museum") ||
               lowerInput.contains("heritage");
    }
}