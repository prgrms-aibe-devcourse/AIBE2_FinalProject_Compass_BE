package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.TravelCandidate;

import java.util.List;

public interface TravelCandidateQueryRepository {

    List<TravelCandidate> findTopActiveByRegionAndCategoryKeywords(String region,
                                                                   List<String> categoryKeywords,
                                                                   int limit);

    List<TravelCandidate> findActiveByRegionsAndTimeBlock(List<String> regions,
                                                          TravelCandidate.TimeBlock timeBlock,
                                                          int limit);
}
