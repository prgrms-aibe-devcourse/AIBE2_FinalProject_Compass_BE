package com.compass.domain.trip.service;

import com.compass.domain.trip.client.TourApiClient;
import com.compass.domain.trip.config.TourApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tour API 서비스 테스트
 * REQ-CRAWL-001: Tour API 클라이언트 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class TourApiServiceTest {
    
    @Mock
    private TourApiClient tourApiClient;
    
    @Mock
    private TourApiProperties properties;
    
    private TourApiService tourApiService;
    
    @BeforeEach
    void setUp() {
        tourApiService = new TourApiService(tourApiClient, properties);
    }
    
    @Test
    @DisplayName("Seoul JSON 카테고리를 Tour API ContentTypeId로 매핑")
    void testMapCategoryToContentTypeId() {
        // Given & When & Then
        assertEquals("12", tourApiService.mapCategoryToContentTypeId("Palace"));
        assertEquals("12", tourApiService.mapCategoryToContentTypeId("Historic Gate"));
        assertEquals("12", tourApiService.mapCategoryToContentTypeId("UNESCO Site"));
        
        assertEquals("14", tourApiService.mapCategoryToContentTypeId("Museum"));
        assertEquals("14", tourApiService.mapCategoryToContentTypeId("Theater"));
        assertEquals("14", tourApiService.mapCategoryToContentTypeId("Arts Complex"));
        
        assertEquals("38", tourApiService.mapCategoryToContentTypeId("Shopping Mall"));
        assertEquals("38", tourApiService.mapCategoryToContentTypeId("Traditional Market"));
        assertEquals("38", tourApiService.mapCategoryToContentTypeId("Market"));
        
        assertEquals("39", tourApiService.mapCategoryToContentTypeId("Food Alley"));
        assertEquals("39", tourApiService.mapCategoryToContentTypeId("Food Street"));
        
        assertEquals("28", tourApiService.mapCategoryToContentTypeId("Sports Venue"));
        assertEquals("28", tourApiService.mapCategoryToContentTypeId("Theme Park"));
        assertEquals("28", tourApiService.mapCategoryToContentTypeId("Attraction"));
        
        // 기본값 테스트
        assertEquals("12", tourApiService.mapCategoryToContentTypeId("Unknown Category"));
        assertEquals("12", tourApiService.mapCategoryToContentTypeId(""));
    }
    
    @Test
    @DisplayName("Seoul JSON 카테고리 대소문자 구분 없이 매핑")
    void testMapCategoryToContentTypeIdCaseInsensitive() {
        // Given & When & Then
        assertEquals("12", tourApiService.mapCategoryToContentTypeId("palace"));
        assertEquals("12", tourApiService.mapCategoryToContentTypeId("PALACE"));
        assertEquals("12", tourApiService.mapCategoryToContentTypeId("Palace"));
        
        assertEquals("14", tourApiService.mapCategoryToContentTypeId("museum"));
        assertEquals("14", tourApiService.mapCategoryToContentTypeId("MUSEUM"));
        assertEquals("14", tourApiService.mapCategoryToContentTypeId("Museum"));
        
        assertEquals("38", tourApiService.mapCategoryToContentTypeId("shopping mall"));
        assertEquals("38", tourApiService.mapCategoryToContentTypeId("SHOPPING MALL"));
        assertEquals("38", tourApiService.mapCategoryToContentTypeId("Shopping Mall"));
    }
}
