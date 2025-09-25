package com.compass.domain.chat.route_optimization.config;

import com.compass.domain.chat.orchestrator.ContextManager;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.service.EnhancedPerplexityClient;
import com.compass.domain.chat.service.RegionalTravelPlaceCollector;
import com.compass.domain.chat.route_optimization.client.KakaoMobilityClient;
import com.compass.domain.chat.route_optimization.repository.TravelItineraryRepository;
import com.compass.domain.chat.route_optimization.repository.TravelPlaceCandidateRepository;
import com.compass.domain.chat.route_optimization.repository.TravelPlaceRepository;
import com.compass.domain.chat.route_optimization.service.ItineraryPersistenceService;
import com.compass.domain.chat.route_optimization.service.MultiPathOptimizationService;
import com.compass.domain.chat.route_optimization.service.RouteOptimizationService;
import com.compass.domain.chat.route_optimization.service.RouteOptimizationOrchestrationService;
import com.compass.domain.chat.route_optimization.strategy.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class RouteOptimizationTestConfig {

    @MockBean
    private ChatThreadRepository chatThreadRepository;

    @MockBean
    private TravelItineraryRepository itineraryRepository;

    @MockBean
    private TravelPlaceRepository placeRepository;

    @MockBean
    private TravelPlaceCandidateRepository candidateRepository;

    @MockBean
    private ContextManager contextManager;

    @MockBean
    private RestTemplate restTemplate;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public EnhancedPerplexityClient enhancedPerplexityClient() {
        return new EnhancedPerplexityClient();
    }

    @Bean
    public RegionalTravelPlaceCollector regionalTravelPlaceCollector(
        EnhancedPerplexityClient perplexityClient,
        ObjectMapper objectMapper
    ) {
        return new RegionalTravelPlaceCollector(perplexityClient, objectMapper);
    }

    @Bean
    public KakaoMobilityClient kakaoMobilityClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        return new KakaoMobilityClient(restTemplate, objectMapper);
    }

    @Bean
    public RouteOptimizationService routeOptimizationService(
        KakaoMobilityClient kakaoMobilityClient
    ) {
        return new RouteOptimizationService(kakaoMobilityClient);
    }

    @Bean
    public DistanceOptimizationStrategy distanceOptimizationStrategy() {
        return new DistanceOptimizationStrategy();
    }

    @Bean
    public TimeOptimizationStrategy timeOptimizationStrategy() {
        return new TimeOptimizationStrategy();
    }

    @Bean
    public BalancedOptimizationStrategy balancedOptimizationStrategy() {
        return new BalancedOptimizationStrategy();
    }

    @Bean
    public OptimizationStrategyFactory optimizationStrategyFactory(
        DistanceOptimizationStrategy distanceStrategy,
        TimeOptimizationStrategy timeStrategy,
        BalancedOptimizationStrategy balancedStrategy
    ) {
        return new OptimizationStrategyFactory(
            distanceStrategy,
            timeStrategy,
            balancedStrategy
        );
    }

    @Bean
    public ItineraryPersistenceService itineraryPersistenceService(
        TravelItineraryRepository itineraryRepository,
        TravelPlaceRepository placeRepository,
        TravelPlaceCandidateRepository candidateRepository,
        ChatThreadRepository threadRepository
    ) {
        return new ItineraryPersistenceService(
            itineraryRepository,
            placeRepository,
            candidateRepository,
            threadRepository
        );
    }

    @Bean
    public MultiPathOptimizationService multiPathOptimizationService(
        KakaoMobilityClient kakaoMobilityClient
    ) {
        return new MultiPathOptimizationService(kakaoMobilityClient);
    }

    @Bean
    public RouteOptimizationOrchestrationService routeOptimizationOrchestrationService(
        ChatThreadRepository threadRepository,
        ObjectMapper objectMapper,
        ContextManager contextManager,
        ItineraryPersistenceService persistenceService,
        OptimizationStrategyFactory strategyFactory,
        MultiPathOptimizationService multiPathOptimizationService,
        KakaoMobilityClient kakaoMobilityClient
    ) {
        return new RouteOptimizationOrchestrationService(
            threadRepository,
            objectMapper,
            contextManager,
            persistenceService,
            strategyFactory,
            multiPathOptimizationService,
            kakaoMobilityClient
        );
    }
}