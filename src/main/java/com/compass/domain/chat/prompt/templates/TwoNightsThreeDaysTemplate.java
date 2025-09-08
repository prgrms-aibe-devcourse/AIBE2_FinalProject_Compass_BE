package com.compass.domain.chat.prompt.templates;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;
import org.springframework.stereotype.Component;

/**
 * Template for 2N3D (2박 3일) trip itinerary generation
 * Covers three-day travel plans with two nights accommodation
 */
@Component
public class TwoNightsThreeDaysTemplate extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        2박 3일 여행 일정을 계획해드리겠습니다.
        
        여행 정보:
        - 목적지: {{destination}}
        - 여행 기간: {{start_date}} ~ {{end_date}}
        - 여행 스타일: {{travel_style}}
        - 예산: {{budget}}원
        - 동행자: {{companions}}
        - 숙박 선호: {{accommodation_preference}}
        {{special_requirements}}
        
        다음과 같은 구조로 일정을 생성해주세요:
        
        ## 2박 3일 여행 일정
        
        ### Day 1 - {{start_date}} (도착일)
        
        #### 오전 (09:00 - 12:00)
        - 출발 및 이동
        - 숙소 도착 및 짐 보관
        - 가벼운 주변 탐방
        
        #### 점심 (12:00 - 13:30)
        - 첫 현지 식사
        - 지역 대표 메뉴 체험
        
        #### 오후 (13:30 - 18:00)
        - 주요 관광지 1-2곳 방문
        - 여유로운 일정으로 시작
        - 사진 촬영 명소
        
        #### 저녁 (18:00 - 20:00)
        - 지역 맛집 저녁 식사
        - 현지 분위기 체험
        
        #### 야간 (20:00 - 22:00)
        - 야경 명소 또는 야시장
        - 숙소 체크인 및 휴식
        
        ### Day 2 - (본격 관광일)
        
        #### 오전 (09:00 - 12:00)
        - 호텔 조식 또는 브런치
        - 핵심 관광지 방문
        - 체험 프로그램 참여
        
        #### 점심 (12:00 - 13:30)
        - 특색 있는 점심 식사
        - 현지인 추천 맛집
        
        #### 오후 (13:30 - 18:00)
        - 테마별 관광 코스
        - 문화/역사/자연 탐방
        - 액티비티 또는 체험
        
        #### 저녁 (18:00 - 20:00)
        - 특별한 저녁 식사
        - 뷰맛집 또는 유명 레스토랑
        
        #### 야간 (20:00 - 22:00)
        - 자유 시간
        - 휴식 또는 추가 관광
        
        ### Day 3 - {{end_date}} (귀가일)
        
        #### 오전 (09:00 - 11:00)
        - 체크아웃
        - 마지막 관광지 방문
        - 기념품 쇼핑
        
        #### 점심 (11:00 - 12:30)
        - 마지막 현지 식사
        - 포장 가능한 간식 구매
        
        #### 오후 (12:30 - 17:00)
        - 추가 관광 또는 쇼핑
        - 귀가 준비
        - 안전한 귀가
        
        ### 숙박 정보
        - 숙소 유형: {{accommodation_preference}}
        - 1박당 예상 비용
        - 추천 지역 및 접근성
        - 예약 팁
        
        ### 예상 비용 내역
        - 교통비: 왕복 및 현지 이동
        - 숙박비: 2박 총액
        - 식사비: 총 8끼 예상
        - 입장료 및 활동비
        - 쇼핑 및 기타
        - **총 예상 비용**
        
        ### 준비물 및 팁
        - 2박 3일 필수 준비물
        - 계절별 의류 준비
        - 현지 교통 카드/패스
        - 사전 예약 필요 항목
        - 환전 및 결제 팁
        
        ### 추천 코스
        - 가족 여행 코스
        - 연인 여행 코스
        - 친구 여행 코스
        
        균형 잡힌 일정으로 여행의 즐거움을 극대화하는 계획을 세워주세요.
        """;
    
    public TwoNightsThreeDaysTemplate() {
        super(
            "two_nights_three_days",
            "2박 3일 여행 일정 템플릿",
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
        return lowerInput.contains("2박 3일") || 
               lowerInput.contains("2박3일") ||
               lowerInput.contains("이박 삼일") ||
               lowerInput.contains("2n3d") ||
               (lowerInput.contains("3일") && lowerInput.contains("여행"));
    }
}