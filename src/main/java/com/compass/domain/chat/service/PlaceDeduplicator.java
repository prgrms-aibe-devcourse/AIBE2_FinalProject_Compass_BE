package com.compass.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 장소 중복 제거 서비스
 * 
 * 여러 API에서 수집한 장소의 중복을 제거하고 정보를 통합합니다.
 * - 좌표 기반 근접도 체크 (100m 이내)
 * - 이름 유사도 체크 (Levenshtein distance)
 * - 주소 일치 체크
 * - 평점 및 정보 병합
 */
@Slf4j
@Service
public class PlaceDeduplicator {

    private static final double COORDINATE_THRESHOLD = 0.001; // 약 100m (위도/경도 기준)
    private static final int NAME_SIMILARITY_THRESHOLD = 3;   // Levenshtein distance 임계값
    private static final double EARTH_RADIUS_KM = 6371.0;     // 지구 반지름 (km)

    /**
     * 중복 장소 제거 및 정보 병합
     *
     * @param places 원본 장소 리스트
     * @return 중복 제거된 장소 리스트
     */
    public List<TourPlace> deduplicate(List<TourPlace> places) {
        if (places == null || places.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("중복 제거 시작: {} 개 장소", places.size());

        // LinkedHashMap으로 순서 유지하며 중복 제거
        Map<String, TourPlace> uniquePlaces = new LinkedHashMap<>();
        
        for (TourPlace candidate : places) {
            if (candidate == null || candidate.name() == null) {
                continue; // null 체크
            }

            boolean isDuplicate = false;

            // 기존 장소들과 비교하여 중복 체크
            for (Map.Entry<String, TourPlace> entry : uniquePlaces.entrySet()) {
                TourPlace existing = entry.getValue();
                
                if (isSamePlace(candidate, existing)) {
                    // 중복 발견 - 정보 병합
                    TourPlace merged = mergePlaceInfo(existing, candidate);
                    uniquePlaces.put(entry.getKey(), merged);
                    isDuplicate = true;
                    
                    log.debug("중복 장소 병합: {} + {} -> {}", 
                             existing.name(), candidate.name(), merged.name());
                    break;
                }
            }

            if (!isDuplicate) {
                // 새로운 장소 추가
                String key = generateUniqueKey(candidate);
                uniquePlaces.put(key, candidate);
            }
        }

        List<TourPlace> result = new ArrayList<>(uniquePlaces.values());
        log.info("중복 제거 완료: {} -> {} 개 장소 ({}% 중복률)", 
                places.size(), result.size(), 
                Math.round((1.0 - (double)result.size() / places.size()) * 100));

        return result;
    }

    /**
     * 두 장소가 동일한지 판단
     * 
     * @param place1 첫 번째 장소
     * @param place2 두 번째 장소
     * @return 동일 장소 여부
     */
    private boolean isSamePlace(TourPlace place1, TourPlace place2) {
        // 1. 좌표 근접도 체크 (가장 정확한 방법)
        if (place1.latitude() != null && place1.longitude() != null &&
            place2.latitude() != null && place2.longitude() != null) {
            
            double distance = calculateDistance(
                place1.latitude(), place1.longitude(),
                place2.latitude(), place2.longitude()
            );
            
            if (distance <= 0.1) { // 100m 이내
                log.debug("좌표 기반 중복 감지: {} <-> {} (거리: {}m)", 
                         place1.name(), place2.name(), Math.round(distance * 1000));
                return true;
            }
        }

        // 2. 이름 유사도 체크
        if (place1.name() != null && place2.name() != null) {
            int nameDistance = calculateLevenshteinDistance(
                place1.name().toLowerCase().trim(),
                place2.name().toLowerCase().trim()
            );
            
            if (nameDistance <= NAME_SIMILARITY_THRESHOLD) {
                log.debug("이름 유사도 기반 중복 감지: {} <-> {} (거리: {})", 
                         place1.name(), place2.name(), nameDistance);
                return true;
            }
        }

        // 3. 주소 일치 체크
        if (place1.address() != null && place2.address() != null) {
            String addr1 = normalizeAddress(place1.address());
            String addr2 = normalizeAddress(place2.address());
            
            if (addr1.equals(addr2)) {
                log.debug("주소 기반 중복 감지: {} <-> {}", place1.name(), place2.name());
                return true;
            }
        }

        return false;
    }

    /**
     * 두 장소 정보 병합
     * 
     * @param existing 기존 장소
     * @param candidate 새로운 장소
     * @return 병합된 장소 정보
     */
    private TourPlace mergePlaceInfo(TourPlace existing, TourPlace candidate) {
        // 더 완전한 정보를 우선 선택
        String name = chooseBetter(existing.name(), candidate.name(), String::length);
        String address = chooseBetter(existing.address(), candidate.address(), String::length);
        String category = chooseBetter(existing.category(), candidate.category(), String::length);
        String description = mergeDescriptions(existing.description(), candidate.description());
        String operatingHours = chooseBetter(existing.operatingHours(), candidate.operatingHours(), String::length);
        String priceRange = chooseBetter(existing.priceRange(), candidate.priceRange(), String::length);
        
        // 좌표는 더 정확한 것 선택 (소수점 자리수가 많은 것)
        Double latitude = chooseBetter(existing.latitude(), candidate.latitude(), 
                                     lat -> lat != null ? lat.toString().length() : 0);
        Double longitude = chooseBetter(existing.longitude(), candidate.longitude(), 
                                      lon -> lon != null ? lon.toString().length() : 0);
        
        // 평점은 평균 계산
        Double rating = mergeRatings(existing.rating(), candidate.rating());
        
        // 태그는 합집합
        List<String> tags = mergeTags(existing.tags(), candidate.tags());
        
        // 출처는 병합
        String source = mergeSources(existing.source(), candidate.source());
        
        // 여행 스타일은 기존 것 우선 (첫 번째로 설정된 것 유지)
        String travelStyle = chooseBetter(existing.travelStyle(), candidate.travelStyle(), String::length);

        return new TourPlace(
            generateUniqueId(name, address),
            name,
            address,
            latitude,
            longitude,
            category,
            rating,
            description,
            operatingHours,
            priceRange,
            tags,
            source,
            travelStyle,
            existing.timeBlock() != null ? existing.timeBlock() : candidate.timeBlock(),
            existing.day() != null ? existing.day() : candidate.day(),
            chooseBetter(existing.recommendTime(), candidate.recommendTime(), String::length)
        );
    }

    /**
     * Haversine 공식으로 두 좌표 간 거리 계산 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Levenshtein Distance 계산 (문자열 유사도)
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }
        
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }
        
        return dp[len1][len2];
    }

    /**
     * 주소 정규화 (비교를 위한)
     */
    private String normalizeAddress(String address) {
        if (address == null) return "";
        
        return address.toLowerCase()
                     .replaceAll("\\s+", " ")           // 공백 정규화
                     .replaceAll("[()\\[\\]]", "")      // 괄호 제거
                     .replaceAll("특별시|광역시|시|군|구", "") // 행정구역 단위 제거
                     .trim();
    }

    /**
     * 더 나은 값 선택 (null이 아니고 더 긴/큰 값)
     */
    private <T> T chooseBetter(T existing, T candidate, java.util.function.ToIntFunction<T> scorer) {
        if (existing == null) return candidate;
        if (candidate == null) return existing;
        
        return scorer.applyAsInt(candidate) > scorer.applyAsInt(existing) ? candidate : existing;
    }

    /**
     * 설명 병합
     */
    private String mergeDescriptions(String desc1, String desc2) {
        if (desc1 == null) return desc2;
        if (desc2 == null) return desc1;
        
        // 중복 내용 제거하고 병합
        if (desc1.contains(desc2) || desc2.contains(desc1)) {
            return desc1.length() > desc2.length() ? desc1 : desc2;
        }
        
        return desc1 + " " + desc2;
    }

    /**
     * 평점 병합 (평균 계산)
     */
    private Double mergeRatings(Double rating1, Double rating2) {
        if (rating1 == null) return rating2;
        if (rating2 == null) return rating1;
        
        return Math.round((rating1 + rating2) / 2.0 * 10.0) / 10.0; // 소수점 1자리
    }

    /**
     * 태그 병합 (합집합)
     */
    private List<String> mergeTags(List<String> tags1, List<String> tags2) {
        Set<String> mergedTags = new HashSet<>();
        
        if (tags1 != null) mergedTags.addAll(tags1);
        if (tags2 != null) mergedTags.addAll(tags2);
        
        return new ArrayList<>(mergedTags);
    }

    /**
     * 출처 병합
     */
    private String mergeSources(String source1, String source2) {
        if (source1 == null) return source2;
        if (source2 == null) return source1;
        
        if (source1.equals(source2)) return source1;
        
        return source1 + ", " + source2;
    }

    /**
     * 고유 키 생성
     */
    private String generateUniqueKey(TourPlace place) {
        return place.name() + "_" + System.nanoTime();
    }

    /**
     * 고유 ID 생성
     */
    private String generateUniqueId(String name, String address) {
        String base = (name != null ? name : "") + (address != null ? address : "");
        return "place_" + Math.abs(base.hashCode());
    }

    /**
     * 관광지 Record (PlaceSelectionService와 동일한 구조)
     */
    public record TourPlace(
        String id,                   // 고유 식별자
        String name,                 // 장소명
        String address,              // 주소
        Double latitude,             // 위도
        Double longitude,            // 경도
        String category,             // 카테고리 (맛집, 카페, 관광지 등)
        Double rating,               // 평점 (1-5)
        String description,          // 설명
        String operatingHours,       // 운영시간
        String priceRange,           // 가격대 ($~$$$$)
        List<String> tags,           // 태그
        String source,               // 출처 (TourAPI, Perplexity 등)
        String travelStyle,          // 여행 스타일
        
        // 🔥 새 가이드 추가 필드
        String timeBlock,            // 시간대 블록 (BREAKFAST, MORNING_ACTIVITY 등)
        Integer day,                 // 여행 일차 (1, 2, 3)
        String recommendTime         // 추천 방문 시간 (예: "10:00-11:30")
    ) {}
}
