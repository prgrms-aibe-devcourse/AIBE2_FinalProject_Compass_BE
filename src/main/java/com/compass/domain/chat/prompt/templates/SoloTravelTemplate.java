package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for solo travel planning
 * Focuses on safety, social opportunities, and personal growth
 */
@Component
public class SoloTravelTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        혼자 떠나는 여행 계획을 세워드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 예산: {{budget}}
        - 여행 목적: {{travel_purpose}}
        - 관심사: {{interests}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 솔로 여행 계획
        
        ### 안전한 숙박
        - 안전한 지역의 숙소
        - 소셜 호스텔 옵션
        - 개인실 vs 도미토리
        - 24시간 리셉션
        
        ### 혼자서도 즐거운 활동
        각 날짜별로:
        - 혼자 하기 좋은 관광
        - 가이드 투어 참여
        - 워킹 투어
        - 셀프 포토 스팟
        
        ### 소셜 기회
        - 미트업 이벤트
        - 그룹 투어
        - 쿠킹 클래스
        - 언어 교환 모임
        
        ### 개인 성장 활동
        - 명상/요가
        - 저널링 시간
        - 현지 문화 체험
        - 새로운 도전
        
        ### 혼자 식사하기
        - 바 좌석 있는 레스토랑
        - 카페 추천
        - 푸드 마켓
        - 테이크아웃 옵션
        
        ### 안전 가이드
        - 안전 지역/위험 지역
        - 긴급 연락처
        - 현지 SIM 카드
        - 위치 공유 앱
        
        ### 자유 시간 활용
        - 독서 장소
        - 산책 코스
        - 카페 작업
        - 휴식 공간
        
        ### 네트워킹
        - 현지인과 교류
        - 여행자 커뮤니티
        - 온라인 모임
        - 코워킹 스페이스
        
        ### 예상 비용
        - 숙박비
        - 식사비
        - 활동비
        - 교통비
        - 총 예상 비용
        
        ### 혼자 여행 팁
        - 짐 최소화
        - 시간 관리
        - 외로움 대처
        - 자신감 유지
        """;
    
    public SoloTravelTemplate() {
        super(
            "solo_travel",
            "솔로 여행 계획 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "budget", 
                        "travel_purpose", "interests"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("혼자") || 
               lowerInput.contains("솔로") ||
               lowerInput.contains("나홀로") ||
               lowerInput.contains("자유여행") ||
               lowerInput.contains("solo") ||
               lowerInput.contains("alone") ||
               lowerInput.contains("single travel");
    }
}