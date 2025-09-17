package com.compass.domain.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;


//빠른 입력 폼의 전체 구조를 정의하는 DTO

public record QuickInputFormDto(
        String formType, // 폼 구조
        List<FormField> formFields, // 필드 타입
        Map<String, Object> validationRules // 검증 규칙
) {

    // 빠른 입력 폼을 구성하는 개별 필드의 속성을 정의하는 내부 DTO
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FormField(
            String name,          // 필드 이름 (서버로 전송될 때의 key)
            String type,          // 프론트엔드 UI 컴포넌트 타입
            String label,         // 사용자에게 보여질 필드의 라벨
            String placeholder,   // 입력 필드에 표시될 안내 문구
            List<String> options, // 선택형 필드(select, radio 등)의 옵션 목록
            boolean required      // 필수 입력 여부
    ) {}
}