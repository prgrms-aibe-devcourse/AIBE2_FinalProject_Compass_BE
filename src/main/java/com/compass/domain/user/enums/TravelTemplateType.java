package com.compass.domain.user.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum TravelTemplateType {
    //================================================================================
    // 카테고리 1: 활동 및 테마 (Activities & Themes)
    //================================================================================

    // 미식 (Gourmet)
    /**
     * 파인다이닝/미식 여행
     */
    FINE_DINING_TRIP,
    /**
     * 노포/로컬 맛집 탐방
     */
    LOCAL_FOOD_STALL_TOUR,
    /**
     * 카페 투어
     */
    CAFE_HOPPING,
    /**
     * 디저트 맛집 투어
     */
    DESSERT_TOUR,
    /**
     * 양조장/와이너리 투어
     */
    BREWERY_AND_WINERY_TOUR,

    // 문화/예술 (Culture & Arts)
    /**
     * 박물관/미술관 투어
     */
    MUSEUM_AND_GALLERY_TOUR,
    /**
     * 유적지/사적지 순회
     */
    HISTORIC_SITE_HOPPING,
    /**
     * 건축물 탐방
     */
    ARCHITECTURAL_TOUR,
    /**
     * 전통시장 체험
     */
    TRADITIONAL_MARKET_TOUR,
    /**
     * 콘서트/페스티벌 참가
     */
    CONCERT_AND_FESTIVAL_TRIP,
    /**
     * 연극/공연 관람
     */
    THEATER_AND_PERFORMANCE,

    // 액티비티 (Activities)
    /**
     * 등산/산악 트레킹
     */
    MOUNTAIN_TREKKING,
    /**
     * 서핑/다이빙 등 수상 스포츠
     */
    WATER_SPORTS,
    /**
     * 스키/스노보드 등 동계 스포츠
     */
    WINTER_SPORTS,
    /**
     * 캠핑/글램핑
     */
    CAMPING_AND_GLAMPING,
    /**
     * 낚시 여행
     */
    FISHING_TRIP,
    /**
     * 골프 여행
     */
    GOLF_TOUR,
    /**
     * 자전거/라이딩 여행
     */
    BICYCLE_TOUR,

    // 쇼핑 (Shopping)
    /**
     * 명품/럭셔리 쇼핑
     */
    LUXURY_BRAND_SHOPPING,
    /**
     * 아울렛/면세점 쇼핑
     */
    OUTLET_AND_DUTY_FREE_SHOPPING,
    /**
     * 빈티지/플리마켓 쇼핑
     */
    VINTAGE_AND_FLEA_MARKET_SHOPPING,

    // 휴양/힐링 (Relaxation & Healing)
    /**
     * 스파/웰니스
     */
    SPA_AND_WELLNESS,
    /**
     * 템플스테이
     */
    TEMPLE_STAY,
    /**
     * 산림욕/자연휴양림
     */
    FOREST_BATHING_TRIP,

    //================================================================================
    // 카테고리 2: 장소 및 환경 (Location & Environment)
    //================================================================================
    /**
     * 대도시 여행
     */
    URBAN_CITY_TOUR,
    /**
     * 농촌/시골 마을 체험
     */
    RURAL_VILLAGE_STAY,
    /**
     * 해변 휴양지
     */
    BEACH_RESORT_VACATION,
    /**
     * 섬 여행
     */
    ISLAND_HOPPING,
    /**
     * 국립공원 탐방
     */
    NATIONAL_PARK_EXPLORER,
    /**
     * 테마파크/놀이공원 중심
     */
    THEME_PARK_FOCUSED,
    /**
     * 야경 명소 탐방
     */
    NIGHT_VIEW_SPOT_TOUR,

    //================================================================================
    // 카테고리 3: 동반자 유형 (Companion Type)
    //================================================================================
    /**
     * 친구 여행
     */
    FRIENDS_TRIP,
    /**
     * 자녀 동반 여행
     */
    PARENT_CHILD_TRIP,
    /**
     * 3대 이상 대가족 여행
     */
    MULTI_GENERATION_FAMILY_TRIP,
    /**
     * 반려동물 동반 여행
     */
    PET_FRIENDLY_TRIP,
    /**
     * 동호회/단체 여행
     */
    CLUB_OR_GROUP_TOUR,

    //================================================================================
    // 카테고리 4: 숙소 유형 (Accommodation Style)
    //================================================================================
    /**
     * 특급/5성급 호텔 호캉스
     */
    LUXURY_HOTEL_STAY,
    /**
     * 부티크/디자인 호텔
     */
    BOUTIQUE_HOTEL_TOUR,
    /**
     * 한옥 스테이
     */
    HANOK_STAY,
    /**
     * 펜션/풀빌라
     */
    PENSION_AND_POOL_VILLA,
    /**
     * 게스트하우스/호스텔
     */
    GUESTHOUSE_AND_HOSTEL_STAY,

    //================================================================================
    // 카테고리 5: 여행 기간 및 시기 (Duration & Timing)
    //================================================================================
    /**
     * 주말 여행
     */
    WEEKEND_GETAWAY,
    /**
     * 4박 5일 여행
     */
    FOUR_NIGHTS_FIVE_DAYS,
    /**
     * 일주일 내외 여행
     */
    WEEKLONG_TRIP,
    /**
     * 장기 체류 - 2주 이상
     */
    LONG_TERM_STAY,
    /**
     * 명절/연휴 시즌 여행
     */
    HOLIDAY_SEASON_TRIP,

    //================================================================================
    // 카테고리 6: 이동 수단 (Transportation)
    //================================================================================
    /**
     * 기차 여행
     */
    TRAIN_JOURNEY,
    /**
     * 자차/렌터카 로드트립
     */
    ROAD_TRIP_BY_CAR,
    /**
     * 대중교통 이용 여행
     */
    PUBLIC_TRANSIT_EXPLORER,
    /**
     * 크루즈 여행
     */
    CRUISE_TRIP;

    public static List<String> getNames() {
        return Arrays.stream(TravelTemplateType.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}