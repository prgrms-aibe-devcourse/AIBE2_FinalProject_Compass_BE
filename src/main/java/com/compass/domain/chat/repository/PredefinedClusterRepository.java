package com.compass.domain.chat.repository;

import com.compass.domain.chat.entity.PredefinedCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredefinedClusterRepository extends JpaRepository<PredefinedCluster, Long> {
    
    List<PredefinedCluster> findByCityAndIsActiveTrueOrderByPriorityDesc(String city);
    
    @Query("SELECT pc FROM PredefinedCluster pc WHERE pc.city = :city AND pc.isActive = true ORDER BY pc.priority DESC")
    List<PredefinedCluster> findActiveClustersByCity(@Param("city") String city);
    
    PredefinedCluster findByClusterNameAndIsActiveTrue(String clusterName);
}



