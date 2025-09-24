package com.compass.domain.chat.collection.service.validator;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;

import java.util.Optional;

// 개별 검증 규칙을 정의하는 인터페이스
public interface ValidationRule {

    // 이 규칙을 적용할 조건인지 판단합니다.
    boolean appliesTo(TravelFormSubmitRequest request);

    // 규칙 위반 시 오류 메시지를, 정상이면 빈 Optional을 반환합니다.
    Optional<String> validate(TravelFormSubmitRequest request);
}
