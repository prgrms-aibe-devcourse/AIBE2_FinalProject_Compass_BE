package com.compass.domain.chat.function.processing.phase3.date_selection.model.cluster;

import com.compass.domain.chat.function.processing.phase3.date_selection.model.TourPlace;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

// K-Means 클러스터
@Data
public class Cluster {
    private Point center;  // 클러스터 중심점
    private List<TourPlace> places;  // 클러스터에 속한 장소들

    public Cluster(Point center) {
        this.center = center;
        this.places = new ArrayList<>();
    }

    public void clearPlaces() {
        this.places.clear();
    }

    public void addPlace(TourPlace place) {
        this.places.add(place);
    }

    // 클러스터 중심점 재계산
    public void recalculateCenter() {
        if (places.isEmpty()) return;

        double sumLat = 0.0;
        double sumLng = 0.0;

        for (TourPlace place : places) {
            sumLat += place.latitude();
            sumLng += place.longitude();
        }

        center.setLat(sumLat / places.size());
        center.setLng(sumLng / places.size());
    }
}