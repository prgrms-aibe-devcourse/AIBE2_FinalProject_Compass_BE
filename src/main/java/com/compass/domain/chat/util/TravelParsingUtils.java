package com.compass.domain.chat.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 여행 정보 파싱을 위한 공통 유틸리티 클래스
 * 중복된 파싱 로직을 한 곳에서 관리
 */
@Slf4j
@UtilityClass
public class TravelParsingUtils {
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*~\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern MONEY_PATTERN = Pattern.compile("(\\d+)(만원|천원|원|,000|000)");
    
    /**
     * 여행 기간(박) 파싱
     */
    public static int parseDurationNights(String text) {
        if (text == null || text.isEmpty()) {
            return 2; // 기본값
        }
        
        String lowerText = text.toLowerCase();
        
        // 당일치기 체크
        if (lowerText.contains("당일") || lowerText.contains("day trip")) {
            return 0;
        }
        
        // N박 패턴 체크
        if (lowerText.contains("1박")) return 1;
        if (lowerText.contains("2박")) return 2;
        if (lowerText.contains("3박")) return 3;
        if (lowerText.contains("4박")) return 4;
        if (lowerText.contains("5박")) return 5;
        
        // 숫자 추출
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse duration from: {}", text);
            }
        }
        
        return 2; // 기본값
    }
    
    /**
     * 예산 레벨 파싱
     */
    public static String parseBudgetLevel(String text) {
        if (text == null || text.isEmpty()) {
            return "moderate";
        }
        
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("저렴") || lowerText.contains("budget") || 
            lowerText.contains("cheap") || lowerText.contains("절약")) {
            return "budget";
        }
        
        if (lowerText.contains("럭셔리") || lowerText.contains("luxury") || 
            lowerText.contains("premium") || lowerText.contains("고급")) {
            return "luxury";
        }
        
        // 기본값은 moderate
        return "moderate";
    }
    
    /**
     * 금액 파싱 (원화 기준)
     */
    public static Integer parseMoneyAmount(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        Matcher matcher = MONEY_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                int amount = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2);
                
                if (unit.contains("만원")) {
                    amount *= 10000;
                } else if (unit.contains("천원") || unit.contains(",000")) {
                    amount *= 1000;
                }
                
                return amount;
            } catch (NumberFormatException e) {
                log.warn("Failed to parse money amount from: {}", text);
            }
        }
        
        return null;
    }
    
    /**
     * 날짜 범위 파싱
     */
    public static DateRange parseDateRange(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        Matcher matcher = DATE_RANGE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                LocalDate startDate = LocalDate.parse(matcher.group(1));
                LocalDate endDate = LocalDate.parse(matcher.group(2));
                return new DateRange(startDate, endDate);
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse date range from: {}", text);
            }
        }
        
        return null;
    }
    
    /**
     * 동행자 타입 파싱 (parseGroupType 별칭)
     */
    public static String parseGroupType(String text) {
        return parseCompanionType(text);
    }
    
    /**
     * 동행자 타입 파싱
     */
    public static String parseCompanionType(String text) {
        if (text == null || text.isEmpty()) {
            return "solo";
        }
        
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("혼자") || lowerText.contains("alone") || 
            lowerText.contains("solo") || lowerText.contains("개인")) {
            return "solo";
        }
        
        if (lowerText.contains("연인") || lowerText.contains("couple") || 
            lowerText.contains("커플") || lowerText.contains("둘이")) {
            return "couple";
        }
        
        if (lowerText.contains("가족") || lowerText.contains("family") || 
            lowerText.contains("부모") || lowerText.contains("자녀")) {
            return "family";
        }
        
        if (lowerText.contains("친구") || lowerText.contains("friend") || 
            lowerText.contains("동료")) {
            return "friends";
        }
        
        if (lowerText.contains("비즈니스") || lowerText.contains("business") || 
            lowerText.contains("출장") || lowerText.contains("업무")) {
            return "business";
        }
        
        return "solo"; // 기본값
    }
    
    /**
     * 여행자 수 파싱
     */
    public static int parseTravelerCount(String text, String companionType) {
        if (text == null || text.isEmpty()) {
            return getDefaultTravelerCount(companionType);
        }
        
        // 숫자 추출
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse traveler count from: {}", text);
            }
        }
        
        return getDefaultTravelerCount(companionType);
    }
    
    /**
     * 동행자 타입별 기본 인원수
     */
    private static int getDefaultTravelerCount(String companionType) {
        return switch (companionType) {
            case "solo", "business" -> 1;
            case "couple" -> 2;
            case "family" -> 4;
            case "friends" -> 3;
            default -> 1;
        };
    }
    
    /**
     * 목적지 파싱 (한국 도시)
     */
    public static String parseDestination(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        String lowerText = text.toLowerCase();
        
        // 주요 도시 매핑
        if (lowerText.contains("서울") || lowerText.contains("seoul")) return "서울";
        if (lowerText.contains("부산") || lowerText.contains("busan")) return "부산";
        if (lowerText.contains("제주") || lowerText.contains("jeju")) return "제주";
        if (lowerText.contains("경주") || lowerText.contains("gyeongju")) return "경주";
        if (lowerText.contains("전주") || lowerText.contains("jeonju")) return "전주";
        if (lowerText.contains("강릉") || lowerText.contains("gangneung")) return "강릉";
        if (lowerText.contains("인천") || lowerText.contains("incheon")) return "인천";
        if (lowerText.contains("대구") || lowerText.contains("daegu")) return "대구";
        if (lowerText.contains("대전") || lowerText.contains("daejeon")) return "대전";
        if (lowerText.contains("광주") || lowerText.contains("gwangju")) return "광주";
        if (lowerText.contains("울산") || lowerText.contains("ulsan")) return "울산";
        
        // 원본 텍스트 반환 (파싱 실패 시)
        return text.trim();
    }
    
    /**
     * 날짜 범위를 나타내는 내부 클래스
     */
    public static record DateRange(LocalDate startDate, LocalDate endDate) {
        public int getNights() {
            return (int) (endDate.toEpochDay() - startDate.toEpochDay());
        }
        
        public int getDays() {
            return getNights() + 1;
        }
    }
}