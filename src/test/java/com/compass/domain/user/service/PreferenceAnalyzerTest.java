package com.compass.domain.user.service;

import com.compass.domain.trip.entity.TravelHistory;
import com.compass.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferenceAnalyzerTest {

    private PreferenceAnalyzer preferenceAnalyzer;

    @Mock
    @Qualifier("geminiChatClient")
    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        // RETURNS_DEEP_STUBS: 복잡한 메서드 체인을 모킹할 때 중간 과정을 생략하고 최종 결과만 정의할 수 있게 해줍니다.
        chatClient = mock(ChatClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        preferenceAnalyzer = new PreferenceAnalyzer(chatClient, new ObjectMapper());
    }

    @Test
    @DisplayName("AI 기반 선호도 분석 성공")
    void analyzeWithAi_success() {
        // given
        List<TravelHistory> histories = List.of(
                TravelHistory.builder().travelStyle("ACTIVITY").build(),
                TravelHistory.builder().travelStyle("ACTIVITY").build(),
                TravelHistory.builder().travelStyle("RELAXATION").build()
        );

        // AI가 반환할 응답을 미리 설정
        String aiResponse = "  activity_seeker  ";

        // when: 복잡한 중간 과정 없이, 최종 호출 결과만 모킹합니다.
        when(chatClient.prompt()
                .system(any(String.class))
                .user(any(String.class))
                .call()
                .content()).thenReturn(aiResponse);

        // when
        String result = preferenceAnalyzer.analyzeTravelStyleWithAi(histories);

        // then
        // AI의 지저분한 응답이 깔끔하게 정제되었는지 확인
        assertThat(result).isEqualTo("ACTIVITY_SEEKER");
    }

    @Test
    @DisplayName("AI 분석 실패 시 규칙 기반으로 자동 전환 (Fallback)")
    void analyzeWithAi_fallback_on_ai_error() {
        // given
        List<TravelHistory> histories = List.of(
                TravelHistory.builder().travelStyle("ACTIVITY").build(),
                TravelHistory.builder().travelStyle("ACTIVITY").build(),
                TravelHistory.builder().travelStyle("RELAXATION").build()
        );

        // when: AI 호출의 최종 단계에서 예외가 발생하도록 설정합니다.
        when(chatClient.prompt()
                .system(any(String.class))
                .user(any(String.class))
                .call()
                .content()).thenThrow(new RuntimeException("AI server is down"));

        // when
        String result = preferenceAnalyzer.analyzeTravelStyleWithAi(histories);

        // then
        // AI가 실패했으므로, 규칙 기반 분석 결과(가장 빈번한 'ACTIVITY')가 반환되었는지 확인
        assertThat(result).isEqualTo("ACTIVITY");
    }

    @Test
    @DisplayName("여행 기록이 없을 때 NEW_TRAVELER 반환")
    void analyzeWithAi_emptyHistory() {
        // given
        List<TravelHistory> emptyHistories = Collections.emptyList();

        // when
        String result = preferenceAnalyzer.analyzeTravelStyleWithAi(emptyHistories);

        // then
        assertThat(result).isEqualTo("NEW_TRAVELER");
        // AI 호출이 전혀 발생하지 않았는지 확인
        verify(chatClient, never()).prompt();
    }


}