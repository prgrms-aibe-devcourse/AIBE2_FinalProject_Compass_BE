package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for business trip planning
 * Focuses on efficiency and professional needs
 */
@Component
public class BusinessTripTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        비즈니스 출장 일정을 계획해드리겠습니다.
        
        출장 정보:
        - 목적지: {{destination}}
        - 출장 기간: {{start_date}} ~ {{end_date}}
        - 미팅 장소: {{meeting_locations}}
        - 출장 목적: {{purpose}}
        - 예산: {{budget}}
        - 동행자: {{companions}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 비즈니스 출장 계획
        
        ### 숙박 정보
        - 비즈니스 호텔 추천
        - 미팅 장소와의 거리
        - 비즈니스 센터 시설
        - 와이파이 및 업무 환경
        
        ### 미팅 일정
        - 미팅 시간 및 장소
        - 이동 소요 시간
        - 준비 시간 확보
        - 백업 플랜
        
        ### 교통 계획
        - 공항 이동
        - 미팅 장소 이동
        - 현지 교통 수단
        - 택시/렌터카 옵션
        
        ### 식사 계획
        - 비즈니스 런치/디너 장소
        - 근처 간단한 식사 옵션
        - 룸서비스 가능 여부
        
        ### 업무 시간 외 활동
        - 네트워킹 기회
        - 간단한 관광
        - 운동 시설
        
        ### 비즈니스 서비스
        - 프린트/팩스 서비스
        - 회의실 예약
        - 통역 서비스
        
        ### 예상 비용
        - 숙박비
        - 교통비
        - 식사비
        - 기타 비용
        - 총 예상 비용
        
        ### 체크리스트
        - 필요 서류
        - 비즈니스 복장
        - 프레젠테이션 자료
        - 명함 및 선물
        """;
    
    public BusinessTripTemplate() {
        super(
            "business_trip",
            "비즈니스 출장 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "meeting_locations", 
                        "purpose", "budget", "companions"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("출장") || 
               lowerInput.contains("비즈니스") ||
               lowerInput.contains("미팅") ||
               lowerInput.contains("업무") ||
               lowerInput.contains("business") ||
               lowerInput.contains("meeting") ||
               lowerInput.contains("conference");
    }
}