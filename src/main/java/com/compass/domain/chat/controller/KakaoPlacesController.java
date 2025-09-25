package com.compass.domain.chat.controller;

import com.compass.domain.chat.entity.TourPlace;
import com.compass.domain.chat.function.external.SearchKakaoPlacesFunction;
import com.compass.domain.chat.repository.TourPlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Kakao Places API를 사용한 장소 검색 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/kakao-places")
@RequiredArgsConstructor
public class KakaoPlacesController {
    
    private final SearchKakaoPlacesFunction kakaoPlacesFunction;
    private final TourPlaceRepository tourPlaceRepository;
    
    /**
     * Kakao Places API로 장소 검색 및 저장
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchAndSavePlaces(
            @RequestBody KakaoSearchRequest request) {
        
        log.info("Kakao Places API 검색 시작: {}", request);
        
        try {
            log.info("SearchKakaoPlacesFunction Bean 확인: {}", kakaoPlacesFunction != null ? "정상" : "NULL");
            
            // Kakao API는 최대 15개까지만 지원하므로 여러 번 호출
            List<SearchKakaoPlacesFunction.KakaoPlace> kakaoPlaces = new ArrayList<>();
            int totalRequested = request.getSize();
            int maxPerCall = 15;
            int callsNeeded = (totalRequested + maxPerCall - 1) / maxPerCall; // 올림 계산
            
            // 클러스터별 동적 키워드 생성
            String[] keywords = generateClusterKeywords(request.getClusterName());
            
            for (int i = 0; i < keywords.length && kakaoPlaces.size() < totalRequested; i++) {
                int remainingNeeded = totalRequested - kakaoPlaces.size();
                int currentCallSize = Math.min(maxPerCall, remainingNeeded);
                
                List<SearchKakaoPlacesFunction.KakaoPlace> batchPlaces = 
                    kakaoPlacesFunction.searchPlaces(keywords[i], "", currentCallSize);
                
                kakaoPlaces.addAll(batchPlaces);
                log.info("키워드 '{}': {}개 장소 추가 (총 {}개)", keywords[i], batchPlaces.size(), kakaoPlaces.size());
                
                // API 호출 간격 (과도한 호출 방지)
                if (i < keywords.length - 1) {
                    Thread.sleep(200); // 200ms 대기
                }
            }
            
            log.info("Kakao API 검색 결과: {}개 장소", kakaoPlaces.size());
            
            // Kakao Place를 TourPlace로 변환 (시간블록 분배 포함)
            List<TourPlace> tourPlaces = new ArrayList<>();
            for (int i = 0; i < kakaoPlaces.size(); i++) {
                SearchKakaoPlacesFunction.KakaoPlace kakaoPlace = kakaoPlaces.get(i);
                
                // 여행에 적합하지 않은 장소 필터링
                if (!isTravelSuitablePlace(kakaoPlace)) {
                    log.debug("여행 부적합 장소 필터링: {} - {}", kakaoPlace.getName(), kakaoPlace.getCategory());
                    continue;
                }
                
                TourPlace tourPlace = convertKakaoPlaceToTourPlace(kakaoPlace, request.getClusterName(), i);
                tourPlaces.add(tourPlace);
            }
            
            // 중복 제거 후 DB에 저장
            List<TourPlace> uniquePlaces = filterDuplicatePlaces(tourPlaces);
            
            // 저장 전 overview 확인
            for (TourPlace place : uniquePlaces) {
                log.info("저장 전 장소: {} - overview: {}", place.getName(), place.getOverview());
            }
            
            List<TourPlace> savedPlaces = tourPlaceRepository.saveAll(uniquePlaces);
            
            // 저장된 장소들의 overview 확인
            for (TourPlace savedPlace : savedPlaces) {
                log.info("저장된 장소: {} - overview: {}", savedPlace.getName(), savedPlace.getOverview());
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalPlaces", savedPlaces.size());
            result.put("clusterName", request.getClusterName());
            result.put("message", "Kakao Places API 검색 및 저장 완료");
            
            log.info("Kakao Places API 검색 완료: {}개 장소 저장", savedPlaces.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Kakao Places API 검색 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("message", "Kakao Places API 검색 실패");
            
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }
    
    /**
     * 중복 장소 필터링
     */
    private List<TourPlace> filterDuplicatePlaces(List<TourPlace> places) {
        Set<String> uniqueNames = new HashSet<>();
        List<TourPlace> uniquePlaces = new ArrayList<>();
        
        for (TourPlace place : places) {
            String name = place.getName();
            if (!uniqueNames.contains(name)) {
                uniqueNames.add(name);
                uniquePlaces.add(place);
            }
        }
        
        log.info("중복 제거: {}개 -> {}개", places.size(), uniquePlaces.size());
        return uniquePlaces;
    }
    
    /**
     * 한글 클러스터명을 영어명으로 정규화
     */
    private String normalizeClusterName(String clusterName) {
        return switch (clusterName.toLowerCase().trim()) {
            // 서울 지역
            case "홍대", "홍익대", "홍익대학교" -> "hongdae";
            case "강남", "강남역" -> "gangnam";
            case "성수", "성수동" -> "sungsu";
            case "종로", "종로구" -> "jongno";
            case "이태원" -> "itaewon";
            case "북촌", "북촌한옥마을" -> "bukchon";
            case "명동", "명동거리" -> "myeongdong";
            case "인사동" -> "insadong";
            case "남산", "남산타워" -> "namsan";
            case "동대문", "동대문시장" -> "dongdaemun";
            case "코엑스" -> "coex";
            
            // 부산 지역
            case "해운대", "해운대해수욕장" -> "haeundae";
            case "광안리", "광안리해수욕장" -> "gwanganri";
            case "부산역" -> "busan_station";
            case "남포", "남포동" -> "nampo";
            case "부산" -> "busan";
            
            // 제주도 지역
            case "제주시", "제주" -> "jeju_city";
            case "서귀포", "서귀포시" -> "seogwipo";
            case "중문", "중문관광단지" -> "jungmun";
            case "성산", "성산일출봉" -> "seongsan";
            case "제주도" -> "jeju";
            
            // 대구 지역
            case "동성로" -> "dongseongno";
            case "대구역" -> "daegu_station";
            case "대구" -> "daegu";
            
            // 광주 지역
            case "광주" -> "gwangju_center";
            
            // 기타 지역
            case "대전" -> "daejeon";
            case "울산" -> "ulsan";
            case "인천" -> "incheon";
            case "수원" -> "suwon";
            case "천안" -> "cheonan";
            case "청주" -> "cheongju";
            case "전주" -> "jeonju";
            case "여수" -> "yeosu";
            case "포항" -> "pohang";
            case "창원" -> "changwon";
            case "김해" -> "gimhae";
            
            // 기본값: 입력값 그대로 반환 (이미 영어명이거나 새로운 지역)
            default -> clusterName;
        };
    }

    /**
     * 클러스터별 동적 키워드 생성 (한글명 지원 + 유명 관광지 추가)
     */
    private String[] generateClusterKeywords(String clusterName) {
        // 한글명을 영어명으로 변환
        String normalizedName = normalizeClusterName(clusterName);
        
        return switch (normalizedName.toLowerCase()) {
            case "hongdae" -> new String[]{
                "홍대 맛집", "홍대 카페", "홍대 레스토랑", "홍대 브런치", "홍대 디저트",
                "홍대입구 맛집", "홍대입구 카페", "홍대 관광", "홍대 쇼핑", "홍대 문화"
            };
            case "gangnam" -> new String[]{
                "강남 맛집", "강남 카페", "강남 레스토랑", "강남 브런치", "강남 디저트",
                "강남역 맛집", "강남역 카페", "강남 쇼핑", "강남 관광", "강남 엔터테인먼트"
            };
            case "sungsu" -> new String[]{
                "성수 맛집", "성수 카페", "성수 레스토랑", "성수 브런치", "성수 디저트",
                "성수동 맛집", "성수동 카페", "성수 갤러리", "성수 관광", "성수 문화"
            };
            case "jongno" -> new String[]{
                "종로 맛집", "종로 카페", "종로 레스토랑", "종로 전통", "종로 디저트",
                "인사동 맛집", "인사동 카페", "종로 관광", "종로 문화", "종로 전통음식"
            };
            case "itaewon" -> new String[]{
                "이태원 맛집", "이태원 카페", "이태원 레스토랑", "이태원 바", "이태원 디저트",
                "이태원 외국음식", "이태원 클럽", "이태원 관광", "이태원 문화", "이태원 나이트라이프"
            };
            case "bukchon" -> new String[]{
                "북촌 맛집", "북촌 카페", "북촌 레스토랑", "북촌 전통", "북촌 디저트",
                "삼청동 맛집", "삼청동 카페", "북촌 갤러리", "북촌 관광", "북촌 전통문화"
            };
            
            // 서울 유명 관광지
            case "myeongdong" -> new String[]{
                "명동 맛집", "명동 카페", "명동 쇼핑", "명동 관광", "명동 문화",
                "명동 스트리트푸드", "명동 쇼핑몰", "명동 관광", "명동 엔터테인먼트"
            };
            case "insadong" -> new String[]{
                "인사동 맛집", "인사동 카페", "인사동 전통", "인사동 관광", "인사동 문화",
                "인사동 전통음식", "인사동 갤러리", "인사동 공예", "인사동 전통문화"
            };
            case "namsan" -> new String[]{
                "남산 맛집", "남산 카페", "남산 관광", "남산 문화", "남산 전망대",
                "남산타워 맛집", "남산타워 카페", "남산 관광", "남산 야경"
            };
            case "dongdaemun" -> new String[]{
                "동대문 맛집", "동대문 카페", "동대문 쇼핑", "동대문 관광", "동대문 문화",
                "동대문 시장", "동대문 패션", "동대문 쇼핑", "동대문 엔터테인먼트"
            };
            case "coex" -> new String[]{
                "코엑스 맛집", "코엑스 카페", "코엑스 쇼핑", "코엑스 관광", "코엑스 문화",
                "코엑스 아쿠아리움", "코엑스 쇼핑몰", "코엑스 관광", "코엑스 엔터테인먼트"
            };
            
            // 부산 유명 관광지
            case "haeundae" -> new String[]{
                "해운대 맛집", "해운대 카페", "해운대 관광", "해운대 해변", "해운대 문화",
                "해운대 해물", "해운대 바다", "해운대 관광", "해운대 엔터테인먼트"
            };
            case "gwanganri" -> new String[]{
                "광안리 맛집", "광안리 카페", "광안리 관광", "광안리 해변", "광안리 문화",
                "광안리 해물", "광안리 바다", "광안리 관광", "광안리 엔터테인먼트"
            };
            case "busan_station" -> new String[]{
                "부산역 맛집", "부산역 카페", "부산역 관광", "부산역 문화", "부산역 쇼핑",
                "부산역 해물", "부산역 관광", "부산역 엔터테인먼트"
            };
            case "nampo" -> new String[]{
                "남포 맛집", "남포 카페", "남포 관광", "남포 문화", "남포 쇼핑",
                "남포 해물", "남포 관광", "남포 엔터테인먼트"
            };
            case "busan" -> new String[]{
                "부산 맛집", "부산 카페", "부산 관광", "부산 해변", "부산 문화",
                "부산 해물", "부산 바다", "부산 관광", "부산 엔터테인먼트"
            };
            
            // 제주도 유명 관광지
            case "jeju_city" -> new String[]{
                "제주시 맛집", "제주시 카페", "제주시 관광", "제주시 문화", "제주시 쇼핑",
                "제주 맛집", "제주 카페", "제주 관광", "제주 문화"
            };
            case "seogwipo" -> new String[]{
                "서귀포 맛집", "서귀포 카페", "서귀포 관광", "서귀포 문화", "서귀포 자연",
                "서귀포 해변", "서귀포 관광", "서귀포 엔터테인먼트"
            };
            case "jungmun" -> new String[]{
                "중문 맛집", "중문 카페", "중문 관광", "중문 문화", "중문 자연",
                "중문 해변", "중문 관광", "중문 엔터테인먼트"
            };
            case "seongsan" -> new String[]{
                "성산 맛집", "성산 카페", "성산 관광", "성산 문화", "성산 자연",
                "성산일출봉", "성산 관광", "성산 엔터테인먼트"
            };
            case "jeju" -> new String[]{
                "제주 맛집", "제주 카페", "제주 관광", "제주 자연", "제주 해변",
                "제주 문화", "제주 쇼핑", "제주 엔터테인먼트"
            };
            
            // 대구 유명 관광지
            case "dongseongno" -> new String[]{
                "동성로 맛집", "동성로 카페", "동성로 쇼핑", "동성로 관광", "동성로 문화",
                "동성로 패션", "동성로 쇼핑", "동성로 엔터테인먼트"
            };
            case "daegu_station" -> new String[]{
                "대구역 맛집", "대구역 카페", "대구역 관광", "대구역 문화", "대구역 쇼핑",
                "대구역 관광", "대구역 엔터테인먼트"
            };
            case "daegu" -> new String[]{
                "대구 맛집", "대구 카페", "대구 관광", "대구 문화", "대구 쇼핑",
                "대구 관광", "대구 엔터테인먼트"
            };
            
            // 광주 지역
            case "gwangju_center" -> new String[]{
                "광주 맛집", "광주 카페", "광주 관광", "광주 문화", "광주 쇼핑",
                "광주 예술", "광주 관광", "광주 엔터테인먼트"
            };
            
            // 기타 지역 (동적 생성)
            default -> new String[]{
                clusterName + " 맛집", clusterName + " 카페", clusterName + " 레스토랑", 
                clusterName + " 관광", clusterName + " 문화", clusterName + " 쇼핑", 
                clusterName + " 엔터테인먼트"
            };
        };
    }
    
    /**
     * Kakao Place를 TourPlace로 변환 (카테고리 매핑 개선 + 클러스터별 시간블록 분배)
     */
    private TourPlace convertKakaoPlaceToTourPlace(SearchKakaoPlacesFunction.KakaoPlace kakaoPlace, String clusterName, int placeIndex) {
        String kakaoCategory = kakaoPlace.getCategory();
        String mappedCategory = mapKakaoCategoryToTourCategory(kakaoCategory);
        String operatingHours = getOperatingHoursByCategory(mappedCategory);
        
        // 클러스터별 여행 스타일을 고려한 시간블록 분배
        String timeBlock = getTimeBlockByClusterAndIndex(clusterName, placeIndex, mappedCategory);
        String recommendTime = getRecommendTimeByTimeBlock(timeBlock);
        
        // 위도/경도 정보 파싱 (Kakao API: x=경도, y=위도)
        Double latitude = parseCoordinate(kakaoPlace.getY());
        Double longitude = parseCoordinate(kakaoPlace.getX());
        
        // 간단한 description과 상세한 overview 생성
        String simpleDescription = generateSimpleDescription(kakaoPlace.getName(), mappedCategory, timeBlock);
        String detailedOverview = generateDetailedDescription(kakaoPlace.getName(), mappedCategory, timeBlock, clusterName);
        log.info("생성된 simple description: {}", simpleDescription);
        log.info("생성된 detailed overview: {}", detailedOverview);
        
        // TourPlace 객체를 생성자로 직접 생성 (Builder 대신)
        TourPlace tourPlace = new TourPlace();
        tourPlace.setExternalId("kakao_" + System.currentTimeMillis() + "_" + kakaoPlace.getName().hashCode());
        tourPlace.setName(kakaoPlace.getName());
        tourPlace.setAddress(kakaoPlace.getAddress());
        tourPlace.setOperatingHours(operatingHours);
        tourPlace.setDescription(simpleDescription);
        tourPlace.setOverview(detailedOverview);
        tourPlace.setRating(getDefaultRatingByCategory(mappedCategory));
        tourPlace.setCategory(mappedCategory);
        tourPlace.setTimeBlock(timeBlock);
        tourPlace.setRecommendTime(recommendTime);
        tourPlace.setSource("KakaoPlaces");
        tourPlace.setIsTrendy(true);
        tourPlace.setClusterName(clusterName);
        tourPlace.setMatchScore(0.9);
        tourPlace.setThreadId("kakao-places-001");
        tourPlace.setLatitude(latitude);
        tourPlace.setLongitude(longitude);
        
        return tourPlace;
    }
    
    /**
     * 좌표 문자열을 Double로 파싱
     */
    private Double parseCoordinate(String coordinate) {
        if (coordinate == null || coordinate.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(coordinate.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 간단한 description 생성 (DB의 description 필드용) - 장소를 한 줄로 요약
     */
    private String generateSimpleDescription(String placeName, String category, String timeBlock) {
        return generatePlaceSummary(placeName, category, timeBlock);
    }
    
    /**
     * 장소를 한 줄로 요약하는 메서드
     */
    private String generatePlaceSummary(String placeName, String category, String timeBlock) {
        // 장소명에서 특징을 파악해서 한 줄 요약 생성
        String summary = analyzePlaceName(placeName);
        
        if (!summary.isEmpty()) {
            return summary;
        }
        
        // 기본적인 시간대별 설명
        return getTimeBasedDescription(category, timeBlock);
    }
    
    /**
     * 장소명을 분석해서 특징을 파악
     */
    private String analyzePlaceName(String placeName) {
        // 브랜드별 특징
        if (placeName.contains("스타벅스")) return "세계적인 커피 브랜드";
        if (placeName.contains("투썸플레이스")) return "디저트가 맛있는 카페";
        if (placeName.contains("이디야")) return "합리적인 가격의 커피";
        if (placeName.contains("커피빈")) return "프리미엄 커피 전문점";
        if (placeName.contains("메가커피")) return "대용량 커피 전문점";
        if (placeName.contains("빽다방")) return "저렴한 가격의 커피";
        
        // 영화관
        if (placeName.contains("CGV")) return "최신 영화관";
        if (placeName.contains("롯데시네마")) return "롯데 시네마";
        if (placeName.contains("메가박스")) return "메가박스 영화관";
        
        // 패스트푸드
        if (placeName.contains("맥도날드")) return "맥도날드 패스트푸드";
        if (placeName.contains("버거킹")) return "버거킹 햄버거";
        if (placeName.contains("KFC")) return "KFC 치킨";
        if (placeName.contains("롯데리아")) return "롯데리아 햄버거";
        
        // 카페 특징
        if (placeName.contains("땡스네이쳐")) return "자연친화적인 힐링 카페";
        if (placeName.contains("디저트")) return "디저트 전문 카페";
        if (placeName.contains("브런치")) return "브런치 전문 카페";
        if (placeName.contains("베이커리")) return "신선한 빵과 커피";
        if (placeName.contains("테마")) return "특별한 테마 카페";
        if (placeName.contains("문화")) return "문화 공간";
        
        // 음식점 특징
        if (placeName.contains("강강술래")) return "홍대 인기 술집";
        if (placeName.contains("칸다소바")) return "일본식 소바 전문점";
        if (placeName.contains("스시")) return "일본식 스시 전문점";
        if (placeName.contains("라멘")) return "일본식 라멘 전문점";
        if (placeName.contains("파스타")) return "이탈리안 파스타 전문점";
        if (placeName.contains("스테이크")) return "고급 스테이크 전문점";
        if (placeName.contains("한식")) return "전통 한식당";
        if (placeName.contains("중식")) return "중국 요리 전문점";
        if (placeName.contains("일식")) return "일본 요리 전문점";
        if (placeName.contains("양식")) return "서양 요리 전문점";
        
        // 관광지 특징
        if (placeName.contains("경복궁")) return "조선왕조의 대표 궁궐";
        if (placeName.contains("궁")) return "역사적인 궁궐";
        if (placeName.contains("사찰")) return "전통 사찰";
        if (placeName.contains("박물관")) return "문화와 역사를 배울 수 있는 곳";
        if (placeName.contains("미술관")) return "예술 작품을 감상할 수 있는 곳";
        if (placeName.contains("갤러리")) return "현대 미술 갤러리";
        if (placeName.contains("공원")) return "자연을 즐길 수 있는 공원";
        if (placeName.contains("한옥")) return "전통 한옥";
        
        // 쇼핑
        if (placeName.contains("백화점")) return "고급 쇼핑몰";
        if (placeName.contains("아울렛")) return "할인 쇼핑 아울렛";
        if (placeName.contains("마트")) return "생활용품 쇼핑";
        if (placeName.contains("편의점")) return "편의점";
        
        return "";
    }
    
    /**
     * 시간대별 기본 설명
     */
    private String getTimeBasedDescription(String category, String timeBlock) {
        switch (category) {
            case "카페":
                if (timeBlock.equals("BREAKFAST")) return "아침에 마시기 좋은 커피";
                if (timeBlock.equals("LUNCH")) return "점심시간에 편안한 카페";
                if (timeBlock.equals("DINNER")) return "저녁에 분위기 좋은 카페";
                return "언제든지 방문하기 좋은 카페";
                
            case "맛집":
                if (timeBlock.equals("BREAKFAST")) return "아침에 든든한 식사";
                if (timeBlock.equals("LUNCH")) return "점심에 맛있는 식사";
                if (timeBlock.equals("DINNER")) return "저녁에 특별한 식사";
                return "언제든지 맛있는 식사";
                
            case "관광지":
                return "지역의 대표적인 관광지";
                
            case "문화시설":
                return "문화 활동을 즐길 수 있는 곳";
                
            case "쇼핑":
                return "쇼핑을 즐길 수 있는 곳";
                
            case "엔터테인먼트":
                return "재미있는 활동을 할 수 있는 곳";
                
            default:
                return "방문하기 좋은 장소";
        }
    }
    
    /**
     * 장소명에서 특징 추출
     */
    private String extractNameFeature(String placeName) {
        // 브랜드명 추출
        if (placeName.contains("스타벅스")) return "스타벅스";
        if (placeName.contains("투썸플레이스")) return "투썸플레이스";
        if (placeName.contains("이디야")) return "이디야";
        if (placeName.contains("커피빈")) return "커피빈";
        if (placeName.contains("메가커피")) return "메가커피";
        if (placeName.contains("빽다방")) return "빽다방";
        
        // 영화관
        if (placeName.contains("CGV")) return "CGV";
        if (placeName.contains("롯데시네마")) return "롯데시네마";
        if (placeName.contains("메가박스")) return "메가박스";
        
        // 패스트푸드
        if (placeName.contains("맥도날드")) return "맥도날드";
        if (placeName.contains("버거킹")) return "버거킹";
        if (placeName.contains("KFC")) return "KFC";
        if (placeName.contains("롯데리아")) return "롯데리아";
        
        // 카페 특징
        if (placeName.contains("디저트")) return "디저트 전문";
        if (placeName.contains("브런치")) return "브런치";
        if (placeName.contains("베이커리")) return "베이커리";
        if (placeName.contains("테마")) return "테마";
        
        // 음식점 특징
        if (placeName.contains("한식")) return "한식";
        if (placeName.contains("중식")) return "중식";
        if (placeName.contains("일식")) return "일식";
        if (placeName.contains("양식")) return "양식";
        if (placeName.contains("이탈리안")) return "이탈리안";
        if (placeName.contains("프렌치")) return "프렌치";
        if (placeName.contains("스시")) return "스시";
        if (placeName.contains("라멘")) return "라멘";
        if (placeName.contains("파스타")) return "파스타";
        if (placeName.contains("스테이크")) return "스테이크";
        
        // 관광지 특징
        if (placeName.contains("궁")) return "궁궐";
        if (placeName.contains("사찰")) return "사찰";
        if (placeName.contains("박물관")) return "박물관";
        if (placeName.contains("미술관")) return "미술관";
        if (placeName.contains("갤러리")) return "갤러리";
        if (placeName.contains("공원")) return "공원";
        if (placeName.contains("한옥")) return "한옥";
        
        // 쇼핑
        if (placeName.contains("백화점")) return "백화점";
        if (placeName.contains("아울렛")) return "아울렛";
        if (placeName.contains("마트")) return "마트";
        if (placeName.contains("편의점")) return "편의점";
        
        return "";
    }
    
    /**
     * 카테고리별 설명
     */
    private String getCategoryDescription(String category, String timeBlock) {
        switch (category) {
            case "카페":
                if (timeBlock.equals("BREAKFAST")) {
                    return "아침 커피";
                } else if (timeBlock.equals("LUNCH")) {
                    return "점심 카페";
                } else if (timeBlock.equals("DINNER")) {
                    return "저녁 카페";
                } else {
                    return "카페";
                }
                
            case "맛집":
                if (timeBlock.equals("BREAKFAST")) {
                    return "아침 식사";
                } else if (timeBlock.equals("LUNCH")) {
                    return "점심 식사";
                } else if (timeBlock.equals("DINNER")) {
                    return "저녁 식사";
                } else {
                    return "맛집";
                }
                
            case "관광지":
                return "관광지";
                
            case "문화시설":
                return "문화시설";
                
            case "쇼핑":
                return "쇼핑";
                
            case "엔터테인먼트":
                return "엔터테인먼트";
                
            default:
                return "장소";
        }
    }
    
    /**
     * 카테고리와 시간대에 맞는 상세한 description 생성
     */
    private String generateDetailedDescription(String placeName, String category, String timeBlock, String clusterName) {
        StringBuilder description = new StringBuilder();
        
        // 카테고리별 기본 설명
        switch (category) {
            case "카페":
                if (timeBlock.equals("BREAKFAST")) {
                    description.append("아침에 마시기 좋은 커피와 디저트를 즐길 수 있는 ");
                } else if (timeBlock.equals("LUNCH")) {
                    description.append("점심시간에 편안하게 쉴 수 있는 ");
                } else if (timeBlock.equals("DINNER")) {
                    description.append("저녁에 분위기 좋은 ");
                } else {
                    description.append("언제든지 방문하기 좋은 ");
                }
                description.append("카페입니다.");
                break;
                
            case "관광지":
                if (placeName.contains("궁") || placeName.contains("궁궐")) {
                    description.append("조선왕조의 대표 궁궐로 역사적 의미가 깊은 ");
                } else if (placeName.contains("거리") || placeName.contains("거리")) {
                    description.append("젊은 문화와 트렌드를 만날 수 있는 ");
                } else {
                    description.append("지역의 대표적인 ");
                }
                description.append("관광 명소입니다.");
                break;
                
            case "맛집":
                if (timeBlock.equals("BREAKFAST")) {
                    description.append("아침에 든든하게 먹을 수 있는 ");
                } else if (timeBlock.equals("LUNCH")) {
                    description.append("점심에 맛있게 즐길 수 있는 ");
                } else if (timeBlock.equals("DINNER")) {
                    description.append("저녁에 특별한 식사를 할 수 있는 ");
                } else {
                    description.append("언제든지 맛있는 ");
                }
                description.append("맛집입니다.");
                break;
                
            case "문화시설":
                if (placeName.contains("영화") || placeName.contains("시네마")) {
                    description.append("최신 영화를 감상할 수 있는 ");
                } else if (placeName.contains("박물관") || placeName.contains("미술관")) {
                    description.append("문화와 예술을 체험할 수 있는 ");
                } else {
                    description.append("문화 활동을 즐길 수 있는 ");
                }
                description.append("문화시설입니다.");
                break;
                
            case "쇼핑":
                description.append("다양한 쇼핑을 즐길 수 있는 ");
                description.append("쇼핑 명소입니다.");
                break;
                
            case "엔터테인먼트":
                if (placeName.contains("노래") || placeName.contains("연습")) {
                    description.append("친구들과 함께 즐길 수 있는 ");
                } else {
                    description.append("재미있는 활동을 할 수 있는 ");
                }
                description.append("엔터테인먼트 시설입니다.");
                break;
                
            case "교통":
                description.append("편리한 교통 접근이 가능한 ");
                description.append("교통 시설입니다.");
                break;
                
            default:
                description.append("방문하기 좋은 ");
                description.append("장소입니다.");
                break;
        }
        
        // 클러스터별 추가 정보
        if (clusterName != null && !clusterName.isEmpty()) {
            description.append(" ").append(clusterName).append(" 지역의 대표적인 장소로, ");
            if (clusterName.contains("홍대")) {
                description.append("젊은 문화와 트렌드의 중심지입니다.");
            } else if (clusterName.contains("명동")) {
                description.append("서울의 대표적인 상업지역입니다.");
            } else if (clusterName.contains("경복궁")) {
                description.append("역사와 전통이 살아있는 지역입니다.");
            } else {
                description.append("특별한 매력이 있는 지역입니다.");
            }
        }
        
        return description.toString();
    }
    
    /**
     * Kakao 카테고리를 TourPlace 카테고리로 매핑 (실제 API 응답 기반)
     */
    private String mapKakaoCategoryToTourCategory(String kakaoCategory) {
        if (kakaoCategory == null || kakaoCategory.isEmpty()) {
            return "기타";
        }
        
        // 카페는 우선적으로 체크 (음식점보다 먼저)
        if (kakaoCategory.contains("카페") || kakaoCategory.contains("테마카페") || kakaoCategory.contains("디저트카페")) {
            return "카페";
        }
        
        // 음식점 관련 (카페 제외)
        if (kakaoCategory.contains("음식점") || kakaoCategory.contains("레스토랑")) {
            return "음식점";
        }
        
        if (kakaoCategory.contains("관광") || kakaoCategory.contains("명소") || kakaoCategory.contains("공원") || 
            kakaoCategory.contains("문화시설") || kakaoCategory.contains("박물관") || kakaoCategory.contains("미술관")) {
            return "관광지";
        }
        
        if (kakaoCategory.contains("쇼핑") || kakaoCategory.contains("마트") || kakaoCategory.contains("백화점") || 
            kakaoCategory.contains("편의점") || kakaoCategory.contains("마트")) {
            return "쇼핑";
        }
        
        if (kakaoCategory.contains("문화") || kakaoCategory.contains("예술") || kakaoCategory.contains("갤러리") || 
            kakaoCategory.contains("공연장") || kakaoCategory.contains("극장")) {
            return "문화시설";
        }
        
        if (kakaoCategory.contains("엔터테인먼트") || kakaoCategory.contains("영화") || kakaoCategory.contains("노래방") || 
            kakaoCategory.contains("PC방") || kakaoCategory.contains("오락실")) {
            return "엔터테인먼트";
        }
        
        if (kakaoCategory.contains("교통") || kakaoCategory.contains("지하철") || kakaoCategory.contains("버스") || 
            kakaoCategory.contains("역") || kakaoCategory.contains("공항")) {
            return "교통";
        }
        
        if (kakaoCategory.contains("숙박") || kakaoCategory.contains("호텔") || kakaoCategory.contains("펜션") || 
            kakaoCategory.contains("게스트하우스") || kakaoCategory.contains("리조트")) {
            return "숙박";
        }
        
        if (kakaoCategory.contains("학교") || kakaoCategory.contains("교육") || kakaoCategory.contains("대학교")) {
            return "교육시설";
        }
        
        if (kakaoCategory.contains("병원") || kakaoCategory.contains("약국") || kakaoCategory.contains("의료")) {
            return "의료시설";
        }
        
        if (kakaoCategory.contains("미용") || kakaoCategory.contains("헤어") || kakaoCategory.contains("네일")) {
            return "미용시설";
        }
        
        return "기타";
    }
    
    /**
     * 카테고리별 운영시간 설정
     */
    private String getOperatingHoursByCategory(String category) {
        return switch (category) {
            case "음식점" -> "11:00-22:00";
            case "카페" -> "08:00-23:00";
            case "관광지" -> "09:00-18:00";
            case "쇼핑" -> "10:00-22:00";
            case "문화시설" -> "10:00-19:00";
            case "엔터테인먼트" -> "10:00-24:00";
            case "교통" -> "05:00-01:00";
            case "숙박" -> "24시간";
            case "교육시설" -> "09:00-18:00";
            case "의료시설" -> "09:00-18:00";
            case "미용시설" -> "10:00-21:00";
            default -> "운영시간 정보 없음";
        };
    }
    
    /**
     * 카테고리별 시간블록 설정
     */
    private String getTimeBlockByCategory(String category) {
        return switch (category) {
            case "음식점" -> "LUNCH"; // 점심으로 기본 설정
            case "카페" -> "CAFE";
            case "관광지" -> "MORNING_ACTIVITY";
            case "쇼핑" -> "AFTERNOON_ACTIVITY";
            case "문화시설" -> "AFTERNOON_ACTIVITY";
            case "엔터테인먼트" -> "EVENING_ACTIVITY";
            case "교통" -> "MORNING_ACTIVITY";
            case "숙박" -> "MORNING_ACTIVITY";
            case "교육시설" -> "MORNING_ACTIVITY";
            case "의료시설" -> "MORNING_ACTIVITY";
            case "미용시설" -> "AFTERNOON_ACTIVITY";
            default -> "BREAKFAST";
        };
    }
    
    /**
     * 클러스터별 여행 스타일을 고려한 시간블록 분배 (70개를 7개 블록으로 10개씩 분배)
     */
    private String getTimeBlockByClusterAndIndex(String clusterName, int placeIndex, String category) {
        // 클러스터별 여행 스타일 정의
        Map<String, String[]> clusterTimeBlocks = Map.of(
            "hongdae", new String[]{"BREAKFAST", "MORNING_ACTIVITY", "LUNCH", "CAFE", "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"},
            "gangnam", new String[]{"BREAKFAST", "MORNING_ACTIVITY", "LUNCH", "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY", "EVENING_ACTIVITY"}, // 쇼핑/엔터테인먼트 강화
            "sungsu", new String[]{"BREAKFAST", "CAFE", "LUNCH", "CAFE", "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"}, // 카페 강화
            "jongno", new String[]{"BREAKFAST", "MORNING_ACTIVITY", "MORNING_ACTIVITY", "LUNCH", "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"}, // 관광지 강화
            "itaewon", new String[]{"BREAKFAST", "MORNING_ACTIVITY", "LUNCH", "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY", "EVENING_ACTIVITY"}, // 나이트라이프 강화
            "bukchon", new String[]{"BREAKFAST", "MORNING_ACTIVITY", "MORNING_ACTIVITY", "LUNCH", "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"} // 전통문화 강화
        );
        
        String[] timeBlocks = clusterTimeBlocks.getOrDefault(clusterName, 
            new String[]{"BREAKFAST", "MORNING_ACTIVITY", "LUNCH", "CAFE", "AFTERNOON_ACTIVITY", "DINNER", "EVENING_ACTIVITY"});
        
        // 70개를 7개 블록으로 10개씩 분배
        int blockIndex = (placeIndex / 10) % timeBlocks.length;
        return timeBlocks[blockIndex];
    }
    
    /**
     * 여행에 적합한 장소인지 판단
     */
    private boolean isTravelSuitablePlace(SearchKakaoPlacesFunction.KakaoPlace kakaoPlace) {
        String category = kakaoPlace.getCategory();
        String name = kakaoPlace.getName();
        
        if (category == null || name == null) {
            return false;
        }
        
        // 1. 여행에 적합한 카테고리만 허용 (화이트리스트 방식)
        String[] suitableCategories = {
            "음식점", "카페", "관광지", "문화시설", "쇼핑", "엔터테인먼트", 
            "숙박", "교통", "미용시설", "관광", "문화", "레저", "스포츠"
        };
        
        boolean hasSuitableCategory = false;
        for (String suitable : suitableCategories) {
            if (category.contains(suitable)) {
                hasSuitableCategory = true;
                break;
            }
        }
        
        if (!hasSuitableCategory) {
            return false;
        }
        
        // 2. 여행에 부적합한 키워드 패턴 필터링 (포괄적 방식)
        String[] unsuitablePatterns = {
            // 주거/생활시설
            "아파트", "빌라", "오피스텔", "주택", "원룸", "고시원",
            
            // 주차/교통시설
            "주차장", "주차", "공영주차장", "민영주차장", "지하주차장",
            
            // 상업시설 (편의점, 마트 등)
            "편의점", "마트", "슈퍼마켓", "대형마트", "백화점", "쇼핑센터", "쇼핑몰",
            
            // 의료시설
            "병원", "약국", "의료", "치과", "한의원", "정형외과", "내과", "외과",
            
            // 금융시설
            "은행", "ATM", "금융", "보험", "증권", "카드", "대출",
            
            // 부동산/중개업
            "부동산", "중개업소", "공인중개사", "부동산중개",
            
            // 교육시설
            "학원", "교습소", "학습", "과외", "학교", "대학교", "고등학교", "중학교", "초등학교", "캠퍼스",
            
            // 공공기관
            "공공기관", "관공서", "구청", "시청", "동사무소", "우체국", "파출소", "소방서",
            
            // 사무/업무시설
            "사무실", "오피스", "사무동", "회사", "기업", "본사", "지점", "지사", "사업소", "본부",
            "연구소", "연구센터", "기술센터", "개발센터", "e커머스", "사업본부",
            // 엔터테인먼트 회사/본사
            "엔터테인먼트", "매니지먼트", "기획사", "레이블", "레코드", "뮤직", "엔터",
            
            // 물류/창고시설
            "창고", "물류", "배송", "물류센터", "배송센터", "물류창고",
            
            // 자동차 관련
            "자동차", "정비", "주유소", "세차장", "정비소", "정비센터", "자동차정비", "세차소",
            
            // 관리/보안시설
            "관리", "관리소", "관리사무소", "부대시설", "시설관리", "보안실", "경비실",
            "출입구", "게이트", "게이트하우스",
            
            // 기타 부적합 시설
            "화장실", "개방화장실", "장례식장", "장례", "상가동", "공장"
        };
        
        // 카테고리 기반 필터링
        for (String pattern : unsuitablePatterns) {
            if (category.contains(pattern)) {
                return false;
            }
        }
        
        // 장소명 기반 필터링
        for (String pattern : unsuitablePatterns) {
            if (name.contains(pattern)) {
                return false;
            }
        }
        
        // 3. 특수 케이스 필터링 (복합 조건)
        if (isBusinessOrCorporate(name)) {
            return false;
        }
        
        if (isUtilityOrInfrastructure(name)) {
            return false;
        }
        
        if (isEducationalInstitution(name)) {
            return false;
        }
        
        if (isEntertainmentCompany(name)) {
            return false;
        }
        
        // 4. 여행 관련 예외 허용 (특별한 경우만)
        if (name.contains("문화") && (name.contains("갤러리") || name.contains("전시") || name.contains("박물관"))) {
            return true;
        }
        
        if (name.contains("관광") || name.contains("명소") || name.contains("랜드마크")) {
            return true;
        }
        
        // 5. 최종 검증: 카테고리가 "기타"인 경우 더 엄격한 검증
        if (category.contains("기타") || category.contains("기타시설")) {
            return isSuitableForTourism(name);
        }
        
        return true;
    }
    
    /**
     * 비즈니스/기업 관련 시설 여부 확인
     */
    private boolean isBusinessOrCorporate(String name) {
        String[] businessKeywords = {"본부", "사업본부", "지점", "지사", "사업소", "회사", "기업", "본사"};
        String[] corporateSuffixes = {"(주)", "주식회사", "유한회사", "합명회사", "합자회사", "재단법인", "사단법인"};
        
        for (String keyword : businessKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        
        for (String suffix : corporateSuffixes) {
            if (name.contains(suffix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 유틸리티/인프라 시설 여부 확인
     */
    private boolean isUtilityOrInfrastructure(String name) {
        String[] utilityKeywords = {"관리소", "시설관리", "부대시설", "물류", "배송", "창고", "정비", "주유", "세차"};
        
        for (String keyword : utilityKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 교육기관 여부 확인
     */
    private boolean isEducationalInstitution(String name) {
        String[] educationKeywords = {"대학교", "고등학교", "중학교", "초등학교", "캠퍼스", "학원", "교습소", "학습"};
        
        for (String keyword : educationKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 엔터테인먼트 회사 여부 확인
     */
    private boolean isEntertainmentCompany(String name) {
        String[] entertainmentKeywords = {
            "스타쉽", "안테나", "FNC", "모드하우스", "판타지오", "플필", "바로", "킨", "피네이션", 
            "에이치뮤직", "F&F", "매니지먼트숲", "디오디", "비에이치", "SM", "YG", "JYP", "빅히트",
            "큐브", "플레디스", "위에화", "스타쉽엔터테인먼트", "안테나뮤직", "FNC엔터테인먼트",
            "모드하우스엔터테인먼트", "판타지오엔터테인먼트", "바로엔터테인먼트", "킨엔터테인먼트",
            "피네이션엔터테인먼트", "에이치뮤직엔터테인먼트", "F&F엔터테인먼트", "디오디엔터테인먼트",
            "비에이치엔터테인먼트", "매니지먼트숲엔터테인먼트", "엔터테인먼트본사", "기획사", "레이블"
        };
        
        // 정확한 회사명 매칭
        for (String keyword : entertainmentKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        
        // 패턴 매칭: "OOO엔터테인먼트" 형태
        if (name.matches(".*엔터테인먼트.*") && 
            (name.contains("본사") || name.contains("사무실") || name.contains("건물") || name.contains("빌딩"))) {
            return true;
        }
        
        // 패턴 매칭: "OOO뮤직" 형태
        if (name.matches(".*뮤직.*") && 
            (name.contains("본사") || name.contains("사무실") || name.contains("건물") || name.contains("빌딩"))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 여행에 적합한지 최종 검증 (기타 카테고리용)
     */
    private boolean isSuitableForTourism(String name) {
        String[] tourismKeywords = {"관광", "명소", "랜드마크", "문화", "갤러리", "전시", "박물관", "공원", "해변", "산", "강", "호수"};
        
        for (String keyword : tourismKeywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 카테고리별 기본 평점 설정 (Kakao API는 평점 정보를 제공하지 않음)
     */
    private Double getDefaultRatingByCategory(String category) {
        return switch (category) {
            case "음식점" -> 4.2; // 음식점은 보통 높은 평점
            case "카페" -> 4.0;   // 카페는 중간 평점
            case "관광지" -> 4.3; // 관광지는 높은 평점
            case "쇼핑" -> 4.1;   // 쇼핑은 중간 높은 평점
            case "문화시설" -> 4.4; // 문화시설은 높은 평점
            case "엔터테인먼트" -> 4.0; // 엔터테인먼트는 중간 평점
            case "교통" -> 3.8;   // 교통은 중간 낮은 평점
            case "숙박" -> 4.2;   // 숙박은 높은 평점
            case "교육시설" -> 4.1; // 교육시설은 중간 높은 평점
            case "의료시설" -> 3.9; // 의료시설은 중간 평점
            case "미용시설" -> 4.1; // 미용시설은 중간 높은 평점
            default -> 4.0;       // 기본값
        };
    }
    
    /**
     * 시간블록별 추천 시간 설정
     */
    private String getRecommendTimeByTimeBlock(String timeBlock) {
        return switch (timeBlock) {
            case "BREAKFAST" -> "08:00-10:00";
            case "MORNING_ACTIVITY" -> "10:00-12:00";
            case "LUNCH" -> "12:00-14:00";
            case "CAFE" -> "14:00-16:00";
            case "AFTERNOON_ACTIVITY" -> "16:00-18:00";
            case "DINNER" -> "18:00-20:00";
            case "EVENING_ACTIVITY" -> "20:00-22:00";
            default -> "시간 정보 없음";
        };
    }
    
    /**
     * Kakao Places 검색 요청 DTO
     */
    public static class KakaoSearchRequest {
        private String clusterName;
        private int size;
        
        public String getClusterName() { return clusterName; }
        public void setClusterName(String clusterName) { this.clusterName = clusterName; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        @Override
        public String toString() {
            return "KakaoSearchRequest{" +
                "clusterName='" + clusterName + '\'' +
                ", size=" + size +
                '}';
        }
    }
}
