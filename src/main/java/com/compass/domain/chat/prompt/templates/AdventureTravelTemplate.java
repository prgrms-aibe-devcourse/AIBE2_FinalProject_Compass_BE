package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for adventure travel planning
 * Focuses on outdoor activities and extreme sports
 */
@Component
public class AdventureTravelTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        모험 여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 모험 타입: {{adventure_types}}
        - 체력 수준: {{fitness_level}}
        - 경험 수준: {{experience_level}}
        - 예산: {{budget}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 모험 여행 계획
        
        ### 액티비티 일정
        각 날짜별로:
        - 메인 모험 활동
        - 난이도 및 소요 시간
        - 필요 장비
        - 안전 브리핑
        
        ### 익스트림 스포츠
        - 번지점프/스카이다이빙
        - 래프팅/카약
        - 암벽등반/트레킹
        - 다이빙/서핑
        
        ### 자연 탐험
        - 트레킹 코스
        - 캠핑 장소
        - 야생 동물 관찰
        - 일출/일몰 포인트
        
        ### 장비 및 준비물
        - 필수 장비 리스트
        - 렌탈 가능 장비
        - 안전 장비
        - 의류 및 신발
        
        ### 안전 가이드
        - 현지 가이드 정보
        - 응급 처치 키트
        - 보험 필수 사항
        - 비상 연락처
        
        ### 체력 관리
        - 사전 준비 운동
        - 휴식 일정
        - 영양 보충
        - 수분 섭취
        
        ### 숙박 옵션
        - 베이스캠프
        - 산장/롯지
        - 캠핑/글램핑
        - 에코 리조트
        
        ### 예상 비용
        - 액티비티 비용
        - 장비 렌탈
        - 가이드 비용
        - 숙박 및 식사
        - 총 예상 비용
        """;
    
    public AdventureTravelTemplate() {
        super(
            "adventure_travel",
            "모험 여행 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "adventure_types", 
                        "fitness_level", "experience_level", "budget"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("모험") || 
               lowerInput.contains("어드벤처") ||
               lowerInput.contains("익스트림") ||
               lowerInput.contains("트레킹") ||
               lowerInput.contains("등산") ||
               lowerInput.contains("하이킹") ||
               lowerInput.contains("adventure") ||
               lowerInput.contains("extreme") ||
               lowerInput.contains("hiking") ||
               lowerInput.contains("trekking");
    }
}