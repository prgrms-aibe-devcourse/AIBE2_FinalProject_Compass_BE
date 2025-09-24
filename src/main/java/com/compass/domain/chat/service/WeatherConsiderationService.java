package com.compass.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 날씨 고려 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherConsiderationService {

    // 날씨 정보 조회
    public WeatherInfo getWeatherInfo(String destination, LocalDate date) {
        log.info("날씨 정보 조회: {} - {}", destination, date);

        try {
            // 7일 이내 여행인 경우 날씨 정보 조회
            if (isWithinWeek(date)) {
                return getWeatherForecast(destination, date);
            }
            
            return null;

        } catch (Exception e) {
            log.error("날씨 정보 조회 실패: {} - {}", destination, date, e);
            return null;
        }
    }

    // 7일 이내 여행 여부 확인
    private boolean isWithinWeek(LocalDate travelDate) {
        var now = LocalDate.now();
        var daysBetween = ChronoUnit.DAYS.between(now, travelDate);
        return daysBetween >= 0 && daysBetween <= 7;
    }

    // 날씨 예보 조회
    private WeatherInfo getWeatherForecast(String destination, LocalDate date) {
        // 기본 날씨 정보 반환 (향후 WeatherAPIClient 연동 예정)
        return new WeatherInfo(
            destination,
            date,
            "맑음",
            25,
            false
        );
    }

    // 날씨 정보 Record
    public record WeatherInfo(
        String location,        // 지역
        LocalDate date,         // 날짜
        String condition,       // 날씨 상태
        Integer temperature,    // 기온
        boolean isRainy        // 비 여부
    ) {}
}

