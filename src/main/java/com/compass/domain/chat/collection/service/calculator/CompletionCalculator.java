package com.compass.domain.chat.collection.service.calculator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import java.util.List;

// 여행 정보 수집 진행률 계산 인터페이스
public interface CompletionCalculator {

    // 0~100% 진행률을 계산하여 반환
    int calculate(TravelFormSubmitRequest info);

    // 아직 입력되지 않은 필수 필드 목록을 반환
    List<String> getRequiredFields(TravelFormSubmitRequest info);
}