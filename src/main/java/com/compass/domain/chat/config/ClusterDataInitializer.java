package com.compass.domain.chat.config;

import com.compass.domain.chat.entity.PredefinedCluster;
import com.compass.domain.chat.repository.PredefinedClusterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClusterDataInitializer implements CommandLineRunner {
    
    private final PredefinedClusterRepository clusterRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initializeSeoulClusters();
    }
    
    private void initializeSeoulClusters() {
        if (clusterRepository.count() > 0) {
            log.info("클러스터 데이터가 이미 존재합니다.");
            return;
        }
        
        List<PredefinedCluster> seoulClusters = Arrays.asList(
            // 홍대 클러스터
            PredefinedCluster.builder()
                .clusterName("hongdae")
                .displayName("홍대")
                .city("서울")
                .centerLatitude(37.5563)
                .centerLongitude(126.9234)
                .radiusMeters(2000)
                .styles("[\"젊은\", \"활동적\", \"문화\", \"예술\"]")
                .ageGroup("20-30대")
                .budgetLevel("중간")
                .characteristics("[\"인디문화\", \"거리공연\", \"클럽\", \"카페\"]")
                .isActive(true)
                .priority(5)
                .build(),
            
            // 강남 클러스터
            PredefinedCluster.builder()
                .clusterName("gangnam")
                .displayName("강남")
                .city("서울")
                .centerLatitude(37.5172)
                .centerLongitude(127.0473)
                .radiusMeters(3000)
                .styles("[\"럭셔리\", \"쇼핑\", \"비즈니스\"]")
                .ageGroup("30-40대")
                .budgetLevel("높음")
                .characteristics("[\"백화점\", \"고급레스토랑\", \"엔터테인먼트\"]")
                .isActive(true)
                .priority(4)
                .build(),
            
            // 성수 클러스터
            PredefinedCluster.builder()
                .clusterName("sungsu")
                .displayName("성수")
                .city("서울")
                .centerLatitude(37.5446)
                .centerLongitude(127.0559)
                .radiusMeters(2000)
                .styles("[\"트렌디\", \"창작\", \"힙스터\"]")
                .ageGroup("20-30대")
                .budgetLevel("중간")
                .characteristics("[\"카페\", \"팝업스토어\", \"갤러리\", \"브런치\"]")
                .isActive(true)
                .priority(3)
                .build(),
            
            // 종로 클러스터
            PredefinedCluster.builder()
                .clusterName("jongno")
                .displayName("종로")
                .city("서울")
                .centerLatitude(37.5735)
                .centerLongitude(126.9788)
                .radiusMeters(2500)
                .styles("[\"전통\", \"역사\", \"문화\"]")
                .ageGroup("전체")
                .budgetLevel("낮음")
                .characteristics("[\"궁궐\", \"한옥\", \"전통시장\", \"박물관\"]")
                .isActive(true)
                .priority(2)
                .build(),
            
            // 이태원 클러스터
            PredefinedCluster.builder()
                .clusterName("itaewon")
                .displayName("이태원")
                .city("서울")
                .centerLatitude(37.5347)
                .centerLongitude(126.9947)
                .radiusMeters(1500)
                .styles("[\"국제적\", \"다양성\", \"나이트라이프\"]")
                .ageGroup("20-40대")
                .budgetLevel("중간")
                .characteristics("[\"다국적음식\", \"바\", \"클럽\", \"쇼핑\"]")
                .isActive(true)
                .priority(1)
                .build()
        );
        
        clusterRepository.saveAll(seoulClusters);
        log.info("서울 클러스터 {}개 초기화 완료", seoulClusters.size());
    }
}



