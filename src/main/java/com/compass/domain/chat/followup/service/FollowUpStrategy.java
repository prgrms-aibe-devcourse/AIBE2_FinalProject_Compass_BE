package com.compass.domain.chat.followup.service;

import com.compass.domain.chat.collection.dto.TravelInfo;
import com.compass.domain.chat.followup.dto.FollowUpQuestion;

import java.util.Optional;

// 후속 질문 생성 전략에 대한 공통 인터페이스
public interface FollowUpStrategy {

    // 주어진 여행 정보를 바탕으로 후속 질문을 생성합니다.
    // 정보가 모두 채워져 있으면 Optional.empty()를 반환합니다.
    Optional<FollowUpQuestion> generate(TravelInfo travelInfo);

}
