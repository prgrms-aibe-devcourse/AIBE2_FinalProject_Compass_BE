package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.PredefinedCluster;
import com.compass.domain.chat.repository.PredefinedClusterRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredefinedClusterService {
    
    private final PredefinedClusterRepository clusterRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 도시별 활성 클러스터 조회
     */
    public List<PredefinedCluster> getActiveClustersByCity(String city) {
        return clusterRepository.findActiveClustersByCity(city);
    }
    
    /**
     * 클러스터명으로 클러스터 조회
     */
    public PredefinedCluster getClusterByName(String clusterName) {
        return clusterRepository.findByClusterNameAndIsActiveTrue(clusterName);
    }
    
    /**
     * 여행 스타일과 클러스터 매칭 점수 계산
     */
    public Map<String, Double> calculateStyleMatchScores(String travelStyle, String city) {
        List<PredefinedCluster> clusters = getActiveClustersByCity(city);
        
        return clusters.stream()
            .collect(java.util.stream.Collectors.toMap(
                PredefinedCluster::getClusterName,
                cluster -> calculateMatchScore(travelStyle, cluster)
            ));
    }
    
    /**
     * 개별 클러스터와 여행 스타일 매칭 점수 계산
     */
    private Double calculateMatchScore(String travelStyle, PredefinedCluster cluster) {
        try {
            // 스타일 키워드 추출
            List<String> clusterStyles = objectMapper.readValue(
                cluster.getStyles(), 
                new TypeReference<List<String>>() {}
            );
            
            // 여행 스타일에서 키워드 매칭
            double matchCount = 0;
            for (String style : clusterStyles) {
                if (travelStyle.toLowerCase().contains(style.toLowerCase())) {
                    matchCount++;
                }
            }
            
            // 매칭 점수 계산 (0.0 ~ 1.0)
            return clusterStyles.isEmpty() ? 0.0 : matchCount / clusterStyles.size();
            
        } catch (Exception e) {
            log.error("스타일 매칭 점수 계산 실패: {}", e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 클러스터별 장소 분배 비율 계산
     */
    public Map<String, Integer> calculatePlaceDistribution(Map<String, Double> matchScores, int totalPlaces) {
        // 매칭 점수에 따른 가중치 계산
        double totalWeight = matchScores.values().stream().mapToDouble(Double::doubleValue).sum();
        
        return matchScores.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    double ratio = entry.getValue() / totalWeight;
                    return (int) Math.round(totalPlaces * ratio);
                }
            ));
    }
}


