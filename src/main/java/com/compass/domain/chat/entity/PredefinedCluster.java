package com.compass.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "predefined_clusters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredefinedCluster {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "cluster_name", nullable = false, unique = true)
    private String clusterName;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;
    
    @Column(name = "city", nullable = false)
    private String city;
    
    @Column(name = "center_latitude")
    private Double centerLatitude;
    
    @Column(name = "center_longitude")
    private Double centerLongitude;
    
    @Column(name = "radius_meters")
    private Integer radiusMeters;
    
    @Column(name = "styles", columnDefinition = "TEXT")
    private String styles; // JSON 형태로 저장: ["젊은", "활동적", "문화"]
    
    @Column(name = "age_group")
    private String ageGroup; // "20-30대", "30-40대"
    
    @Column(name = "budget_level")
    private String budgetLevel; // "낮음", "중간", "높음"
    
    @Column(name = "characteristics", columnDefinition = "TEXT")
    private String characteristics; // JSON 형태로 저장: ["인디문화", "거리공연", "클럽"]
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @Column(name = "priority")
    private Integer priority; // 우선순위 (높을수록 우선)
}


