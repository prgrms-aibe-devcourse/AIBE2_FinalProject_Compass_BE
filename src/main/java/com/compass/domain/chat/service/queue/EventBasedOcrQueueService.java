package com.compass.domain.chat.service.queue;

import com.compass.domain.chat.function.processing.event.ImageOcrQueuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventBasedOcrQueueService implements OcrQueueService {

    private final ApplicationEventPublisher publisher;

    @Override
    public void enqueue(String objectKey, String imageUrl, String threadId, String userId, String contentType) {
        publisher.publishEvent(new ImageOcrQueuedEvent(objectKey, imageUrl, threadId, userId, contentType));
        log.debug("OCR 큐(이벤트) 등록 - key: {}", objectKey);
    }
}
