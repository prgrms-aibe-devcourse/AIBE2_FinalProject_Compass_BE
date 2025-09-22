package com.compass.domain.chat.model.context;

import com.compass.domain.chat.model.enums.Intent;
import com.compass.domain.chat.model.enums.TravelPhase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.model.request.TravelPlanRequest;

// 대화 컨텍스트 관리 - Thread별 대화 상태 및 수집된 정보 저장
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TravelContext implements Serializable {

    private static final long serialVersionUID = 1L;

    // 표준 데이터 키 상수 - Function들과의 통합을 위한 표준화
    public static final String KEY_DESTINATIONS = "destinations";
    public static final String KEY_DEPARTURE = "departureLocation";
    public static final String KEY_START_DATE = "startDate";
    public static final String KEY_END_DATE = "endDate";
    public static final String KEY_BUDGET = "budget";
    public static final String KEY_TRAVEL_STYLE = "travelStyle";
    public static final String KEY_COMPANIONS = "companions";
    public static final String KEY_INTERESTS = "interests";
    public static final String KEY_ACCOMMODATION_TYPE = "accommodationType";
    public static final String KEY_TRANSPORTATION_TYPE = "transportationType";
    public static final String KEY_RESERVATION_DOCUMENT = "reservationDocument";

    // 기본 식별 정보
    private String threadId;  // 대화 스레드 식별자
    private String userId;    // 사용자 식별자

    // Phase 및 Intent 관리
    private String currentPhase;  // 현재 대화 단계 (TravelPhase enum)
    private Intent currentIntent; // 현재 사용자 의도

    // 수집된 여행 정보 저장
    // 키: destination, startDate, endDate, budget, travelers, style, activities 등
    @Builder.Default
    private Map<String, Object> collectedInfo = new ConcurrentHashMap<>();

    // 생성된 여행 계획 (ItineraryDTO 또는 JSON 형태로 저장)
    private Object travelPlan;

    // 대화 내역 관리
    @Builder.Default
    private List<MessageHistory> messageHistory = new ArrayList<>();

    // 대화 진행 추적
    @Builder.Default
    private int conversationCount = 0;  // 대화 횟수 카운트

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();  // 컨텍스트 생성 시간

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();  // 마지막 업데이트 시간

    // 추가 메타데이터
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();  // 확장 가능한 메타데이터

    // 여행 계획 확인 대기 상태
    @Builder.Default
    private boolean waitingForTravelConfirmation = false;

    // 대화 횟수 증가
    public void incrementConversation() {
        this.conversationCount++;
        this.updatedAt = LocalDateTime.now();
    }

    // 수집된 정보 추가/업데이트
    public void updateCollectedInfo(String key, Object value) {
        if (collectedInfo == null) {
            collectedInfo = new ConcurrentHashMap<>();
        }
        collectedInfo.put(key, value);
        this.updatedAt = LocalDateTime.now();
    }

    // 메시지 히스토리 추가
    public void addMessage(String role, String content) {
        if (messageHistory == null) {
            messageHistory = new ArrayList<>();
        }
        messageHistory.add(MessageHistory.builder()
            .role(role)
            .content(content)
            .timestamp(LocalDateTime.now())
            .build());
        this.updatedAt = LocalDateTime.now();
    }

    // Phase 업데이트 (String으로 받아서 처리)
    public void updatePhase(String phase) {
        this.currentPhase = phase;
        this.updatedAt = LocalDateTime.now();
    }

    // Phase 업데이트 (TravelPhase enum으로 받아서 처리)
    public void updatePhase(TravelPhase phase) {
        this.currentPhase = phase.name();
        this.updatedAt = LocalDateTime.now();
    }

    // Intent 업데이트
    public void updateIntent(Intent intent) {
        this.currentIntent = intent;
        this.updatedAt = LocalDateTime.now();
    }

    // 컨텍스트 초기화 (새 대화 시작)
    public void reset() {
        this.collectedInfo.clear();
        this.messageHistory.clear();
        this.travelPlan = null;
        this.conversationCount = 0;
        this.currentPhase = TravelPhase.INITIALIZATION.name();
        this.currentIntent = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // TravelFormSubmitRequest로부터 정보 업데이트
    public void updateFromFormSubmit(TravelFormSubmitRequest request) {
        if (request == null) return;

        if (request.destinations() != null) {
            updateCollectedInfo(KEY_DESTINATIONS, request.destinations());
        }
        if (request.departureLocation() != null) {
            updateCollectedInfo(KEY_DEPARTURE, request.departureLocation());
        }
        if (request.travelDates() != null) {
            if (request.travelDates().startDate() != null) {
                updateCollectedInfo(KEY_START_DATE, request.travelDates().startDate());
            }
            if (request.travelDates().endDate() != null) {
                updateCollectedInfo(KEY_END_DATE, request.travelDates().endDate());
            }
        }
        if (request.budget() != null) {
            updateCollectedInfo(KEY_BUDGET, request.budget());
        }
        if (request.travelStyle() != null) {
            updateCollectedInfo(KEY_TRAVEL_STYLE, request.travelStyle());
        }
        if (request.companions() != null) {
            updateCollectedInfo(KEY_COMPANIONS, request.companions());
        }
        if (request.reservationDocument() != null) {
            updateCollectedInfo(KEY_RESERVATION_DOCUMENT, request.reservationDocument());
        }
    }

    // TravelPlanRequest로 변환
    @SuppressWarnings("unchecked")
    public TravelPlanRequest toTravelPlanRequest() {
        return new TravelPlanRequest(
            (List<String>) collectedInfo.getOrDefault(KEY_DESTINATIONS, List.of()),
            (java.time.LocalDate) collectedInfo.get(KEY_START_DATE),
            (java.time.LocalDate) collectedInfo.get(KEY_END_DATE),
            (Integer) collectedInfo.get(KEY_BUDGET),
            (List<String>) collectedInfo.getOrDefault(KEY_TRAVEL_STYLE, List.of()),
            collectedInfo.containsKey(KEY_COMPANIONS) ?
                (collectedInfo.get(KEY_COMPANIONS) instanceof List ?
                    (List<String>) collectedInfo.get(KEY_COMPANIONS) :
                    List.of(String.valueOf(collectedInfo.get(KEY_COMPANIONS)))) :
                List.of("혼자"),
            (String) collectedInfo.get(KEY_DEPARTURE),
            (List<String>) collectedInfo.getOrDefault(KEY_INTERESTS, List.of()),
            (String) collectedInfo.getOrDefault(KEY_ACCOMMODATION_TYPE, "호텔"),
            (String) collectedInfo.getOrDefault(KEY_TRANSPORTATION_TYPE, "대중교통")
        );
    }

    // TravelFormSubmitRequest로 변환 (현재 수집된 정보 기반)
    @SuppressWarnings("unchecked")
    public TravelFormSubmitRequest toTravelFormSubmitRequest() {
        java.time.LocalDate startDate = (java.time.LocalDate) collectedInfo.get(KEY_START_DATE);
        java.time.LocalDate endDate = (java.time.LocalDate) collectedInfo.get(KEY_END_DATE);

        TravelFormSubmitRequest.DateRange dateRange = null;
        if (startDate != null && endDate != null) {
            dateRange = new TravelFormSubmitRequest.DateRange(startDate, endDate);
        }

        return new TravelFormSubmitRequest(
            userId,
            (List<String>) collectedInfo.getOrDefault(KEY_DESTINATIONS, List.of()),
            (String) collectedInfo.get(KEY_DEPARTURE),
            dateRange,
            (String) collectedInfo.get(KEY_COMPANIONS),
            collectedInfo.get(KEY_BUDGET) != null ?
                Long.valueOf(String.valueOf(collectedInfo.get(KEY_BUDGET))) : null,
            (List<String>) collectedInfo.getOrDefault(KEY_TRAVEL_STYLE, List.of()),
            (String) collectedInfo.get(KEY_RESERVATION_DOCUMENT)
        );
    }

    // 필수 정보 완성도 체크
    public boolean isRequiredInfoComplete() {
        // 필수 항목: 목적지, 출발지, 여행 날짜
        boolean hasDestinations = collectedInfo.containsKey(KEY_DESTINATIONS) &&
            !((List<?>) collectedInfo.get(KEY_DESTINATIONS)).isEmpty();
        boolean hasDeparture = collectedInfo.containsKey(KEY_DEPARTURE) &&
            collectedInfo.get(KEY_DEPARTURE) != null;
        boolean hasDates = collectedInfo.containsKey(KEY_START_DATE) &&
            collectedInfo.containsKey(KEY_END_DATE) &&
            collectedInfo.get(KEY_START_DATE) != null &&
            collectedInfo.get(KEY_END_DATE) != null;

        return hasDestinations && hasDeparture && hasDates;
    }

    // 진행률 계산 (수집된 정보 비율)
    public double calculateProgress() {
        int totalFields = 10; // 전체 수집 가능한 필드 수
        int collectedFields = 0;

        if (collectedInfo.containsKey(KEY_DESTINATIONS)) collectedFields++;
        if (collectedInfo.containsKey(KEY_DEPARTURE)) collectedFields++;
        if (collectedInfo.containsKey(KEY_START_DATE)) collectedFields++;
        if (collectedInfo.containsKey(KEY_END_DATE)) collectedFields++;
        if (collectedInfo.containsKey(KEY_BUDGET)) collectedFields++;
        if (collectedInfo.containsKey(KEY_TRAVEL_STYLE)) collectedFields++;
        if (collectedInfo.containsKey(KEY_COMPANIONS)) collectedFields++;
        if (collectedInfo.containsKey(KEY_INTERESTS)) collectedFields++;
        if (collectedInfo.containsKey(KEY_ACCOMMODATION_TYPE)) collectedFields++;
        if (collectedInfo.containsKey(KEY_TRANSPORTATION_TYPE)) collectedFields++;

        return (double) collectedFields / totalFields * 100;
    }

    // 내부 클래스: 메시지 히스토리
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageHistory implements Serializable {
        private static final long serialVersionUID = 1L;

        private String role;      // user, assistant, system
        private String content;   // 메시지 내용
        private LocalDateTime timestamp;  // 메시지 시간
    }
}