package com.compass.domain.chat.prompt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AbstractPromptTemplate 테스트")
class AbstractPromptTemplateTest {
    
    private static class TestPromptTemplate extends AbstractPromptTemplate {
        public TestPromptTemplate() {
            super(
                "test_template",
                "Test template for unit testing",
                "Hello {{name}}, you are {{age}} years old. {{optionalMessage}}",
                new String[] {"name", "age"},
                new String[] {"optionalMessage"}
            );
        }
    }
    
    private final TestPromptTemplate template = new TestPromptTemplate();
    
    @Test
    @DisplayName("템플릿 기본 정보 확인")
    void testTemplateBasicInfo() {
        assertThat(template.getName()).isEqualTo("test_template");
        assertThat(template.getDescription()).isEqualTo("Test template for unit testing");
        assertThat(template.getTemplate()).contains("{{name}}", "{{age}}", "{{optionalMessage}}");
    }
    
    @Test
    @DisplayName("필수 파라미터와 선택 파라미터 확인")
    void testParameters() {
        assertThat(template.getRequiredParameters()).containsExactly("name", "age");
        assertThat(template.getOptionalParameters()).containsExactly("optionalMessage");
    }
    
    @Test
    @DisplayName("모든 파라미터가 있을 때 프롬프트 생성")
    void testBuildPromptWithAllParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "John");
        params.put("age", 25);
        params.put("optionalMessage", "Welcome to our service!");
        
        String result = template.buildPrompt(params);
        
        assertThat(result).isEqualTo("Hello John, you are 25 years old. Welcome to our service!");
    }
    
    @Test
    @DisplayName("선택 파라미터 없이 프롬프트 생성")
    void testBuildPromptWithoutOptionalParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Jane");
        params.put("age", 30);
        
        String result = template.buildPrompt(params);
        
        assertThat(result).isEqualTo("Hello Jane, you are 30 years old.");
    }
    
    @Test
    @DisplayName("필수 파라미터 누락 시 예외 발생")
    void testBuildPromptMissingRequiredParameter() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Bob");
        // age is missing
        
        assertThatThrownBy(() -> template.buildPrompt(params))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Missing required parameters");
    }
    
    @Test
    @DisplayName("파라미터 검증 - 모든 필수 파라미터 있음")
    void testValidateParametersAllPresent() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Alice");
        params.put("age", 28);
        
        assertThat(template.validateParameters(params)).isTrue();
    }
    
    @Test
    @DisplayName("파라미터 검증 - 필수 파라미터 누락")
    void testValidateParametersMissingRequired() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Charlie");
        
        assertThat(template.validateParameters(params)).isFalse();
    }
    
    @Test
    @DisplayName("파라미터 검증 - null 파라미터")
    void testValidateParametersNull() {
        assertThat(template.validateParameters(null)).isFalse();
    }
    
    @Test
    @DisplayName("특수 문자가 포함된 파라미터 처리")
    void testBuildPromptWithSpecialCharacters() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "John $mith");
        params.put("age", 25);
        params.put("optionalMessage", "Price: $100");
        
        String result = template.buildPrompt(params);
        
        assertThat(result).isEqualTo("Hello John $mith, you are 25 years old. Price: $100");
    }
}