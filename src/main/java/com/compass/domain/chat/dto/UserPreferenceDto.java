package com.compass.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 사용자 선호도 DTO
 * REQ-PERS-007: 신규 사용자 선호도 수집
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {
    
    /**
     * 여행 스타일 (휴양, 관광, 액티비티, 미식 등)
     */
    private List<String> travelStyles;
    
    /**
     * 선호 여행 기간 (당일치기, 1박2일, 3박4일 이상 등)
     */
    private String preferredDuration;
    
    /**
     * 동반자 유형 (혼자, 친구, 가족, 연인 등)
     */
    private String companionType;
    
    /**
     * 예산 범위 (저예산, 중간, 고급)
     */
    private String budgetRange;
    
    /**
     * 관심 테마 (역사/문화, 자연, 쇼핑, 축제 등)
     */
    private List<String> interests;
    
    /**
     * 선호 계절
     */
    private List<String> preferredSeasons;
    
    /**
     * 선호 지역 (국내/해외)
     */
    private String preferredRegion;
    
    /**
     * 특별 요구사항 (알레르기, 접근성, 기타 제약사항)
     */
    private String specialRequirements;
}