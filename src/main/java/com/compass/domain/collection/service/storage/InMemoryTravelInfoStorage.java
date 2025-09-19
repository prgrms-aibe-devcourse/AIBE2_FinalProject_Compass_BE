package com.compass.domain.collection.service.storage;

import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 메모리(Map)를 저장소로 사용하는 TravelInfoStorage 구현체
@Component
public class InMemoryTravelInfoStorage implements TravelInfoStorage {

    private final Map<String, TravelFormSubmitRequest> storage = new ConcurrentHashMap<>();

    @Override
    public void save(String threadId, TravelFormSubmitRequest info) {
        storage.put(threadId, info);
    }

    @Override
    public TravelFormSubmitRequest load(String threadId) {
        // 저장된 정보가 없으면, 비어있는 새 객체를 반환하여 NullPointerException 방지
        return storage.getOrDefault(threadId, new TravelFormSubmitRequest(null, null, null, null, null, null, null, null));
    }

    @Override
    public void delete(String threadId) {
        storage.remove(threadId);
    }
}