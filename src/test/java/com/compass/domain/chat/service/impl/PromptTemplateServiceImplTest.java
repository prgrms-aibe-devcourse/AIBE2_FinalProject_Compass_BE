package com.compass.domain.chat.service.impl;

import com.compass.domain.chat.dto.PromptRequest;
import com.compass.domain.chat.dto.PromptResponse;
import com.compass.domain.chat.prompt.PromptTemplateRegistry;
import com.compass.domain.chat.prompt.PromptTemplate;
import com.compass.domain.chat.prompt.travel.TravelPlanningPrompt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromptTemplateServiceImpl 테스트")
class PromptTemplateServiceImplTest {
    
    @Mock
    private PromptTemplateRegistry templateRegistry;
    
    @InjectMocks
    private PromptTemplateServiceImpl service;
    
    private Map<String, Object> parameters;
    
    @BeforeEach
    void setUp() {
        parameters = new HashMap<>();
    }
    
    @Test
    @DisplayName("프롬프트 빌드 성공")
    void testBuildPromptSuccess() {
        String templateName = "travel_planning";
        String expectedPrompt = "Generated travel planning prompt";
        
        when(templateRegistry.buildPrompt(templateName, parameters))
            .thenReturn(expectedPrompt);
        
        String result = service.buildPrompt(templateName, parameters);
        
        assertThat(result).isEqualTo(expectedPrompt);
        verify(templateRegistry).buildPrompt(templateName, parameters);
    }
    
    @Test
    @DisplayName("풍부한 컨텍스트와 함께 프롬프트 빌드")
    void testBuildEnrichedPrompt() {
        PromptRequest request = PromptRequest.builder()
            .templateName("travel_recommendation")
            .parameters(parameters)
            .sessionId("session-123")
            .userId("user-456")
            .build();
        
        String expectedPrompt = "Enriched prompt content";
        when(templateRegistry.buildPrompt(eq("travel_recommendation"), any()))
            .thenReturn(expectedPrompt);
        
        PromptResponse response = service.buildEnrichedPrompt(request);
        
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getPrompt()).isEqualTo(expectedPrompt);
        assertThat(response.getTemplateName()).isEqualTo("travel_recommendation");
        assertThat(response.getParameters()).containsKey("timestamp");
        assertThat(response.getParameters()).containsEntry("sessionId", "session-123");
    }
    
    @Test
    @DisplayName("프롬프트 빌드 실패 시 에러 응답")
    void testBuildEnrichedPromptError() {
        PromptRequest request = PromptRequest.builder()
            .templateName("invalid_template")
            .parameters(parameters)
            .build();
        
        when(templateRegistry.buildPrompt(any(), any()))
            .thenThrow(new IllegalArgumentException("Template not found"));
        
        PromptResponse response = service.buildEnrichedPrompt(request);
        
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorMessage()).contains("Template not found");
    }
    
    @Test
    @DisplayName("사용 가능한 템플릿 목록 조회")
    void testGetAvailableTemplates() {
        Set<String> expectedTemplates = Set.of(
            "travel_planning",
            "travel_recommendation",
            "destination_discovery"
        );
        
        when(templateRegistry.getTemplateNames()).thenReturn(expectedTemplates);
        
        Set<String> result = service.getAvailableTemplates();
        
        assertThat(result).isEqualTo(expectedTemplates);
        verify(templateRegistry).getTemplateNames();
    }
    
    @Test
    @DisplayName("템플릿 상세 정보 조회")
    void testGetTemplateDetails() {
        String templateName = "travel_planning";
        PromptTemplate mockTemplate = mock(PromptTemplate.class);
        
        when(mockTemplate.getName()).thenReturn(templateName);
        when(mockTemplate.getDescription()).thenReturn("Test description");
        when(mockTemplate.getRequiredParameters()).thenReturn(new String[]{"param1", "param2"});
        when(mockTemplate.getOptionalParameters()).thenReturn(new String[]{"optional1"});
        when(mockTemplate.getTemplate()).thenReturn("Template content");
        
        when(templateRegistry.getTemplate(templateName)).thenReturn(Optional.of(mockTemplate));
        
        Map<String, Object> details = service.getTemplateDetails(templateName);
        
        assertThat(details).containsEntry("name", templateName);
        assertThat(details).containsEntry("description", "Test description");
        assertThat(details).containsKey("requiredParameters");
        assertThat(details).containsKey("optionalParameters");
        assertThat(details).containsEntry("template", "Template content");
    }
    
    @Test
    @DisplayName("존재하지 않는 템플릿 상세 정보 조회 시 예외")
    void testGetTemplateDetailsNotFound() {
        String templateName = "non_existent";
        
        when(templateRegistry.getTemplate(templateName)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> service.getTemplateDetails(templateName))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Template not found");
    }
    
    @Test
    @DisplayName("여행 계획 쿼리에 대한 템플릿 선택")
    void testSelectTemplateForTravelPlanning() {
        String query = "I want to plan a trip to Japan for 7 days";
        
        String selectedTemplate = service.selectTemplate(query, null);
        
        assertThat(selectedTemplate).isEqualTo("travel_planning");
    }
    
    @Test
    @DisplayName("추천 쿼리에 대한 템플릿 선택")
    void testSelectTemplateForRecommendation() {
        String query = "What restaurants do you recommend in Tokyo?";
        
        String selectedTemplate = service.selectTemplate(query, null);
        
        assertThat(selectedTemplate).isEqualTo("travel_recommendation");
    }
    
    @Test
    @DisplayName("목적지 탐색 쿼리에 대한 템플릿 선택")
    void testSelectTemplateForDestinationDiscovery() {
        String query = "Where should I go for a beach vacation?";
        
        String selectedTemplate = service.selectTemplate(query, null);
        
        assertThat(selectedTemplate).isEqualTo("destination_discovery");
    }
    
    @Test
    @DisplayName("예산 관련 쿼리에 대한 템플릿 선택")
    void testSelectTemplateForBudget() {
        String query = "How can I save money on my trip to Europe?";
        
        String selectedTemplate = service.selectTemplate(query, null);
        
        assertThat(selectedTemplate).isEqualTo("budget_optimization");
    }
    
    @Test
    @DisplayName("로컬 경험 쿼리에 대한 템플릿 선택")
    void testSelectTemplateForLocalExperience() {
        String query = "I want to experience local culture and hidden gems in Seoul";
        
        String selectedTemplate = service.selectTemplate(query, null);
        
        assertThat(selectedTemplate).isEqualTo("local_experience");
    }
    
    @Test
    @DisplayName("컨텍스트 힌트를 활용한 템플릿 선택")
    void testSelectTemplateWithContext() {
        String query = "Tell me about Paris";
        Map<String, Object> context = Map.of(
            "needsItinerary", true
        );
        
        String selectedTemplate = service.selectTemplate(query, context);
        
        assertThat(selectedTemplate).isEqualTo("travel_planning");
    }
    
    @Test
    @DisplayName("사용자 입력에서 파라미터 추출")
    void testExtractParameters() {
        String userInput = "I want to visit Tokyo for 5 days with a budget of $2000";
        
        Map<String, Object> extracted = service.extractParameters(
            "travel_planning", userInput, null
        );
        
        assertThat(extracted)
            .containsEntry("userQuery", userInput)
            .containsEntry("destination", "Tokyo")
            .containsEntry("duration", "5 days")
            .containsKey("budgetRange");
        
        assertThat(extracted.get("budgetRange").toString()).contains("2000");
    }
    
    @Test
    @DisplayName("여행자 수 추출")
    void testExtractTravelers() {
        String userInput = "Planning a trip for 4 people to Rome";
        
        Map<String, Object> extracted = service.extractParameters(
            "travel_planning", userInput, null
        );
        
        assertThat(extracted).containsEntry("numberOfPeople", 4);
    }
    
    @Test
    @DisplayName("솔로 여행 감지")
    void testExtractSoloTravel() {
        String userInput = "I want to travel solo to Barcelona";
        
        Map<String, Object> extracted = service.extractParameters(
            "travel_planning", userInput, null
        );
        
        assertThat(extracted).containsEntry("numberOfPeople", 1);
    }
    
    @Test
    @DisplayName("커플 여행 감지")
    void testExtractCoupleTravel() {
        String userInput = "Planning a romantic trip for a couple to Paris";
        
        Map<String, Object> extracted = service.extractParameters(
            "travel_planning", userInput, null
        );
        
        assertThat(extracted).containsEntry("numberOfPeople", 2);
    }
    
    @Test
    @DisplayName("기본 파라미터 추가")
    void testAddDefaultParameters() {
        String userInput = "Tell me about travel options";
        
        Map<String, Object> extracted = service.extractParameters(
            "travel_planning", userInput, null
        );
        
        // Check that default parameters are added
        assertThat(extracted)
            .containsKeys("destination", "startDate", "endDate", 
                         "numberOfPeople", "tripPurpose", "userPreferences",
                         "travelStyle", "totalBudget");
    }
}