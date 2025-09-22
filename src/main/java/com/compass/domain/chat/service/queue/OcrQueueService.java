package com.compass.domain.chat.service.queue;

public interface OcrQueueService {

    void enqueue(String objectKey, String imageUrl, String threadId, String userId, String contentType);
}
