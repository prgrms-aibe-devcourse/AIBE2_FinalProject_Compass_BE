package com.compass.domain.chat.service;

import com.compass.domain.chat.model.Cluster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 클러스터 관리 서비스
 * 개선안에 따른 사전 정의된 클러스터 제공
 */
@Slf4j
@Service
public class ClusterService {
    
    private final Map<String, Cluster> predefinedClusters;
    
    public ClusterService() {
        this.predefinedClusters = initializeClusters();
    }
    
    /**
     * 사전 정의된 클러스터 초기화
     * 개선안의 서울 클러스터 정의 반영
     */
    private Map<String, Cluster> initializeClusters() {
        Map<String, Cluster> clusters = new HashMap<>();
        
        // 홍대 클러스터
        clusters.put("hongdae", Cluster.builder()
            .name("hongdae")
            .displayName("홍대")
            .centerLat(37.5563)
            .centerLng(126.9234)
            .radius(2000)
            .styles(Arrays.asList("젊은", "활동적", "문화", "예술"))
            .ageGroup("20-30대")
            .budget("중간")
            .characteristics(Arrays.asList("인디문화", "거리공연", "클럽", "카페"))
            .description("젊은 문화, 카페, 인디 문화")
            .build());
        
        // 강남 클러스터
        clusters.put("gangnam", Cluster.builder()
            .name("gangnam")
            .displayName("강남")
            .centerLat(37.5172)
            .centerLng(127.0473)
            .radius(2000)
            .styles(Arrays.asList("럭셔리", "쇼핑", "비즈니스"))
            .ageGroup("30-40대")
            .budget("높음")
            .characteristics(Arrays.asList("백화점", "고급레스토랑", "엔터테인먼트"))
            .description("쇼핑, 럭셔리, 비즈니스")
            .build());
        
        // 성수 클러스터
        clusters.put("sungsu", Cluster.builder()
            .name("sungsu")
            .displayName("성수")
            .centerLat(37.5446)
            .centerLng(127.0559)
            .radius(2000)
            .styles(Arrays.asList("트렌디", "힙", "크리에이티브"))
            .ageGroup("20-30대")
            .budget("중간")
            .characteristics(Arrays.asList("카페", "팝업스토어", "갤러리", "공방"))
            .description("트렌디, 카페, 팝업스토어")
            .build());
        
        // 종로 클러스터
        clusters.put("jongno", Cluster.builder()
            .name("jongno")
            .displayName("종로")
            .centerLat(37.5735)
            .centerLng(126.9788)
            .radius(2000)
            .styles(Arrays.asList("전통", "역사", "문화"))
            .ageGroup("40-50대")
            .budget("중간")
            .characteristics(Arrays.asList("궁궐", "박물관", "전통시장", "한옥"))
            .description("전통, 역사, 문화유산")
            .build());
        
        // 이태원 클러스터
        clusters.put("itaewon", Cluster.builder()
            .name("itaewon")
            .displayName("이태원")
            .centerLat(37.5347)
            .centerLng(126.9947)
            .radius(2000)
            .styles(Arrays.asList("국제적", "다양성", "나이트라이프"))
            .ageGroup("20-40대")
            .budget("중간")
            .characteristics(Arrays.asList("다국적음식", "클럽", "바", "쇼핑"))
            .description("국제적, 다양성, 나이트라이프")
            .build());
        
        // 북촌/삼청동 클러스터
        clusters.put("bukchon", Cluster.builder()
            .name("bukchon")
            .displayName("북촌/삼청동")
            .centerLat(37.5838)
            .centerLng(126.9822)
            .radius(2000)
            .styles(Arrays.asList("전통", "예술", "힐링"))
            .ageGroup("30-50대")
            .budget("중간")
            .characteristics(Arrays.asList("한옥", "갤러리", "카페", "공방"))
            .description("한옥, 전통, 갤러리")
            .build());
        
        log.info("사전 정의된 클러스터 {}개 초기화 완료", clusters.size());
        return clusters;
    }
    
    /**
     * 모든 클러스터 조회
     */
    public Map<String, Cluster> getAllClusters() {
        return new HashMap<>(predefinedClusters);
    }
    
    /**
     * 특정 클러스터 조회
     */
    public Cluster getCluster(String clusterName) {
        return predefinedClusters.get(clusterName);
    }
    
    /**
     * 여행 스타일에 따른 클러스터 매칭 점수 계산
     * 개선안의 스타일 기반 매칭 알고리즘
     */
    public Map<String, Double> calculateClusterScores(Map<String, Object> travelStyle) {
        Map<String, Double> scores = new HashMap<>();
        
        for (Map.Entry<String, Cluster> entry : predefinedClusters.entrySet()) {
            String clusterName = entry.getKey();
            Cluster cluster = entry.getValue();
            double score = cluster.calculateMatchScore(travelStyle);
            scores.put(clusterName, score);
        }
        
        // 점수 순으로 정렬
        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }
    
    /**
     * 클러스터별 장소 수집 비율 계산
     * 개선안의 클러스터 분배 알고리즘
     */
    public Map<String, Integer> calculatePlaceDistribution(Map<String, Double> clusterScores, int totalPlaces) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (Map.Entry<String, Double> entry : clusterScores.entrySet()) {
            String clusterName = entry.getKey();
            double score = entry.getValue();
            Cluster cluster = predefinedClusters.get(clusterName);
            
            int placeCount = cluster.calculatePlaceCount(totalPlaces, score);
            distribution.put(clusterName, placeCount);
        }
        
        return distribution;
    }
}