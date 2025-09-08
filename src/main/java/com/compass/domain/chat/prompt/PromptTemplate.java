package com.compass.domain.chat.prompt;

import java.util.Map;

/**
 * Base interface for all prompt templates
 */
public interface PromptTemplate {
    
    /**
     * Get the template name
     */
    String getName();
    
    /**
     * Get the template description
     */
    String getDescription();
    
    /**
     * Get the raw template string with placeholders
     */
    String getTemplate();
    
    /**
     * Build the prompt with the given parameters
     * @param parameters Map of parameter names to values
     * @return The formatted prompt string
     */
    String buildPrompt(Map<String, Object> parameters);
    
    /**
     * Validate if all required parameters are present
     * @param parameters Map of parameter names to values
     * @return true if all required parameters are present
     */
    boolean validateParameters(Map<String, Object> parameters);
    
    /**
     * Get required parameter names
     */
    String[] getRequiredParameters();
    
    /**
     * Get optional parameter names
     */
    String[] getOptionalParameters();
    
    /**
     * Check if this template supports the given user input
     * @param userInput The user's input message
     * @return true if this template can handle the input
     */
    boolean supports(String userInput);
}