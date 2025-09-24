package com.compass.domain.chat.function.processing.phase3.stage2.model;

// Stage1에서 전달받는 관광지 정보
public record TourPlace(
    String id,
    String name,
    String timeBlock,
    Integer day,
    String recommendTime,
    Double latitude,
    Double longitude,
    String address,
    String category,
    String operatingHours,
    String closedDays,
    Boolean petAllowed,
    Boolean parkingAvailable,
    Double rating,
    String priceLevel,
    Boolean isTrendy
) {
    // Builder 패턴 구현
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String timeBlock;
        private Integer day;
        private String recommendTime;
        private Double latitude;
        private Double longitude;
        private String address;
        private String category;
        private String operatingHours;
        private String closedDays;
        private Boolean petAllowed;
        private Boolean parkingAvailable;
        private Double rating;
        private String priceLevel;
        private Boolean isTrendy;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder timeBlock(String timeBlock) { this.timeBlock = timeBlock; return this; }
        public Builder day(Integer day) { this.day = day; return this; }
        public Builder recommendTime(String recommendTime) { this.recommendTime = recommendTime; return this; }
        public Builder latitude(Double latitude) { this.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { this.longitude = longitude; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder operatingHours(String operatingHours) { this.operatingHours = operatingHours; return this; }
        public Builder closedDays(String closedDays) { this.closedDays = closedDays; return this; }
        public Builder petAllowed(Boolean petAllowed) { this.petAllowed = petAllowed; return this; }
        public Builder parkingAvailable(Boolean parkingAvailable) { this.parkingAvailable = parkingAvailable; return this; }
        public Builder rating(Double rating) { this.rating = rating; return this; }
        public Builder priceLevel(String priceLevel) { this.priceLevel = priceLevel; return this; }
        public Builder isTrendy(Boolean isTrendy) { this.isTrendy = isTrendy; return this; }

        public TourPlace build() {
            return new TourPlace(id, name, timeBlock, day, recommendTime, latitude, longitude,
                address, category, operatingHours, closedDays, petAllowed, parkingAvailable,
                rating, priceLevel, isTrendy);
        }
    }

    // Getter 메서드들 (Record가 자동 생성하지만 호환성을 위해 추가)
    public String getId() { return id; }
    public String getName() { return name; }
    public String getTimeBlock() { return timeBlock; }
    public Integer getDay() { return day; }
    public String getRecommendTime() { return recommendTime; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getAddress() { return address; }
    public String getCategory() { return category; }
    public String getOperatingHours() { return operatingHours; }
    public String getClosedDays() { return closedDays; }
    public Boolean getPetAllowed() { return petAllowed; }
    public Boolean getParkingAvailable() { return parkingAvailable; }
    public Double getRating() { return rating; }
    public String getPriceLevel() { return priceLevel; }
    public Boolean getIsTrendy() { return isTrendy; }
}