package com.compass.domain.chat.model.response;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;

// ContinueFollowUpFunction의 입력을 정의하는 DTO
public record FollowUpResponse(
    TravelFormSubmitRequest currentInfo,
    String userInput
) {}