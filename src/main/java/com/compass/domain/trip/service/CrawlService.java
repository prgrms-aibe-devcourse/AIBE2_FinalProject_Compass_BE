package com.compass.domain.trip.service;

import com.compass.domain.trip.client.TourApiClient;
import com.compass.domain.trip.dto.TourApiResponse;
import com.compass.domain.trip.entity.CrawlStatus;
import com.compass.domain.trip.entity.TourPlace;
import com.compass.domain.trip.enums.CrawlStatusType;
import com.compass.domain.trip.repository.CrawlStatusRepository;
import com.compass.domain.trip.repository.TourPlaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 크롤링 서비스
 * REQ-CRAWL-002: Phase별 크롤링 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CrawlService {

    private final TourApiClient tourApiClient;
    private final TourPlaceRepository tourPlaceRepository;
    private final CrawlStatusRepository crawlStatusRepository;
    private final ObjectMapper objectMapper;

    // 지역 코드 매핑
    private static final Map<String, String> AREA_CODE_MAP = Map.of(
        "1", "서울",
        "6", "부산", 
        "39", "제주"
    );

    // 컨텐츠 타입 매핑
    private static final Map<String, String> CONTENT_TYPE_MAP = Map.of(
        "12", "관광지",
        "14", "문화시설",
        "39", "음식점",
        "38", "쇼핑",
        "28", "레포츠",
        "32", "숙박"
    );

    // 컨텐츠 타입별 예상 수집 개수
    private static final Map<String, Integer> EXPECTED_COUNT_MAP = Map.of(
        "12", 500,  // 관광지
        "14", 300,  // 문화시설
        "39", 300,  // 음식점
        "38", 200,  // 쇼핑
        "28", 200,  // 레포츠
        "32", 100   // 숙박
    );

    /**
     * 전체 지역 크롤링 시작
     */
    public CompletableFuture<Void> startFullCrawling() {
        log.info("전체 지역 크롤링 시작");
        
        List<String> areaCodes = Arrays.asList("1", "6", "39");
        List<String> contentTypeIds = Arrays.asList("12", "14", "39", "38", "28", "32");
        
        return startCrawling(areaCodes, contentTypeIds);
    }

    /**
     * 특정 지역 크롤링 시작
     */
    public CompletableFuture<Void> startCrawlingByArea(String areaCode) {
        log.info("지역 {} 크롤링 시작", AREA_CODE_MAP.get(areaCode));
        
        List<String> contentTypeIds = Arrays.asList("12", "14", "39", "38", "28", "32");
        
        return startCrawling(Arrays.asList(areaCode), contentTypeIds);
    }

    /**
     * 크롤링 시작
     */
    public CompletableFuture<Void> startCrawling(List<String> areaCodes, List<String> contentTypeIds) {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String areaCode : areaCodes) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    crawlArea(areaCode, contentTypeIds);
                } catch (Exception e) {
                    log.error("지역 {} 크롤링 실패", AREA_CODE_MAP.get(areaCode), e);
                }
            }, executor);
            
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, throwable) -> {
                    executor.shutdown();
                    if (throwable != null) {
                        log.error("크롤링 중 오류 발생", throwable);
                    } else {
                        log.info("전체 크롤링 완료");
                    }
                });
    }

    /**
     * 특정 지역 크롤링
     */
    private void crawlArea(String areaCode, List<String> contentTypeIds) {
        String areaName = AREA_CODE_MAP.get(areaCode);
        log.info("지역 {} 크롤링 시작", areaName);
        
        for (String contentTypeId : contentTypeIds) {
            try {
                crawlAreaContentType(areaCode, areaName, contentTypeId);
                Thread.sleep(100); // Rate Limiting
            } catch (Exception e) {
                log.error("지역 {} 컨텐츠 타입 {} 크롤링 실패", areaName, CONTENT_TYPE_MAP.get(contentTypeId), e);
            }
        }
        
        log.info("지역 {} 크롤링 완료", areaName);
    }

    /**
     * 특정 지역의 특정 컨텐츠 타입 크롤링
     */
    private void crawlAreaContentType(String areaCode, String areaName, String contentTypeId) {
        String contentTypeName = CONTENT_TYPE_MAP.get(contentTypeId);
        log.info("지역 {} 컨텐츠 타입 {} 크롤링 시작", areaName, contentTypeName);
        
        // 크롤링 상태 생성
        CrawlStatus crawlStatus = CrawlStatus.builder()
                .areaCode(areaCode)
                .areaName(areaName)
                .contentTypeId(contentTypeId)
                .contentTypeName(contentTypeName)
                .status(CrawlStatusType.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .expectedCount(EXPECTED_COUNT_MAP.get(contentTypeId))
                .collectedCount(0)
                .currentPage(1)
                .build();
        
        crawlStatus = crawlStatusRepository.save(crawlStatus);
        
        try {
            int totalCollected = 0;
            int pageNo = 1;
            int numOfRows = 100;
            boolean hasMoreData = true;
            
            while (hasMoreData) {
                // API 호출
                var response = tourApiClient.getAreaBasedList(areaCode, contentTypeId, pageNo, numOfRows);
                
                if (response.isEmpty()) {
                    log.warn("지역 {} 컨텐츠 타입 {} 페이지 {} 데이터 없음", areaName, contentTypeName, pageNo);
                    break;
                }
                
                TourApiResponse apiResponse = response.get();
                List<TourApiResponse.TourItem> items = apiResponse.getResponse().getBody().getItems().getItem();
                
                if (items == null || items.isEmpty()) {
                    log.info("지역 {} 컨텐츠 타입 {} 페이지 {} 데이터 없음", areaName, contentTypeName, pageNo);
                    break;
                }
                
                // 데이터 변환 및 저장
                List<TourPlace> tourPlaces = convertToTourPlaces(items, areaCode, contentTypeId);
                List<TourPlace> savedPlaces = saveTourPlaces(tourPlaces);
                
                totalCollected += savedPlaces.size();
                
                // 크롤링 상태 업데이트
                updateCrawlStatus(crawlStatus, pageNo, totalCollected, null);
                
                log.info("지역 {} 컨텐츠 타입 {} 페이지 {} 완료 - 수집: {}개, 누적: {}개", 
                        areaName, contentTypeName, pageNo, savedPlaces.size(), totalCollected);
                
                // 다음 페이지 확인
                if (items.size() < numOfRows) {
                    hasMoreData = false;
                } else {
                    pageNo++;
                    Thread.sleep(100); // Rate Limiting
                }
            }
            
            // 크롤링 완료
            completeCrawlStatus(crawlStatus, totalCollected);
            log.info("지역 {} 컨텐츠 타입 {} 크롤링 완료 - 총 수집: {}개", areaName, contentTypeName, totalCollected);
            
        } catch (Exception e) {
            // 크롤링 실패
            failCrawlStatus(crawlStatus, e.getMessage());
            log.error("지역 {} 컨텐츠 타입 {} 크롤링 실패", areaName, contentTypeName, e);
        }
    }

    /**
     * Tour API 응답을 TourPlace 엔티티로 변환
     */
    private List<TourPlace> convertToTourPlaces(List<TourApiResponse.TourItem> items, String areaCode, String contentTypeId) {
        return items.stream()
                .map(item -> convertToTourPlace(item, areaCode, contentTypeId))
                .collect(Collectors.toList());
    }

    /**
     * Tour API 응답 아이템을 TourPlace 엔티티로 변환
     */
    private TourPlace convertToTourPlace(TourApiResponse.TourItem item, String areaCode, String contentTypeId) {
        // 키워드 JSON 변환 (Tour API에는 tags가 없으므로 빈 배열로 설정)
        JsonNode keywords = createKeywordsJson(null);
        
        // 상세 정보 JSON 생성
        JsonNode details = createDetailsJson(item);
        
        return TourPlace.builder()
                .contentId(item.getContentId())
                .name(item.getTitle())
                .category(mapContentTypeToCategory(contentTypeId))
                .district(item.getSigunguCode())
                .area(item.getAddr1())
                .latitude(parseDouble(item.getMapY()))
                .longitude(parseDouble(item.getMapX()))
                .keywords(keywords)
                .areaCode(areaCode)
                .contentTypeId(contentTypeId)
                .address(item.getAddr1())
                .phoneNumber(item.getTel())
                .homepageUrl(item.getHomepage())
                .imageUrl(item.getFirstImage())
                .overview(item.getOverview())
                .dataSource("tour_api")
                .crawledAt(LocalDateTime.now())
                .details(details)
                .build();
    }

    /**
     * 키워드 JSON 생성
     */
    private JsonNode createKeywordsJson(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return objectMapper.createArrayNode();
        }
        
        String[] tagArray = tags.split(",");
        return objectMapper.valueToTree(Arrays.stream(tagArray)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList()));
    }

    /**
     * 상세 정보 JSON 생성
     */
    private JsonNode createDetailsJson(TourApiResponse.TourItem item) {
        Map<String, Object> details = new HashMap<>();
        details.put("tel", item.getTel());
        details.put("homepage", item.getHomepage());
        details.put("firstImage", item.getFirstImage());
        details.put("firstImage2", item.getFirstImage2());
        details.put("overview", item.getOverview());
        details.put("zipcode", item.getZipcode());
        details.put("sigunguCode", item.getSigunguCode());
        details.put("sigunguName", item.getSigunguName());
        details.put("areacode", item.getAreaCode());
        details.put("areaname", item.getAreaname());
        
        return objectMapper.valueToTree(details);
    }

    /**
     * 컨텐츠 타입을 카테고리로 매핑
     */
    private String mapContentTypeToCategory(String contentTypeId) {
        return CONTENT_TYPE_MAP.getOrDefault(contentTypeId, "기타");
    }

    /**
     * 문자열을 Double로 변환
     */
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * TourPlace 저장 (중복 제거)
     */
    private List<TourPlace> saveTourPlaces(List<TourPlace> tourPlaces) {
        List<TourPlace> savedPlaces = new ArrayList<>();
        
        for (TourPlace tourPlace : tourPlaces) {
            // 중복 확인
            Optional<TourPlace> existing = tourPlaceRepository.findByContentId(tourPlace.getContentId());
            
            if (existing.isPresent()) {
                // 기존 데이터 업데이트
                TourPlace existingPlace = existing.get();
                updateExistingTourPlace(existingPlace, tourPlace);
                tourPlaceRepository.save(existingPlace);
                savedPlaces.add(existingPlace);
            } else {
                // 새 데이터 저장
                tourPlaceRepository.save(tourPlace);
                savedPlaces.add(tourPlace);
            }
        }
        
        return savedPlaces;
    }

    /**
     * 기존 TourPlace 업데이트
     */
    private void updateExistingTourPlace(TourPlace existing, TourPlace newData) {
        existing.setName(newData.getName());
        existing.setCategory(newData.getCategory());
        existing.setDistrict(newData.getDistrict());
        existing.setArea(newData.getArea());
        existing.setLatitude(newData.getLatitude());
        existing.setLongitude(newData.getLongitude());
        existing.setKeywords(newData.getKeywords());
        existing.setAddress(newData.getAddress());
        existing.setPhoneNumber(newData.getPhoneNumber());
        existing.setHomepageUrl(newData.getHomepageUrl());
        existing.setImageUrl(newData.getImageUrl());
        existing.setOverview(newData.getOverview());
        existing.setDetails(newData.getDetails());
        existing.setCrawledAt(LocalDateTime.now());
    }

    /**
     * 크롤링 상태 업데이트
     */
    private void updateCrawlStatus(CrawlStatus crawlStatus, int currentPage, int collectedCount, String errorMessage) {
        crawlStatus.setCurrentPage(currentPage);
        crawlStatus.setCollectedCount(collectedCount);
        crawlStatus.setErrorMessage(errorMessage);
        
        // 진행률 계산
        if (crawlStatus.getExpectedCount() != null && crawlStatus.getExpectedCount() > 0) {
            int progressPercentage = Math.min(100, (collectedCount * 100) / crawlStatus.getExpectedCount());
            crawlStatus.setProgressPercentage(progressPercentage);
        }
        
        crawlStatusRepository.save(crawlStatus);
    }

    /**
     * 크롤링 완료 처리
     */
    private void completeCrawlStatus(CrawlStatus crawlStatus, int totalCollected) {
        crawlStatus.setStatus(CrawlStatusType.COMPLETED);
        crawlStatus.setCollectedCount(totalCollected);
        crawlStatus.setCompletedAt(LocalDateTime.now());
        crawlStatus.setProgressPercentage(100);
        
        // 소요 시간 계산
        if (crawlStatus.getStartedAt() != null) {
            long durationSeconds = java.time.Duration.between(crawlStatus.getStartedAt(), LocalDateTime.now()).getSeconds();
            crawlStatus.setDurationSeconds(durationSeconds);
        }
        
        crawlStatusRepository.save(crawlStatus);
    }

    /**
     * 크롤링 실패 처리
     */
    private void failCrawlStatus(CrawlStatus crawlStatus, String errorMessage) {
        crawlStatus.setStatus(CrawlStatusType.FAILED);
        crawlStatus.setErrorMessage(errorMessage);
        crawlStatus.setCompletedAt(LocalDateTime.now());
        
        // 소요 시간 계산
        if (crawlStatus.getStartedAt() != null) {
            long durationSeconds = java.time.Duration.between(crawlStatus.getStartedAt(), LocalDateTime.now()).getSeconds();
            crawlStatus.setDurationSeconds(durationSeconds);
        }
        
        crawlStatusRepository.save(crawlStatus);
    }
}
