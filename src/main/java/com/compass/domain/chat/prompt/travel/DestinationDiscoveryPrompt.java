package com.compass.domain.chat.prompt.travel;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;

/**
 * Prompt template for discovering travel destinations based on preferences
 */
public class DestinationDiscoveryPrompt extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        You are a travel destination discovery expert helping users find their perfect travel destination.
        
        User Preferences:
        - Travel Style: {{travelStyle}}
        - Interests: {{interests}}
        - Budget Range: {{budgetRange}}
        - Travel Season: {{travelSeason}}
        - Duration: {{duration}}
        
        Constraints:
        - Departure Location: {{departureLocation}}
        - Visa Requirements: {{visaRequirements}}
        - Language Preferences: {{languagePreferences}}
        {{additionalConstraints}}
        
        Previous Travel Experience:
        {{travelHistory}}
        
        Based on these preferences, suggest 3-5 ideal destinations with:
        1. Destination name and country
        2. Why it matches the user's preferences
        3. Best time to visit
        4. Estimated budget per person
        5. Main attractions and activities
        6. Travel logistics (flight time, visa requirements)
        7. Unique selling points of each destination
        8. Potential drawbacks to consider
        
        Rank the destinations from most to least recommended.
        Provide diverse options that cater to different aspects of their preferences.
        """;
    
    public DestinationDiscoveryPrompt() {
        super(
            "destination_discovery",
            "Destination discovery prompt for finding ideal travel destinations",
            TEMPLATE,
            new String[] {
                "travelStyle",
                "interests",
                "budgetRange",
                "travelSeason",
                "duration",
                "departureLocation"
            },
            new String[] {
                "visaRequirements",
                "languagePreferences",
                "additionalConstraints",
                "travelHistory"
            }
        );
    }
}