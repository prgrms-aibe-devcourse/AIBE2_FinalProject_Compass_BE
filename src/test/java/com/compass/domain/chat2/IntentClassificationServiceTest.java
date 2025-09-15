package com.compass.domain.chat2;

import com.compass.domain.chat2.service.IntentClassificationService;
import com.compass.domain.chat2.model.Intent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IntentClassificationService 테스트
 * REQ-CHAT2-004: Intent 분류 시스템 테스트
 */
class IntentClassificationServiceTest {

    private IntentClassificationService intentClassificationService;

    @BeforeEach
    void setUp() {
        intentClassificationService = new IntentClassificationService();
    }

    @Test
    @DisplayName("여행 계획 생성 Intent 분류 테스트")
    void testClassifyIntent_TravelPlanning() {
        // Given
        String userInput = "서울로 여행 계획을 만들어줘";

        // When
        Intent result = intentClassificationService.classifyIntent(userInput);

        // Then
        assertEquals(Intent.TRAVEL_PLANNING, result);
    }

    @Test
    @DisplayName("일반 질문 Intent 분류 테스트")
    void testClassifyIntent_GeneralQuestion() {
        // Given
        String userInput = "오늘 날씨 어때?";

        // When
        Intent result = intentClassificationService.classifyIntent(userInput);

        // Then
        assertEquals(Intent.GENERAL_QUESTION, result);
    }

    @Test
    @DisplayName("이미지 업로드 Intent 분류 테스트")
    void testClassifyIntent_ImageUpload() {
        // Given
        String userInput = "항공권 이미지를 업로드했어";

        // When
        Intent result = intentClassificationService.classifyIntent(userInput);

        // Then
        assertEquals(Intent.IMAGE_UPLOAD, result);
    }

    @Test
    @DisplayName("정보 수집 Intent 분류 테스트")
    void testClassifyIntent_InformationCollection() {
        // Given
        String userInput = "언제 출발하시나요?";

        // When
        Intent result = intentClassificationService.classifyIntent(userInput);

        // Then
        assertEquals(Intent.INFORMATION_COLLECTION, result);
    }

    @Test
    @DisplayName("여행지 검색 Intent 분류 테스트")
    void testClassifyIntent_DestinationSearch() {
        // Given
        String userInput = "제주도 관광지를 검색해줘";

        // When
        Intent result = intentClassificationService.classifyIntent(userInput);

        // Then
        assertEquals(Intent.DESTINATION_SEARCH, result);
    }

    @Test
    @DisplayName("빈 입력 처리 테스트")
    void testClassifyIntent_EmptyInput() {
        // Given
        String userInput = "";

        // When
        Intent result = intentClassificationService.classifyIntent(userInput);

        // Then
        assertEquals(Intent.UNKNOWN, result);
    }

    @Test
    @DisplayName("null 입력 처리 테스트")
    void testClassifyIntent_NullInput() {
        // Given
        String userInput = null;

        // When
        Intent result = intentClassificationService.classifyIntent(userInput);

        // Then
        assertEquals(Intent.UNKNOWN, result);
    }

    @Test
    @DisplayName("Intent 분류 정확도 테스트")
    void testAccuracy() {
        // Given
        Map<String, Intent> testCases = new HashMap<>();
        testCases.put("서울로 여행 계획을 만들어줘", Intent.TRAVEL_PLANNING);
        testCases.put("오늘 날씨 어때?", Intent.GENERAL_QUESTION);
        testCases.put("항공권 이미지를 업로드했어", Intent.IMAGE_UPLOAD);
        testCases.put("언제 출발하시나요?", Intent.INFORMATION_COLLECTION);
        testCases.put("제주도 관광지를 검색해줘", Intent.DESTINATION_SEARCH);

        // When
        double accuracy = intentClassificationService.testAccuracy(testCases);

        // Then
        assertEquals(1.0, accuracy, 0.01); // 100% 정확도
    }

    @Test
    @DisplayName("여행 관련 Intent 확인 테스트")
    void testIsTravelRelated() {
        // Given & When & Then
        assertTrue(Intent.TRAVEL_PLANNING.isTravelRelated());
        assertTrue(Intent.INFORMATION_COLLECTION.isTravelRelated());
        assertTrue(Intent.DESTINATION_SEARCH.isTravelRelated());
        assertTrue(Intent.RESERVATION_PROCESSING.isTravelRelated());
        
        assertFalse(Intent.GENERAL_QUESTION.isTravelRelated());
        assertFalse(Intent.IMAGE_UPLOAD.isTravelRelated());
        assertFalse(Intent.QUICK_INPUT.isTravelRelated());
        assertFalse(Intent.API_USAGE_CHECK.isTravelRelated());
        assertFalse(Intent.UNKNOWN.isTravelRelated());
    }

    @Test
    @DisplayName("일반 대화 Intent 확인 테스트")
    void testIsGeneralConversation() {
        // Given & When & Then
        assertTrue(Intent.GENERAL_QUESTION.isGeneralConversation());
        assertTrue(Intent.QUICK_INPUT.isGeneralConversation());
        assertTrue(Intent.API_USAGE_CHECK.isGeneralConversation());
        
        assertFalse(Intent.TRAVEL_PLANNING.isGeneralConversation());
        assertFalse(Intent.INFORMATION_COLLECTION.isGeneralConversation());
        assertFalse(Intent.IMAGE_UPLOAD.isGeneralConversation());
        assertFalse(Intent.DESTINATION_SEARCH.isGeneralConversation());
        assertFalse(Intent.RESERVATION_PROCESSING.isGeneralConversation());
        assertFalse(Intent.UNKNOWN.isGeneralConversation());
    }
}
