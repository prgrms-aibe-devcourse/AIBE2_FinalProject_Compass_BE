package com.compass.domain.chat.stage_integration.service;

import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.TravelPlace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Stage2To3DirectConverter {

    /**
     * Stage 2에서 선택된 장소들을 직접 Stage 3용 dailyDistribution으로 변환
     * 데이터베이스 조회 없이 프론트엔드에서 받은 데이터만으로 처리
     */
    public Map<String, Object> convertSelectedPlacesToStage3(TravelContext context, Map<String, Object> metadata) {
        log.info("🔄 [Direct Converter] Stage 2 → Stage 3 직접 변환 시작");

        try {
            // 메타데이터에서 선택된 장소 추출
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> selectedPlaces = (List<Map<String, Object>>) metadata.get("selectedPlaces");

            if (selectedPlaces == null || selectedPlaces.isEmpty()) {
                log.warn("⚠️ 선택된 장소가 없습니다.");
                return Map.of(
                    "success", false,
                    "message", "선택된 장소가 없습니다.",
                    "stage", 3
                );
            }

            log.info("📍 선택된 장소 수: {}", selectedPlaces.size());

            // 여행 날짜 정보 가져오기
            String startDate = (String) context.getCollectedInfo().get(TravelContext.KEY_START_DATE);
            String endDate = (String) context.getCollectedInfo().get(TravelContext.KEY_END_DATE);
            int totalDays = calculateTravelDays(startDate, endDate);
            log.info("📅 여행 기간: {}일 ({}~{})", totalDays, startDate, endDate);

            // selectedPlaces를 TravelPlace로 변환하고 날짜별로 그룹화
            Map<Integer, List<TravelPlace>> dailyDistribution = new HashMap<>();

            for (Map<String, Object> place : selectedPlaces) {
                TravelPlace travelPlace = convertToTravelPlace(place, totalDays, dailyDistribution.size());

                // day 필드 추출 (없으면 균등 분배)
                Integer day = extractDay(place, totalDays, dailyDistribution.size());

                // 날짜별 리스트에 추가
                dailyDistribution.computeIfAbsent(day, k -> new ArrayList<>()).add(travelPlace);
            }

            log.info("📅 생성된 dailyDistribution: {} 일, 총 {} 개 장소",
                dailyDistribution.size(),
                dailyDistribution.values().stream().mapToInt(List::size).sum());

            // 날짜별 장소 수 로깅
            dailyDistribution.forEach((day, places) -> {
                log.info("  Day {}: {} 개 장소", day, places.size());
            });

            // Context에 dailyDistribution 저장
            context.getMetadata().put("dailyDistribution", dailyDistribution);
            context.getMetadata().put("userSelectedPlaces",
                dailyDistribution.values().stream().flatMap(List::stream).collect(Collectors.toList()));

            // 성공 응답 반환
            return Map.of(
                "success", true,
                "message", String.format("%d개 장소를 %d일에 분배했습니다.", selectedPlaces.size(), totalDays),
                "stage", 3,
                "dailyDistribution", dailyDistribution,
                "totalDays", totalDays
            );

        } catch (Exception e) {
            log.error("❌ Direct conversion 중 오류: ", e);
            return Map.of(
                "success", false,
                "message", "변환 중 오류 발생: " + e.getMessage(),
                "stage", 3
            );
        }
    }

    private TravelPlace convertToTravelPlace(Map<String, Object> placeMap, int totalDays, int currentDistributionSize) {
        TravelPlace place = new TravelPlace();

        // ID 설정
        Object id = placeMap.get("id");
        if (id != null) {
            place.setPlaceId(String.valueOf(id));
        } else {
            place.setPlaceId(UUID.randomUUID().toString());
        }

        // 기본 필드 설정
        place.setName(getStringValue(placeMap, "name", "Unknown Place"));
        place.setCategory(getStringValue(placeMap, "category", "기타"));
        place.setAddress(getStringValue(placeMap, "address", ""));
        place.setDescription(getStringValue(placeMap, "description", ""));

        // 좌표 설정 (다양한 필드명 처리)
        Double lat = getDoubleValue(placeMap, "lat", "latitude");
        Double lng = getDoubleValue(placeMap, "lng", "longitude", "lon");

        if (lat != null && lng != null) {
            place.setLatitude(lat);
            place.setLongitude(lng);
        } else {
            // 기본값 (서울 시청)
            place.setLatitude(37.5666805);
            place.setLongitude(126.9784147);
        }

        // 평점 설정
        place.setRating(getDoubleValue(placeMap, "rating"));

        // 사용자 선택 플래그 설정
        place.setIsUserSelected(true);

        return place;
    }

    private Integer extractDay(Map<String, Object> placeMap, int totalDays, int currentDistributionSize) {
        Object dayObj = placeMap.get("day");
        Integer day = null;

        if (dayObj instanceof Number) {
            day = ((Number) dayObj).intValue();
        } else if (dayObj instanceof String) {
            try {
                day = Integer.parseInt((String) dayObj);
            } catch (NumberFormatException e) {
                log.debug("Invalid day value: {}", dayObj);
            }
        }

        // day가 없거나 범위를 벗어나면 균등 분배
        if (day == null || day < 1 || day > totalDays) {
            day = (currentDistributionSize % totalDays) + 1;
        }

        return day;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            return String.valueOf(value);
        }
        return defaultValue;
    }

    private Double getDoubleValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    // Continue to next key
                }
            }
        }
        return null;
    }

    private int calculateTravelDays(String startDate, String endDate) {
        try {
            if (startDate != null && endDate != null) {
                // 간단한 날짜 계산 (실제로는 더 정교한 계산 필요)
                // YYYY-MM-DD 형식 가정
                String[] start = startDate.split("-");
                String[] end = endDate.split("-");

                if (start.length == 3 && end.length == 3) {
                    int startDay = Integer.parseInt(start[2]);
                    int endDay = Integer.parseInt(end[2]);
                    int days = endDay - startDay + 1;

                    // 월이 다른 경우 대략적인 계산
                    int startMonth = Integer.parseInt(start[1]);
                    int endMonth = Integer.parseInt(end[1]);
                    if (endMonth > startMonth) {
                        days += (endMonth - startMonth) * 30;
                    }

                    return Math.max(1, Math.min(days, 30)); // 1~30일로 제한
                }
            }
        } catch (Exception e) {
            log.warn("날짜 계산 실패, 기본값 사용: {}", e.getMessage());
        }

        return 3; // 기본값 3일
    }
}