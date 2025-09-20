package com.compass.domain.collection.service.storage;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;

// 여행 정보 저장 인터페이스
// 수집된 여행 정보를 저장하고 불러옴
public interface TravelInfoStorage {

    // 현재까지 수집된 정보를 저장
    void save(String threadId, TravelFormSubmitRequest info);

    // 이전 대화 정보를 복구
    TravelFormSubmitRequest load(String threadId);

    // 완료된 대화 정보를 삭제
    void delete(String threadId);
}