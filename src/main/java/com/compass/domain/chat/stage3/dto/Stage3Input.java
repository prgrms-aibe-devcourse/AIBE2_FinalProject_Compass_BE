package com.compass.domain.chat.stage3.dto;

import com.compass.domain.chat.stage2.dto.SelectedSchedule;
import java.time.LocalDate;
import java.util.List;

// Stage 3 입력 데이터 (Phase 2에서 전달받는 데이터)
public record Stage3Input(
    String destination,                   // 여행지 (서울, 부산 등)
    LocalDate startDate,                  // 여행 시작일
    LocalDate endDate,                    // 여행 종료일
    String travelStyle,                   // 여행 스타일 (휴양, 관광, 미식 등)
    String travelCompanion,               // 동행자 (가족, 친구, 연인, 혼자)
    List<SelectedSchedule> userSelectedPlaces,  // 사용자가 선택한 장소들
    String transportMode,                 // 이동 수단 (자차, 대중교통)
    Integer budget,                       // 예산 (선택사항)
    List<String> specialRequirements     // 특별 요구사항 (반려동물 동반, 휠체어 접근 등)
) {
    // Phase 2 ConfirmedSchedule에서 변환하는 정적 메서드
    public static Stage3Input fromPhase2Output(
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String travelStyle,
        String travelCompanion,
        String transportMode
    ) {
        return new Stage3Input(
            destination,
            startDate,
            endDate,
            travelStyle,
            travelCompanion,
            List.of(), // 초기에는 빈 리스트
            transportMode != null ? transportMode : "대중교통",
            null,
            List.of()
        );
    }

    // 사용자 선택 장소 추가
    public Stage3Input withUserSelectedPlaces(List<SelectedSchedule> places) {
        return new Stage3Input(
            destination, startDate, endDate, travelStyle, travelCompanion,
            places, transportMode, budget, specialRequirements
        );
    }

    // 특별 요구사항 추가
    public Stage3Input withSpecialRequirements(List<String> requirements) {
        return new Stage3Input(
            destination, startDate, endDate, travelStyle, travelCompanion,
            userSelectedPlaces, transportMode, budget, requirements
        );
    }
}