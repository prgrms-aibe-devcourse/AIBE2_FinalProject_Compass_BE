package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import java.util.List;

// 수집된 여행 정보의 유효성을 검증하는 인터페이스
public interface TravelInfoValidator {

    // 여행 정보의 유효성을 검증
    void validate(TravelFormSubmitRequest info);

    // 검증 실패 시 상세 오류 메시지 목록을 반환
    List<String> getErrors();
}