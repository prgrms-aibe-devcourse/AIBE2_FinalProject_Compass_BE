package com.compass.domain.trip.service;

import com.compass.domain.trip.dto.TravelStyleItem;
import com.compass.domain.trip.dto.TravelStylePreferenceRequest;
import com.compass.domain.trip.dto.TravelStylePreferenceResponse;
import com.compass.domain.trip.entity.UserPreference;
import com.compass.domain.trip.exception.DuplicateTravelStyleException;
import com.compass.domain.trip.exception.InvalidWeightSumException;
import com.compass.domain.trip.repository.UserPreferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.compass.domain.trip.dto.BudgetRequest;
import com.compass.domain.trip.dto.BudgetResponse;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    @Test
    @DisplayName("여행 스타일 선호도 설정 - 성공")
    void setTravelStylePreferences_Success() {
        // Given
        Long userId = 1L;
        TravelStylePreferenceRequest request = TravelStylePreferenceRequest.builder()
                .preferences(Arrays.asList(
                        TravelStyleItem.builder().travelStyle("RELAXATION").weight(new BigDecimal("0.5")).build(),
                        TravelStyleItem.builder().travelStyle("SIGHTSEEING").weight(new BigDecimal("0.3")).build(),
                        TravelStyleItem.builder().travelStyle("ACTIVITY").weight(new BigDecimal("0.2")).build()
                ))
                .build();

        when(userPreferenceRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<UserPreference> preferences = invocation.getArgument(0);
            // Mock 저장된 엔티티들에 ID와 시간 설정
            preferences.forEach(pref -> {
                // Reflection을 사용하여 private 필드 설정 (실제로는 Builder나 생성자 사용)
            });
            return preferences;
        });

        // When
        TravelStylePreferenceResponse response = userPreferenceService.setTravelStylePreferences(userId, request);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalWeight()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(response.getPreferences()).hasSize(3);
        assertThat(response.getMessage()).isEqualTo("여행 스타일 선호도가 성공적으로 설정되었습니다.");

        verify(userPreferenceRepository).deleteByUserIdAndPreferenceType(userId, "TRAVEL_STYLE");
        verify(userPreferenceRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("여행 스타일 선호도 설정 - 가중치 합계 오류")
    void setTravelStylePreferences_InvalidWeightSum() {
        // Given
        Long userId = 1L;
        TravelStylePreferenceRequest request = TravelStylePreferenceRequest.builder()
                .preferences(Arrays.asList(
                        TravelStyleItem.builder().travelStyle("RELAXATION").weight(new BigDecimal("0.6")).build(),
                        TravelStyleItem.builder().travelStyle("SIGHTSEEING").weight(new BigDecimal("0.3")).build(),
                        TravelStyleItem.builder().travelStyle("ACTIVITY").weight(new BigDecimal("0.2")).build()
                ))
                .build();

        // When & Then
        assertThatThrownBy(() -> userPreferenceService.setTravelStylePreferences(userId, request))
                .isInstanceOf(InvalidWeightSumException.class)
                .hasMessageContaining("가중치 합계가 1이 아닙니다");

        verify(userPreferenceRepository, never()).deleteByUserIdAndPreferenceType(any(), any());
        verify(userPreferenceRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("여행 스타일 선호도 설정 - 중복된 여행 스타일")
    void setTravelStylePreferences_DuplicateStyle() {
        // Given
        Long userId = 1L;
        TravelStylePreferenceRequest request = TravelStylePreferenceRequest.builder()
                .preferences(Arrays.asList(
                        TravelStyleItem.builder().travelStyle("RELAXATION").weight(new BigDecimal("0.5")).build(),
                        TravelStyleItem.builder().travelStyle("RELAXATION").weight(new BigDecimal("0.3")).build(),
                        TravelStyleItem.builder().travelStyle("ACTIVITY").weight(new BigDecimal("0.2")).build()
                ))
                .build();

        // When & Then
        assertThatThrownBy(() -> userPreferenceService.setTravelStylePreferences(1L, request))
                .isInstanceOf(DuplicateTravelStyleException.class);
    }
    
    // ================= 예산 수준 테스트 =================
    
    @Test
    @DisplayName("새로운 예산 수준 설정 - 성공")
    void setBudgetLevel_Success() {
        // Given
        Long userId = 1L;
        BudgetRequest request = BudgetRequest.builder().budgetLevel("STANDARD").build();
        
        when(userPreferenceRepository.findByUserIdAndPreferenceType(userId, "BUDGET_LEVEL")).thenReturn(new ArrayList<>());
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BudgetResponse response = userPreferenceService.setOrUpdateBudgetLevel(userId, request);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getBudgetLevel()).isEqualTo("STANDARD");
        assertThat(response.getMessage()).contains("성공적으로 설정");
        verify(userPreferenceRepository, times(1)).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("기존 예산 수준 수정 - 성공")
    void updateBudgetLevel_Success() {
        // Given
        Long userId = 1L;
        BudgetRequest request = BudgetRequest.builder().budgetLevel("LUXURY").build();
        UserPreference existingPreference = UserPreference.builder()
                .userId(userId)
                .preferenceType("BUDGET_LEVEL")
                .preferenceKey("BUDGET")
                .build();

        when(userPreferenceRepository.findByUserIdAndPreferenceType(userId, "BUDGET_LEVEL")).thenReturn(List.of(existingPreference));
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BudgetResponse response = userPreferenceService.setOrUpdateBudgetLevel(userId, request);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getBudgetLevel()).isEqualTo("LUXURY");
        verify(userPreferenceRepository, times(1)).save(existingPreference);
        assertThat(existingPreference.getPreferenceKey()).isEqualTo("LUXURY");
    }

    @Test
    @DisplayName("설정된 예산 수준 조회 - 성공")
    void getBudgetLevel_Success() {
        // Given
        Long userId = 1L;
        UserPreference existingPreference = UserPreference.builder()
                .userId(userId)
                .preferenceType("BUDGET_LEVEL")
                .preferenceKey("STANDARD")
                .build();
        
        when(userPreferenceRepository.findByUserIdAndPreferenceType(userId, "BUDGET_LEVEL")).thenReturn(List.of(existingPreference));

        // When
        BudgetResponse response = userPreferenceService.getBudgetLevel(userId);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getBudgetLevel()).isEqualTo("STANDARD");
    }

    @Test
    @DisplayName("미설정 예산 수준 조회")
    void getBudgetLevel_NotFound() {
        // Given
        Long userId = 1L;
        when(userPreferenceRepository.findByUserIdAndPreferenceType(userId, "BUDGET_LEVEL")).thenReturn(new ArrayList<>());

        // When
        BudgetResponse response = userPreferenceService.getBudgetLevel(userId);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getBudgetLevel()).isNull();
        assertThat(response.getMessage()).isEqualTo("설정된 예산 수준이 없습니다.");
    }

    @Test
    @DisplayName("유효하지 않은 BudgetLevel 문자열로 설정 요청")
    void setBudgetLevel_InvalidLevelString() {
        // Given
        Long userId = 1L;
        BudgetRequest request = BudgetRequest.builder().budgetLevel("INVALID").build();

        // When & Then
        assertThatThrownBy(() -> userPreferenceService.setOrUpdateBudgetLevel(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 예산 수준입니다.");
    }

    @Test
    @DisplayName("여행 스타일 선호도 조회 - 성공")
    void getTravelStylePreferences_Success() {
        // Given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();
        
        List<UserPreference> mockPreferences = Arrays.asList(
                UserPreference.builder()
                        .userId(userId)
                        .preferenceType("TRAVEL_STYLE")
                        .preferenceKey("RELAXATION")
                        .preferenceValue(new BigDecimal("0.5"))
                        .description("휴양 및 힐링을 중심으로 한 여행")
                        .build()
        );
        
        when(userPreferenceRepository.findTravelStylePreferencesByUserId(userId))
                .thenReturn(mockPreferences);

        // When
        TravelStylePreferenceResponse response = userPreferenceService.getTravelStylePreferences(userId);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getPreferences()).hasSize(1);
        assertThat(response.getTotalWeight()).isEqualTo(new BigDecimal("0.5"));

        verify(userPreferenceRepository).findTravelStylePreferencesByUserId(userId);
    }

    @Test
    @DisplayName("여행 스타일 선호도 조회 - 선호도 미설정")
    void getTravelStylePreferences_NotFound() {
        // Given
        Long userId = 1L;
        when(userPreferenceRepository.findTravelStylePreferencesByUserId(userId))
                .thenReturn(List.of());

        // When
        TravelStylePreferenceResponse response = userPreferenceService.getTravelStylePreferences(userId);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getPreferences()).isEmpty();
        assertThat(response.getTotalWeight()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.getMessage()).isEqualTo("설정된 여행 스타일 선호도가 없습니다.");

        verify(userPreferenceRepository).findTravelStylePreferencesByUserId(userId);
    }

    @Test
    @DisplayName("여행 스타일 선호도 수정 - 성공")
    void updateTravelStylePreferences_Success() {
        // Given
        Long userId = 1L;
        TravelStylePreferenceRequest request = TravelStylePreferenceRequest.builder()
                .preferences(Arrays.asList(
                        TravelStyleItem.builder().travelStyle("RELAXATION").weight(new BigDecimal("0.4")).build(),
                        TravelStyleItem.builder().travelStyle("SIGHTSEEING").weight(new BigDecimal("0.4")).build(),
                        TravelStyleItem.builder().travelStyle("ACTIVITY").weight(new BigDecimal("0.2")).build()
                ))
                .build();

        when(userPreferenceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TravelStylePreferenceResponse response = userPreferenceService.updateTravelStylePreferences(userId, request);

        // Then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalWeight()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(response.getPreferences()).hasSize(3);
        assertThat(response.getMessage()).isEqualTo("여행 스타일 선호도가 성공적으로 수정되었습니다.");

        verify(userPreferenceRepository).deleteByUserIdAndPreferenceType(userId, "TRAVEL_STYLE");
        verify(userPreferenceRepository).saveAll(anyList());
    }
}
