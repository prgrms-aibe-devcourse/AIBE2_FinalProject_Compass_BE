package com.compass.domain.parser.service;

// 자연어 입력을 구조화된 데이터로 변환하는 파서 인터페이스
public interface NaturalLanguageParser {

    // 사용자의 자연어 입력을 파싱하여 지정된 타입의 객체로 변환합니다.
    // userInput: 사용자의 원본 메시지 (예: "다음 주 금요일", "제주도")
    // infoType: 추출하려는 정보의 종류 (예: "DATE_RANGE", "DESTINATION")
    // 반환값: 변환된 데이터 객체 (예: DateRange, String)
    Object parse(String userInput, String infoType);

}
