package com.compass.domain.chat.function.processing.phase3.stage2.model.cluster;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// K-Means 클러스터링용 지리적 포인트
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point {
    private double lat;  // 위도
    private double lng;  // 경도
}