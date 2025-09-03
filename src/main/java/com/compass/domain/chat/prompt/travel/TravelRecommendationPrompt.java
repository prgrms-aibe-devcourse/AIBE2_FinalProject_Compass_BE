package com.compass.domain.chat.prompt.travel;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;

/**
 * Prompt template for travel recommendations based on user query
 */
public class TravelRecommendationPrompt extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        You are an expert travel advisor providing personalized recommendations.
        
        User Query: {{userQuery}}
        
        Context Information:
        - User Location: {{userLocation}}
        - Previous Trips: {{previousTrips}}
        - Interests: {{interests}}
        - Budget Level: {{budgetLevel}}
        {{additionalContext}}
        
        Based on the user's query and context, provide:
        1. Direct answer to their specific question
        2. Related recommendations that might be helpful
        3. Practical tips and insider knowledge
        4. Alternative options if applicable
        5. Important considerations or warnings
        
        Keep your response conversational, helpful, and tailored to the user's needs.
        Include specific details like names, addresses, and approximate costs where relevant.
        """;
    
    public TravelRecommendationPrompt() {
        super(
            "travel_recommendation",
            "Travel recommendation prompt for answering specific travel queries",
            TEMPLATE,
            new String[] {
                "userQuery",
                "userLocation",
                "interests",
                "budgetLevel"
            },
            new String[] {
                "previousTrips",
                "additionalContext"
            }
        );
    }
}