package com.compass.domain.chat.prompt.travel;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;

/**
 * Prompt template for local experiences and cultural recommendations
 */
public class LocalExperiencePrompt extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        You are a local culture and experience expert providing authentic travel recommendations.
        
        Destination: {{destination}}
        Duration: {{duration}}
        Traveler Profile: {{travelerProfile}}
        
        Interest Areas:
        - Cultural Experiences: {{culturalInterests}}
        - Food & Dining: {{foodPreferences}}
        - Activities: {{activityPreferences}}
        {{specialInterests}}
        
        Please provide authentic local experiences including:
        
        1. Hidden Gems & Local Favorites
           - Places locals love but tourists often miss
           - Best times to visit to avoid crowds
           - How to get there using local transport
        
        2. Cultural Experiences
           - Local festivals or events during the visit period
           - Traditional customs and etiquette tips
           - Meaningful cultural activities to participate in
        
        3. Authentic Food Experiences
           - Must-try local dishes and where to find them
           - Local markets and food streets
           - Restaurant recommendations off the beaten path
           - Food tours or cooking classes
        
        4. Local Tips & Insights
           - Money-saving tips known to locals
           - Common tourist mistakes to avoid
           - Useful local apps or resources
           - Basic local phrases that will be appreciated
        
        5. Neighborhood Guide
           - Best neighborhoods to explore
           - Where to stay for an authentic experience
           - Local shopping areas and crafts
        
        Focus on authentic, immersive experiences that connect travelers with local culture.
        Avoid overly touristy recommendations unless specifically requested.
        """;
    
    public LocalExperiencePrompt() {
        super(
            "local_experience",
            "Local experience prompt for authentic cultural recommendations",
            TEMPLATE,
            new String[] {
                "destination",
                "duration",
                "travelerProfile",
                "culturalInterests",
                "foodPreferences",
                "activityPreferences"
            },
            new String[] {"specialInterests"}
        );
    }
}