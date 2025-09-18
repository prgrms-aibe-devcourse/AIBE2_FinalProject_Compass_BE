package com.compass.domain.chat.model.request;

// AnalyzeUserInputFunction의 입력을 정의하는 DTO
public record AnalyzeUserInputRequest(
    String userId, //  사용자 id
    String userInput, // 사용자의 자연어 입력
    TravelFormSubmitRequest currentInfo // 현재까지 수집된 여행 정보
) {}