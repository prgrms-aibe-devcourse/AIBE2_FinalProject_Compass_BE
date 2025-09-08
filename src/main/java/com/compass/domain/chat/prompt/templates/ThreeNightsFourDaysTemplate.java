package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for 3N4D (3박 4일) trip itinerary generation
 * Covers four-day travel plans with three nights accommodation
 */
@Component
public class ThreeNightsFourDaysTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        3박 4일 여행 일정을 계획해드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 여행 스타일: {{travel_style}}
        - 예산: {{budget}}원
        - 동행자: {{companions}}
        - 숙박 선호: {{accommodation_preference}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 3박 4일 여행 일정
        
        ### Day 1 - {{start_date}} (도착 및 적응)
        
        #### 오전 (09:00 - 12:00)
        - 출발 및 이동
        - 교통편 이용 (비행기/기차/버스)
        - 현지 도착
        
        #### 점심 (12:00 - 13:30)
        - 도착 후 첫 식사
        - 가벼운 현지 음식
        
        #### 오후 (13:30 - 18:00)
        - 숙소 체크인
        - 주변 지역 탐방
        - 가벼운 관광 시작
        
        #### 저녁 (18:00 - 20:00)
        - 현지 대표 맛집
        - 지역 특산물 체험
        
        #### 야간 (20:00 - 22:00)
        - 야경 명소 방문
        - 휴식 및 다음날 준비
        
        ### Day 2 - (메인 관광 1일차)
        
        #### 오전 (09:00 - 12:00)
        - 조식 후 출발
        - 핵심 관광지 A 방문
        - 문화/역사 체험
        
        #### 점심 (12:00 - 13:30)
        - 관광지 인근 맛집
        - 현지인 추천 메뉴
        
        #### 오후 (13:30 - 18:00)
        - 핵심 관광지 B 방문
        - 액티비티 체험
        - 포토존 방문
        
        #### 저녁 (18:00 - 20:00)
        - 특색 있는 저녁 식사
        - 테마 레스토랑
        
        #### 야간 (20:00 - 22:00)
        - 자유 시간
        - 야시장 또는 공연 관람
        
        ### Day 3 - (메인 관광 2일차)
        
        #### 오전 (09:00 - 12:00)
        - 테마별 관광 코스
        - 자연 경관 탐방
        - 특별 체험 프로그램
        
        #### 점심 (12:00 - 13:30)
        - 미식 탐방
        - 코스 요리 또는 뷔페
        
        #### 오후 (13:30 - 18:00)
        - 쇼핑 타임
        - 기념품 구매
        - 카페 투어
        
        #### 저녁 (18:00 - 20:00)
        - 현지 전통 음식
        - 특별한 다이닝 경험
        
        #### 야간 (20:00 - 22:00)
        - 나이트 투어
        - 바 또는 라운지 방문
        
        ### Day 4 - {{end_date}} (마무리 및 귀가)
        
        #### 오전 (09:00 - 11:00)
        - 체크아웃
        - 마지막 관광지 방문
        - 추가 쇼핑
        
        #### 점심 (11:00 - 12:30)
        - 브런치 또는 가벼운 식사
        - 공항/역 이동 준비
        
        #### 오후 (12:30 - 17:00)
        - 귀가 이동
        - 안전한 도착
        
        ### 숙박 정보
        - 숙소 유형: {{accommodation_preference}}
        - 3박 총 예상 비용
        - 지역별 추천 숙소
        - 조식 포함 여부
        - 부대시설 정보
        
        ### 상세 예상 비용
        - 교통비 
          * 왕복 대중교통/항공
          * 현지 교통비 (3일)
        - 숙박비 (3박)
        - 식사비 
          * 조식 3회
          * 중식 4회
          * 석식 3회
          * 간식/카페
        - 입장료 및 체험비
        - 쇼핑 예산
        - 예비비 (10%)
        - **총 예상 비용**
        
        ### 준비물 체크리스트
        - 여행 서류 (신분증, 예약 확인서)
        - 3박 4일 의류
        - 세면도구 및 개인용품
        - 전자기기 및 충전기
        - 의약품
        - 여행 보험
        
        ### 추천 일정 조합
        - 문화/역사 중심 코스
        - 자연/힐링 중심 코스
        - 미식/쇼핑 중심 코스
        - 액티비티 중심 코스
        
        ### 시즌별 팁
        - 성수기/비수기 고려사항
        - 날씨별 준비사항
        - 현지 행사 및 축제 정보
        
        여유롭고 알찬 3박 4일 일정으로 특별한 추억을 만들 수 있도록 계획해주세요.
        """;
    
    public ThreeNightsFourDaysTemplate() {
        super(
            "three_nights_four_days",
            "3박 4일 여행 일정 템플릿",
            TEMPLATE,
            new String[]{"destination", "start_date", "end_date", "travel_style", 
                        "budget", "companions", "accommodation_preference"},
            new String[]{"special_requirements"}
        );
    }
    
    @Override
    public boolean supports(String userInput) {
        if (userInput == null) return false;
        
        String lowerInput = userInput.toLowerCase();
        return lowerInput.contains("3박 4일") || 
               lowerInput.contains("3박4일") ||
               lowerInput.contains("삼박 사일") ||
               lowerInput.contains("3n4d") ||
               (lowerInput.contains("4일") && lowerInput.contains("여행"));
    }
}