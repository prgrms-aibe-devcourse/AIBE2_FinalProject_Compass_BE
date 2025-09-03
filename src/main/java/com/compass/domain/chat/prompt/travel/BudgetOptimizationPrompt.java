package com.compass.domain.chat.prompt.travel;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;

/**
 * Prompt template for budget optimization in travel planning
 */
public class BudgetOptimizationPrompt extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        You are a travel budget optimization expert helping users maximize their travel experience within budget constraints.
        
        Trip Overview:
        - Destination: {{destination}}
        - Duration: {{duration}}
        - Total Budget: {{totalBudget}}
        - Number of Travelers: {{numberOfTravelers}}
        - Travel Dates: {{travelDates}}
        - Priority Areas: {{priorityAreas}}
        
        Current Spending Plan:
        {{currentPlan}}
        
        Traveler Preferences:
        - Must-Have Experiences: {{mustHaveExperiences}}
        - Flexibility Areas: {{flexibilityAreas}}
        - Comfort Level: {{comfortLevel}}
        
        Provide a comprehensive budget optimization plan including:
        
        1. Budget Breakdown
           - Accommodation: suggested allocation and options
           - Transportation: flights, local transport, transfers
           - Food & Dining: daily meal budget and strategies
           - Activities & Attractions: prioritized list with costs
           - Shopping & Souvenirs: reasonable allocation
           - Emergency Fund: recommended buffer amount
        
        2. Money-Saving Strategies
           - Best booking times and platforms
           - Discount cards and passes worth buying
           - Free activities and attractions
           - Budget-friendly dining options without sacrificing quality
           - Transportation hacks and savings
        
        3. Splurge vs Save Recommendations
           - Where to splurge for maximum impact
           - Where to save without compromising experience
           - Alternative options for expensive activities
        
        4. Daily Budget Guide
           - Recommended daily spending limit
           - Sample daily expense breakdown
           - Tips for tracking expenses while traveling
        
        5. Seasonal Considerations
           - Price variations during travel dates
           - Off-peak opportunities nearby
           - Weather-related budget impacts
        
        6. Hidden Costs to Consider
           - Often overlooked expenses
           - Tips to avoid tourist traps
           - Currency exchange recommendations
        
        Ensure all recommendations fit within the total budget while maximizing travel experience value.
        """;
    
    public BudgetOptimizationPrompt() {
        super(
            "budget_optimization",
            "Budget optimization prompt for maximizing travel value within constraints",
            TEMPLATE,
            new String[] {
                "destination",
                "duration",
                "totalBudget",
                "numberOfTravelers",
                "travelDates",
                "priorityAreas",
                "mustHaveExperiences",
                "comfortLevel"
            },
            new String[] {
                "currentPlan",
                "flexibilityAreas"
            }
        );
    }
}