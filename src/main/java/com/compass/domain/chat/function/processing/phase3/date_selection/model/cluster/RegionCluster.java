package com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// 지역별 클러스터 (예: 홍대 지역, 강남 지역 등)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionCluster {
    private String regionName;       // 지역명
    private Point center;            // 지역 중심점
    private List<TourPlace> places;  // 해당 지역의 장소들
    private double averageRating;    // 평균 평점
    private int placeCount;          // 장소 개수

    // 지역 크기 계산 (장소 개수 기준)
    public int getSize() {
        return places != null ? places.size() : 0;
    }
}