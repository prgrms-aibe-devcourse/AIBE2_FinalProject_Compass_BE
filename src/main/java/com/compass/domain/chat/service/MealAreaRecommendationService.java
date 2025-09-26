package com.compass.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 식사 지역 추천 서비스
 * 특정 식당이 아닌 식사하기 좋은 거리/지역을 추천
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MealAreaRecommendationService {

    // MVP는 서울 지역만 지원
    private static final Map<String, List<MealArea>> REGION_MEAL_AREAS = new HashMap<>();

    static {
        // 서울 지역 식사 거리 데이터 (MVP 버전)
        REGION_MEAL_AREAS.put("서울", List.of(
            new MealArea("명동", "다양한 한식, 중식, 일식 맛집이 모여 있는 관광 중심지", "중구", 37.5636, 126.9869),
            new MealArea("강남역 먹자골목", "트렌디한 맛집과 프랜차이즈가 밀집한 지역", "강남구", 37.4979, 127.0276),
            new MealArea("이태원 경리단길", "다국적 음식점이 모여 있는 이국적인 거리", "용산구", 37.5400, 126.9867),
            new MealArea("홍대 먹자골목", "젊은이들이 즐겨 찾는 다양한 음식점 밀집 지역", "마포구", 37.5563, 126.9220),
            new MealArea("종로 인사동", "전통 한식당과 찻집이 많은 문화의 거리", "종로구", 37.5736, 126.9859),
            new MealArea("성수동 카페거리", "감성적인 카페와 레스토랑이 모여 있는 핫플레이스", "성동구", 37.5447, 127.0557),
            new MealArea("연남동 먹자골목", "소규모 개성있는 맛집들이 모여 있는 지역", "마포구", 37.5659, 126.9253),
            new MealArea("건대입구 먹자골목", "대학가 주변 저렴하고 다양한 맛집", "광진구", 37.5401, 127.0701),
            new MealArea("을지로 노가리골목", "복고 분위기의 술집과 안주 맛집", "중구", 37.5663, 126.9913),
            new MealArea("신림동 먹자골목", "다양한 가격대의 맛집이 모인 지역", "관악구", 37.4846, 126.9303),
            new MealArea("여의도 먹자골목", "직장인들이 즐겨 찾는 점심 맛집 거리", "영등포구", 37.5219, 126.9245),
            new MealArea("망원동 맛집거리", "로컬 맛집과 카페가 어우러진 힙한 동네", "마포구", 37.5556, 126.9019)
        ));
    }

    /**
     * 시간대와 위치를 고려한 식사 지역 추천
     */
    public MealAreaRecommendation recommendMealArea(String region, String timeBlock,
                                                    double currentLat, double currentLng) {
        List<MealArea> availableAreas = REGION_MEAL_AREAS.getOrDefault(region,
            REGION_MEAL_AREAS.get("서울"));

        if (availableAreas.isEmpty()) {
            return getDefaultRecommendation(timeBlock);
        }

        // 현재 위치에서 가까운 식사 지역들을 거리순으로 정렬
        MealArea nearestArea = availableAreas.stream()
            .min((a1, a2) -> {
                double dist1 = calculateDistance(currentLat, currentLng, a1.latitude, a1.longitude);
                double dist2 = calculateDistance(currentLat, currentLng, a2.latitude, a2.longitude);
                return Double.compare(dist1, dist2);
            })
            .orElse(availableAreas.get(0));

        String recommendation = generateRecommendation(timeBlock, nearestArea);

        return new MealAreaRecommendation(
            nearestArea.name,
            nearestArea.description,
            recommendation,
            nearestArea.district,
            nearestArea.latitude,
            nearestArea.longitude
        );
    }

    /**
     * 시간대에 맞는 추천 메시지 생성
     */
    private String generateRecommendation(String timeBlock, MealArea area) {
        String timeSpecificMessage = switch (timeBlock) {
            case "BREAKFAST" -> String.format("%s 근처에서 아침식사를 하시는 것을 추천드립니다. " +
                "이 지역에는 아침 일찍 여는 카페와 식당들이 많습니다.", area.name);

            case "LUNCH" -> String.format("%s 일대에서 점심식사를 하시면 좋을 것 같습니다. " +
                "%s 다양한 메뉴를 선택하실 수 있습니다.", area.name, area.description);

            case "DINNER" -> String.format("저녁식사는 %s에서 하시는 것을 추천드립니다. " +
                "%s 분위기 있는 저녁식사를 즐기실 수 있습니다.", area.name, area.description);

            default -> String.format("%s 근처에서 식사하시면 좋을 것 같습니다. %s",
                area.name, area.description);
        };

        return timeSpecificMessage;
    }

    /**
     * 두 지점 간 거리 계산 (Haversine formula)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c; // 거리 (km)
    }

    /**
     * 기본 추천 (지역 정보가 없을 때)
     */
    private MealAreaRecommendation getDefaultRecommendation(String timeBlock) {
        String message = switch (timeBlock) {
            case "BREAKFAST" -> "근처 카페거리나 아침식사가 가능한 거리에서 식사를 추천드립니다.";
            case "LUNCH" -> "주변 맛집이 모여 있는 거리에서 점심식사를 하시면 좋을 것 같습니다.";
            case "DINNER" -> "저녁 분위기가 좋은 거리에서 식사하시는 것을 추천드립니다.";
            default -> "가까운 먹자골목에서 다양한 음식을 즐겨보세요.";
        };

        return new MealAreaRecommendation(
            "주변 맛집 거리",
            "다양한 음식점이 모여 있는 지역",
            message,
            "",
            0.0,
            0.0
        );
    }

    // 내부 클래스들
    private static class MealArea {
        final String name;
        final String description;
        final String district;
        final double latitude;
        final double longitude;

        MealArea(String name, String description, String district, double latitude, double longitude) {
            this.name = name;
            this.description = description;
            this.district = district;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public record MealAreaRecommendation(
        String areaName,
        String description,
        String recommendation,
        String district,
        double latitude,
        double longitude
    ) {}
}