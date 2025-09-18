package com.compass.domain.collection.service;

import com.compass.domain.collection.dto.TravelInfo;

// 여행 정보 수집기의 공통 동작을 정의하는 인터페이스
public interface TravelInfoCollector {

    /**
     * 입력을 받아 여행 정보를 수집합니다.
     * @param input 수집에 필요한 입력 데이터 (구현체에 따라 타입이 다름)
     * @return 수집된 여행 정보(TravelInfo)
     */
    TravelInfo collect(Object input);

}
