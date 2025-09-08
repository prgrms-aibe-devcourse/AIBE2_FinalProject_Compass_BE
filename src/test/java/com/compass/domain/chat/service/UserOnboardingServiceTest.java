package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.OnboardingResponse;
import com.compass.domain.chat.dto.UserPreferenceDto;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * UserOnboardingService 단위 테스트
 * REQ-PERS-007: 콜드 스타트 해결을 위한 온보딩 서비스 테스트
 */
@Tag("unit")
@DisplayName("신규 사용자 온보딩 서비스 테스트")
class UserOnboardingServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserOnboardingService onboardingService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 테스트용 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .nickname("테스트사용자")
                .role(Role.USER)
                .build();
        
        // 리플렉션을 사용하여 private 필드 설정
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
            
            java.lang.reflect.Field createdAtField = User.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(testUser, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    @DisplayName("신규 사용자 확인 - 5분 이내 가입자")
    void testIsNewUser_NewUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        boolean isNewUser = onboardingService.isNewUser(1L);
        
        // Then
        assertThat(isNewUser).isTrue();
    }
    
    @Test
    @DisplayName("신규 사용자 확인 - 5분 이상 경과한 사용자")
    void testIsNewUser_OldUser() {
        // Given
        try {
            java.lang.reflect.Field createdAtField = User.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(testUser, LocalDateTime.now().minusMinutes(10));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        boolean isNewUser = onboardingService.isNewUser(1L);
        
        // Then
        assertThat(isNewUser).isFalse();
    }
    
    @Test
    @DisplayName("환영 메시지 생성 - 사용자 이름 포함")
    void testGenerateWelcomeMessage_WithName() {
        // When
        String welcomeMessage = onboardingService.generateWelcomeMessage("김철수");
        
        // Then
        assertThat(welcomeMessage).contains("김철수님!");
        assertThat(welcomeMessage).contains("환영합니다");
        assertThat(welcomeMessage).contains("Compass");
        assertThat(welcomeMessage).contains("여행");
    }
    
    @Test
    @DisplayName("환영 메시지 생성 - 사용자 이름 없음")
    void testGenerateWelcomeMessage_WithoutName() {
        // When
        String welcomeMessage = onboardingService.generateWelcomeMessage(null);
        
        // Then
        assertThat(welcomeMessage).contains("여행자님!");
        assertThat(welcomeMessage).contains("환영합니다");
    }
    
    @Test
    @DisplayName("선호도 질문 생성")
    void testGeneratePreferenceQuestions() {
        // When
        List<String> questions = onboardingService.generatePreferenceQuestions();
        
        // Then
        assertThat(questions).isNotEmpty();
        assertThat(questions).hasSize(5);
        assertThat(questions.get(0)).contains("스타일");
        assertThat(questions.get(1)).contains("기간");
        assertThat(questions.get(2)).contains("동반자");
        assertThat(questions.get(3)).contains("예산");
        assertThat(questions.get(4)).contains("테마");
    }
    
    @Test
    @DisplayName("예시 질문 생성")
    void testGenerateExampleQuestions() {
        // When
        List<String> examples = onboardingService.generateExampleQuestions();
        
        // Then
        assertThat(examples).isNotEmpty();
        assertThat(examples).hasSize(5);
        // 각 예시 질문이 여행 관련 키워드를 포함하는지 확인
        examples.forEach(example -> {
            assertThat(example).matches(".*여행.*|.*추천.*|.*일정.*|.*투어.*|.*계획.*|.*곳.*|.*코스.*");
        });
    }
    
    @Test
    @DisplayName("온보딩 응답 생성")
    void testCreateOnboardingResponse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When
        OnboardingResponse response = onboardingService.createOnboardingResponse(1L);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getWelcomeMessage()).contains("테스트사용자님!");
        assertThat(response.getPreferenceQuestions()).hasSize(5);
        assertThat(response.getExampleQuestions()).hasSize(5);
        assertThat(response.isNewUser()).isTrue();
    }
    
    @Test
    @DisplayName("온보딩 응답 생성 - 사용자 없음")
    void testCreateOnboardingResponse_UserNotFound() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> onboardingService.createOnboardingResponse(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }
    
    @Test
    @DisplayName("사용자 선호도 저장")
    void testSaveUserPreferences() {
        // Given
        UserPreferenceDto preferences = UserPreferenceDto.builder()
                .travelStyles(Arrays.asList("휴양", "관광"))
                .preferredDuration("3박4일")
                .companionType("가족")
                .budgetRange("중간")
                .interests(Arrays.asList("역사/문화", "자연"))
                .build();
        
        // When & Then - 현재는 로깅만 하므로 예외가 발생하지 않는지만 확인
        onboardingService.saveUserPreferences(1L, preferences);
    }
    
    @Test
    @DisplayName("현재 계절 반환")
    void testGetCurrentSeason() {
        // Given
        // 현재 계절 확인
        int currentMonth = LocalDateTime.now().getMonthValue();
        String expectedSeason;
        if (currentMonth >= 3 && currentMonth <= 5) {
            expectedSeason = "봄";
        } else if (currentMonth >= 6 && currentMonth <= 8) {
            expectedSeason = "여름";
        } else if (currentMonth >= 9 && currentMonth <= 11) {
            expectedSeason = "가을";
        } else {
            expectedSeason = "겨울";
        }
        
        // When
        // 여러 번 실행해서 계절 질문이 포함되는지 확인
        boolean hasSeasonQuestion = false;
        for (int i = 0; i < 10; i++) {
            List<String> examples = onboardingService.generateExampleQuestions();
            if (examples.stream().anyMatch(example -> example.contains(expectedSeason))) {
                hasSeasonQuestion = true;
                break;
            }
        }
        
        // Then
        // 10번 중 한번은 계절 질문이 포함되어야 함
        assertThat(hasSeasonQuestion).isTrue();
    }
}