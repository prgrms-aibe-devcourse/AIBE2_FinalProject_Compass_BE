package com.compass.domain.chat.collection.service;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;

//  정보 수집 인터페이스
public interface TravelInfoCollector {

    // 사용자 입력으로부터 여행 정보를 수집(업데이트)
    TravelFormSubmitRequest collect(String userInput, TravelFormSubmitRequest currentInfo);

    // 수집된 정보가 유효한지 검증
    void validate(TravelFormSubmitRequest info);
}