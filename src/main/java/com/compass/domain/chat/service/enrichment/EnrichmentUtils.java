package com.compass.domain.chat.service.enrichment;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 보강 서비스 공통 유틸리티
 */
@UtilityClass
@Slf4j
public class EnrichmentUtils {

    private static final Pattern KOREAN_PATTERN = Pattern.compile("[가-힣]+");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    /**
     * API 호출 지연 (Rate Limiting)
     */
    public static void rateLimitDelay(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Rate limit delay interrupted");
        }
    }

    /**
     * 문자열 유사도 계산 (Levenshtein Distance)
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;

        // 특수문자 제거
        s1 = s1.replaceAll("[^가-힣a-zA-Z0-9]", "");
        s2 = s2.replaceAll("[^가-힣a-zA-Z0-9]", "");

        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0;

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        // 초기화
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        // 계산
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // 삭제
                    dp[i][j - 1] + 1),     // 삽입
                    dp[i - 1][j - 1] + cost // 치환
                );
            }
        }

        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - (double) dp[s1.length()][s2.length()] / maxLen;
    }

    /**
     * HTML 태그 제거
     */
    public static String removeHtmlTags(String input) {
        if (input == null) return null;
        return HTML_TAG_PATTERN.matcher(input).replaceAll("").trim();
    }

    /**
     * 문자열 길이 제한
     */
    public static String truncateString(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength - 3) + "...";
    }

    /**
     * 한국어 포함 여부 체크
     */
    public static boolean containsKorean(String input) {
        if (input == null) return false;
        return KOREAN_PATTERN.matcher(input).find();
    }

    /**
     * 카테고리 정제 (카카오맵 형식 -> 간단한 형식)
     * "음식점 > 한식 > 육류,고기" → "한식"
     */
    public static String refineCategory(String kakaoCategory) {
        if (kakaoCategory == null || kakaoCategory.isEmpty()) {
            return null;
        }

        String[] parts = kakaoCategory.split(">");
        if (parts.length >= 2) {
            return parts[1].trim();
        } else if (parts.length == 1) {
            return parts[0].trim();
        }

        return kakaoCategory;
    }

    /**
     * 좌표 유효성 검사
     */
    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return false;

        // 한국 좌표 범위 (대략적)
        return latitude >= 33.0 && latitude <= 43.0 &&
               longitude >= 124.0 && longitude <= 132.0;
    }

    /**
     * 전화번호 형식 정규화
     */
    public static String normalizePhoneNumber(String phone) {
        if (phone == null) return null;

        // 숫자와 하이픈만 남기기
        phone = phone.replaceAll("[^0-9-]", "");

        // 하이픈 위치 정규화
        if (phone.matches("^02\\d{7,8}$")) {
            // 서울 지역번호
            if (phone.length() == 9) {
                return phone.substring(0, 2) + "-" + phone.substring(2, 5) + "-" + phone.substring(5);
            } else if (phone.length() == 10) {
                return phone.substring(0, 2) + "-" + phone.substring(2, 6) + "-" + phone.substring(6);
            }
        } else if (phone.matches("^0\\d{9,10}$")) {
            // 기타 지역번호
            if (phone.length() == 10) {
                return phone.substring(0, 3) + "-" + phone.substring(3, 6) + "-" + phone.substring(6);
            } else if (phone.length() == 11) {
                return phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);
            }
        } else if (phone.matches("^01\\d{8,9}$")) {
            // 휴대폰
            if (phone.length() == 10) {
                return phone.substring(0, 3) + "-" + phone.substring(3, 6) + "-" + phone.substring(6);
            } else if (phone.length() == 11) {
                return phone.substring(0, 3) + "-" + phone.substring(3, 7) + "-" + phone.substring(7);
            }
        }

        return phone;
    }

    /**
     * 가격대 문자열을 숫자로 변환
     * Google Places: 0-4
     */
    public static Integer parsePriceLevel(String priceStr) {
        if (priceStr == null) return null;

        priceStr = priceStr.trim().toLowerCase();
        if (priceStr.contains("무료") || priceStr.contains("free")) {
            return 0;
        } else if (priceStr.contains("저렴") || priceStr.contains("cheap")) {
            return 1;
        } else if (priceStr.contains("보통") || priceStr.contains("moderate")) {
            return 2;
        } else if (priceStr.contains("비싼") || priceStr.contains("expensive")) {
            return 3;
        } else if (priceStr.contains("매우") || priceStr.contains("luxury")) {
            return 4;
        }

        // 숫자 추출 시도
        try {
            return Integer.parseInt(priceStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Boolean 문자열 파싱 (한국어 지원)
     */
    public static Boolean parseBoolean(String value) {
        if (value == null) return null;

        value = value.trim().toLowerCase();
        if (value.contains("가능") || value.contains("있음") || value.contains("yes") ||
            value.contains("true") || value.contains("허용") || value.contains("o")) {
            return true;
        } else if (value.contains("불가") || value.contains("없음") || value.contains("no") ||
                   value.contains("false") || value.contains("금지") || value.contains("x")) {
            return false;
        }

        return null;
    }

    /**
     * 시간 문자열 파싱 (예: "1-2시간" → "1-2시간")
     */
    public static String extractDuration(String text) {
        if (text == null) return null;

        if (text.contains("30분")) return "30분";
        if (text.contains("1시간")) return "1시간";
        if (text.contains("1-2시간")) return "1-2시간";
        if (text.contains("2시간")) return "2시간";
        if (text.contains("2-3시간")) return "2-3시간";
        if (text.contains("3-4시간")) return "3-4시간";
        if (text.contains("반나절")) return "3-4시간";
        if (text.contains("하루")) return "하루";

        return null;
    }
}