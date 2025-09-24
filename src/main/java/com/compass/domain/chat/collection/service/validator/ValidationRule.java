package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;

// 개별 검증 규칙을 정의하는 인터페이스
public interface ValidationRule {

    // 규칙 위반 시 예외를 발생시켜 검증을 수행
    void apply(TravelFormSubmitRequest info);
}
