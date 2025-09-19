package com.compass.domain.chat.parser.service;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;

// 사용자 입력(자연어)에서 여행 정보를 추출하는 인터페이스
public interface TravelInfoParser {

    // 사용자 입력과 현재 정보를 바탕으로 여행 정보를 분석하고 업데이트
    TravelFormSubmitRequest parse(String userInput, TravelFormSubmitRequest currentInfo);
}