package com.compass.domain.chat.prompt.travel;

import com.compass.domain.chat.prompt.AbstractPromptTemplate;

/**
 * TripDetail 엔티티를 위한 일별 상세 일정 생성 프롬프트
 */
public class DailyItineraryPrompt extends AbstractPromptTemplate {
    
    private static final String TEMPLATE = """
        You are a travel itinerary specialist. Create a detailed daily schedule in JSON format.
        
        Trip Context:
        - Destination: {{destination}}
        - Day Number: {{dayNumber}}
        - Date: {{activityDate}}
        - Total People: {{numberOfPeople}}
        - Daily Budget: {{dailyBudget}}
        - Previous Day Summary: {{previousDaySummary}}
        
        User Preferences:
        {{preferences}}
        
        Generate a detailed daily itinerary following this JSON structure:
        {
          "dayNumber": {{dayNumber}},
          "activityDate": "{{activityDate}}",
          "theme": "Day's main theme or focus",
          "activities": [
            {
              "activityTime": "09:00",
              "placeName": "Specific place name",
              "category": "SIGHTSEEING|DINING|TRANSPORT|SHOPPING|ACTIVITY|ENTERTAINMENT",
              "description": "Detailed activity description including what to do and see",
              "estimatedCost": 0,
              "address": "Complete street address",
              "latitude": 37.5665,
              "longitude": 126.9780,
              "tips": "Insider tips, best photo spots, avoid crowds tips",
              "additionalInfo": {
                "duration": "1-2 hours",
                "bookingRequired": false,
                "bookingUrl": "https://...",
                "bestTimeToVisit": "Morning",
                "difficulty": "Easy|Moderate|Challenging",
                "accessibility": "Wheelchair accessible",
                "nearbyAttractions": ["Attraction 1", "Attraction 2"]
              },
              "displayOrder": 1
            }
          ],
          "meals": [
            {
              "mealType": "BREAKFAST|LUNCH|DINNER|SNACK",
              "time": "12:30",
              "restaurant": "Restaurant name",
              "cuisine": "Korean|Japanese|Western|etc",
              "estimatedCost": 15000,
              "address": "Restaurant address",
              "recommendedDishes": ["Dish 1", "Dish 2"],
              "reservationRequired": false,
              "dietaryOptions": ["Vegetarian", "Halal"]
            }
          ],
          "transportation": [
            {
              "fromPlace": "Hotel",
              "toPlace": "First attraction",
              "departureTime": "08:30",
              "transportMode": "SUBWAY|BUS|TAXI|WALK|TRAIN",
              "duration": "30 minutes",
              "cost": 1350,
              "instructions": "Take Line 2 to Gangnam Station, Exit 3",
              "alternativeOptions": ["Taxi: 15 min, ₩8,000"]
            }
          ],
          "dayEndSummary": {
            "totalWalkingDistance": "5.2 km",
            "totalTransitTime": "1.5 hours",
            "totalActivityTime": "8 hours",
            "totalEstimatedCost": 85000,
            "physicalIntensity": "Moderate",
            "highlights": ["Morning temple visit", "Traditional market lunch"],
            "endLocation": "Hotel or accommodation"
          },
          "alternativeOptions": [
            {
              "condition": "If raining",
              "suggestion": "Visit indoor museums instead",
              "affectedActivities": [1, 2]
            }
          ],
          "localEvents": [
            {
              "eventName": "Cherry Blossom Festival",
              "time": "All day",
              "location": "Yeouido Park",
              "free": true
            }
          ]
        }
        
        Important guidelines:
        - Schedule activities with realistic time gaps for transportation
        - Consider opening hours and peak times
        - Balance activity intensity throughout the day
        - Include rest breaks and flexibility
        - All costs in KRW (Korean Won)
        - Provide specific, actionable information
        - Consider weather and season
        """;

    public DailyItineraryPrompt() {
        super(
            "daily_itinerary",
            "Detailed daily itinerary generation for TripDetail entities",
            TEMPLATE,
            new String[] {
                "destination",
                "dayNumber",
                "activityDate",
                "numberOfPeople",
                "dailyBudget",
                "preferences"
            },
            new String[] {
                "previousDaySummary"
            }
        );
    }
}