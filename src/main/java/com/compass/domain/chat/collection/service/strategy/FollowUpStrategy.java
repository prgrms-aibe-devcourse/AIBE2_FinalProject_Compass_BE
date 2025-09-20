package com.compass.domain.chat.collection.service.strategy;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import java.util.Optional;

//Follow-up 질문 생성 전략 인터페이스
public interface FollowUpStrategy {

    // 주어진 정보를 바탕으로 다음에 할 질문을 찾아 반환
    Optional<String> findNextQuestion(TravelFormSubmitRequest info);
}