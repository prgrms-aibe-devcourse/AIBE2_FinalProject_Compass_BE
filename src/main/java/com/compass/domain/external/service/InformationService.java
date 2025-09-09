package com.compass.domain.external.service;

public interface InformationService {
    /**
     * 날씨, 환율 등 실시간 정보 조회 요청을 처리합니다.
     */
    String getRealtimeInfo(String message);
}
