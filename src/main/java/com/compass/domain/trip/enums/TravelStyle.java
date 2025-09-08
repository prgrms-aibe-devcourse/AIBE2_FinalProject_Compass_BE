package com.compass.domain.trip.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 여행 스타일 ENUM
 * 사용자의 여행 선호도를 나타내는 열거형
 */
@Getter
@RequiredArgsConstructor
public enum TravelStyle {
    
    RELAXATION("휴양", "휴식과 힐링을 중심으로 한 여행", "스파, 온천, 해변 리조트, 요가 리트리트"),
    SIGHTSEEING("관광", "관광지 방문 및 문화 체험 중심 여행", "박물관, 궁궐, 사찰, 랜드마크, 전통 마을"),
    ACTIVITY("액티비티", "체험과 활동 중심의 적극적인 여행", "테마파크, 등산로, 체험관, 스포츠 시설");
    
    private final String displayName;
    private final String description;
    private final String recommendedPlaces;
    
    /**
     * 문자열로부터 TravelStyle을 찾는 메서드
     * 
     * @param value 문자열 값
     * @return TravelStyle 또는 null
     */
    public static TravelStyle fromString(String value) {
        if (value == null) {
            return null;
        }
        
        for (TravelStyle style : TravelStyle.values()) {
            if (style.name().equalsIgnoreCase(value) || 
                style.displayName.equals(value)) {
                return style;
            }
        }
        
        throw new IllegalArgumentException("Unknown travel style: " + value);
    }
    
    /**
     * 유효한 여행 스타일인지 확인
     * 
     * @param value 확인할 값
     * @return 유효 여부
     */
    public static boolean isValid(String value) {
        try {
            fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
