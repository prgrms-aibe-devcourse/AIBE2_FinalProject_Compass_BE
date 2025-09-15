package com.compass.domain.chat2.service;

import com.compass.domain.chat2.model.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 사용자 의도 분류 서비스
 * REQ-CHAT2-004: Intent 분류 시스템 (여행/일반 구분)
 */
@Service
@Slf4j
public class IntentClassificationService {
    
    // 여행 계획 관련 키워드 패턴
    private static final List<Pattern> TRAVEL_PLANNING_PATTERNS = Arrays.asList(
        Pattern.compile(".*여행.*계획.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*여행.*일정.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*여행.*추천.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*여행.*코스.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*여행.*루트.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*여행.*스케줄.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 정보 수집 관련 키워드 패턴
    private static final List<Pattern> INFORMATION_COLLECTION_PATTERNS = Arrays.asList(
        Pattern.compile(".*언제.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*어디.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*몇.*명.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*예산.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*스타일.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*선호.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 이미지 업로드 관련 키워드 패턴
    private static final List<Pattern> IMAGE_UPLOAD_PATTERNS = Arrays.asList(
        Pattern.compile(".*이미지.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*사진.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*업로드.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*OCR.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*예약서.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*항공권.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*호텔.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 일반 질문 관련 키워드 패턴
    private static final List<Pattern> GENERAL_QUESTION_PATTERNS = Arrays.asList(
        Pattern.compile(".*날씨.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*환율.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*뉴스.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*안녕.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*도움.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*서비스.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 빠른 입력 관련 키워드 패턴
    private static final List<Pattern> QUICK_INPUT_PATTERNS = Arrays.asList(
        Pattern.compile(".*빠른.*입력.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*한번에.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*폼.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*템플릿.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 여행지 검색 관련 키워드 패턴
    private static final List<Pattern> DESTINATION_SEARCH_PATTERNS = Arrays.asList(
        Pattern.compile(".*검색.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*찾아.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*어디.*가볼.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*관광지.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*명소.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 예약 처리 관련 키워드 패턴
    private static final List<Pattern> RESERVATION_PROCESSING_PATTERNS = Arrays.asList(
        Pattern.compile(".*예약.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*티켓.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*바우처.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*체크인.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*체크아웃.*", Pattern.CASE_INSENSITIVE)
    );
    
    // API 사용량 확인 관련 키워드 패턴
    private static final List<Pattern> API_USAGE_CHECK_PATTERNS = Arrays.asList(
        Pattern.compile(".*사용량.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*제한.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*횟수.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*API.*", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * 사용자 입력을 분석하여 Intent를 분류합니다.
     * 
     * @param userInput 사용자 입력 텍스트
     * @return 분류된 Intent
     */
    public Intent classifyIntent(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            log.warn("빈 입력으로 인해 UNKNOWN Intent 반환");
            return Intent.UNKNOWN;
        }
        
        log.debug("Intent 분류 시작: {}", userInput);
        
        // 우선순위에 따라 Intent 분류
        if (matchesPatterns(userInput, TRAVEL_PLANNING_PATTERNS)) {
            log.debug("TRAVEL_PLANNING Intent로 분류됨");
            return Intent.TRAVEL_PLANNING;
        }
        
        if (matchesPatterns(userInput, IMAGE_UPLOAD_PATTERNS)) {
            log.debug("IMAGE_UPLOAD Intent로 분류됨");
            return Intent.IMAGE_UPLOAD;
        }
        
        if (matchesPatterns(userInput, INFORMATION_COLLECTION_PATTERNS)) {
            log.debug("INFORMATION_COLLECTION Intent로 분류됨");
            return Intent.INFORMATION_COLLECTION;
        }
        
        if (matchesPatterns(userInput, DESTINATION_SEARCH_PATTERNS)) {
            log.debug("DESTINATION_SEARCH Intent로 분류됨");
            return Intent.DESTINATION_SEARCH;
        }
        
        if (matchesPatterns(userInput, RESERVATION_PROCESSING_PATTERNS)) {
            log.debug("RESERVATION_PROCESSING Intent로 분류됨");
            return Intent.RESERVATION_PROCESSING;
        }
        
        if (matchesPatterns(userInput, QUICK_INPUT_PATTERNS)) {
            log.debug("QUICK_INPUT Intent로 분류됨");
            return Intent.QUICK_INPUT;
        }
        
        if (matchesPatterns(userInput, API_USAGE_CHECK_PATTERNS)) {
            log.debug("API_USAGE_CHECK Intent로 분류됨");
            return Intent.API_USAGE_CHECK;
        }
        
        if (matchesPatterns(userInput, GENERAL_QUESTION_PATTERNS)) {
            log.debug("GENERAL_QUESTION Intent로 분류됨");
            return Intent.GENERAL_QUESTION;
        }
        
        log.debug("UNKNOWN Intent로 분류됨");
        return Intent.UNKNOWN;
    }
    
    /**
     * 입력 텍스트가 주어진 패턴들과 매치되는지 확인합니다.
     * 
     * @param input 입력 텍스트
     * @param patterns 패턴 리스트
     * @return 매치 여부
     */
    private boolean matchesPatterns(String input, List<Pattern> patterns) {
        return patterns.stream()
                .anyMatch(pattern -> pattern.matcher(input).matches());
    }
    
    /**
     * Intent 분류 정확도를 테스트합니다.
     * 
     * @param testCases 테스트 케이스 (입력 -> 예상 Intent)
     * @return 정확도 (0.0 ~ 1.0)
     */
    public double testAccuracy(java.util.Map<String, Intent> testCases) {
        if (testCases.isEmpty()) {
            return 0.0;
        }
        
        long correctCount = testCases.entrySet().stream()
                .mapToLong(entry -> {
                    Intent predicted = classifyIntent(entry.getKey());
                    return predicted == entry.getValue() ? 1 : 0;
                })
                .sum();
        
        return (double) correctCount / testCases.size();
    }
}
