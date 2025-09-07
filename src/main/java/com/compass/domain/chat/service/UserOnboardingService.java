package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.OnboardingResponse;
import com.compass.domain.chat.dto.UserPreferenceDto;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 신규 사용자 온보딩 서비스
 * REQ-PERS-007: 콜드 스타트 문제 해결을 위한 온보딩 구현
 * 
 * 주요 기능:
 * - 첫 사용자 환영 메시지 생성
 * - 기본 선호도 수집 폼 제공
 * - 예시 질문 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnboardingService {

    private final UserRepository userRepository;
    
    /**
     * 신규 사용자 체크
     */
    @Transactional(readOnly = true)
    public boolean isNewUser(Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    // 생성된지 5분 이내면 신규 사용자로 간주
                    return user.getCreatedAt().plusMinutes(5).isAfter(java.time.LocalDateTime.now());
                })
                .orElse(false);
    }
    
    /**
     * 환영 메시지 생성
     */
    public String generateWelcomeMessage(String userName) {
        return String.format(
            """
            안녕하세요, %s님! 🎉
            
            Compass 여행 AI 어시스턴트에 오신 것을 환영합니다.
            
            저는 당신의 완벽한 여행을 계획하는 데 도움을 드리기 위해 여기 있습니다.
            국내외 여행지 추천, 일정 계획, 맛집 정보, 숙박 예약까지 
            모든 여행 관련 질문에 답변해 드릴 수 있어요.
            
            더 나은 추천을 위해 몇 가지 선호도를 알려주시겠어요?
            """, userName != null ? userName : "여행자"
        );
    }
    
    /**
     * 기본 선호도 수집 질문 생성
     */
    public List<String> generatePreferenceQuestions() {
        List<String> questions = new ArrayList<>();
        
        questions.add("주로 어떤 스타일의 여행을 선호하시나요? (예: 휴양, 관광, 액티비티, 미식)");
        questions.add("선호하는 여행 기간은 어떻게 되시나요? (예: 당일치기, 1박2일, 3박4일 이상)");
        questions.add("함께 여행하는 동반자가 있으신가요? (예: 혼자, 친구, 가족, 연인)");
        questions.add("예산 범위를 알려주시면 더 정확한 추천이 가능해요. (예: 저예산, 중간, 고급)");
        questions.add("특별히 관심있는 여행 테마가 있으신가요? (예: 역사/문화, 자연, 쇼핑, 축제)");
        
        return questions;
    }
    
    /**
     * 예시 질문 생성 (사용자가 바로 시작할 수 있도록)
     */
    public List<String> generateExampleQuestions() {
        List<String> examples = new ArrayList<>();
        
        // 계절별 추천
        String currentSeason = getCurrentSeason();
        examples.add(String.format("%s에 가기 좋은 국내 여행지 추천해줘", currentSeason));
        
        // 인기 여행지
        examples.add("이번 주말에 서울에서 당일치기로 갈만한 곳 알려줘");
        examples.add("제주도 3박4일 일정 짜줘");
        examples.add("부산 2박3일 맛집 투어 코스 추천해줘");
        
        // 테마별 여행
        examples.add("아이와 함께 가기 좋은 가족 여행지 추천해줘");
        examples.add("연인과 함께 가기 좋은 로맨틱한 여행지 알려줘");
        examples.add("혼자 여행하기 좋은 안전한 해외 도시 추천해줘");
        
        // 예산별 여행
        examples.add("10만원으로 즐기는 강릉 1박2일 여행 계획 세워줘");
        
        // 무작위로 5개 선택
        return getRandomSublist(examples, 5);
    }
    
    /**
     * 온보딩 응답 생성
     */
    @Transactional
    public OnboardingResponse createOnboardingResponse(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        OnboardingResponse response = OnboardingResponse.builder()
                .welcomeMessage(generateWelcomeMessage(user.getNickname()))
                .preferenceQuestions(generatePreferenceQuestions())
                .exampleQuestions(generateExampleQuestions())
                .isNewUser(true)
                .build();
        
        log.info("Created onboarding response for new user: {}", userId);
        return response;
    }
    
    /**
     * 사용자 선호도 저장
     */
    @Transactional
    public void saveUserPreferences(Long userId, UserPreferenceDto preferences) {
        // TODO: UserPreference 엔티티가 생성되면 구현
        // 현재는 로그만 남김
        log.info("Saving preferences for user {}: {}", userId, preferences);
    }
    
    /**
     * 현재 계절 반환
     */
    private String getCurrentSeason() {
        int month = java.time.LocalDate.now().getMonthValue();
        if (month >= 3 && month <= 5) return "봄";
        if (month >= 6 && month <= 8) return "여름";
        if (month >= 9 && month <= 11) return "가을";
        return "겨울";
    }
    
    /**
     * 리스트에서 랜덤하게 n개 선택
     */
    private <T> List<T> getRandomSublist(List<T> list, int n) {
        List<T> result = new ArrayList<>(list);
        java.util.Collections.shuffle(result, new Random());
        return result.subList(0, Math.min(n, result.size()));
    }
}