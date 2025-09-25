package com.compass.domain.chat.service.listener;

import com.compass.domain.chat.function.processing.ProcessOCRFunction;
import com.compass.domain.chat.function.processing.event.ImageOcrQueuedEvent;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.dto.OCRText;
import com.compass.domain.chat.model.request.ImageUrlRequest;
import com.compass.domain.chat.parser.DocumentParser;
import com.compass.domain.chat.orchestrator.PhaseManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrEventListener {

    private final ProcessOCRFunction processOCRFunction;
    private final PhaseManager phaseManager;
    private final List<DocumentParser> documentParsers;  // 모든 파서 주입

    @Async
    @EventListener
    public void handleOcrQueuedEvent(ImageOcrQueuedEvent event) {
        log.info("OCR 이벤트 처리 시작 - threadId: {}, imageUrl: {}",
                event.threadId(), event.imageUrl());

        try {
            // 1. OCR 처리
            var request = new ImageUrlRequest(
                    event.imageUrl(),
                    event.threadId(),
                    event.userId(),
                    "image/jpeg"
            );
            var ocrResult = processOCRFunction.apply(request);

            log.info("OCR 완료 - documentType: {}, textLength: {}",
                    ocrResult.documentType(), ocrResult.extractedText().length());

            // 2. 문서 파싱
            var parser = findParser(ocrResult.documentType());
            if (parser == null) {
                log.warn("파서를 찾을 수 없음 - documentType: {}", ocrResult.documentType());
                // Phase Manager에 원본 텍스트만 전달
                phaseManager.updatePhase2WithOcrText(
                        event.threadId(),
                        ocrResult.extractedText(),
                        ocrResult.documentType()
                );
                return;
            }

            // 3. 확정 일정으로 변환
            var ocrText = new OCRText(
                    ocrResult.extractedText(),
                    event.threadId(),
                    event.userId(),
                    event.imageUrl()
            );
            var confirmedSchedule = parser.parse(ocrText, event.imageUrl());

            log.info("문서 파싱 완료 - title: {}, startTime: {}, isFixed: {}",
                    confirmedSchedule.title(),
                    confirmedSchedule.startTime(),
                    confirmedSchedule.isFixed());

            // 4. Phase Manager에 전달
            phaseManager.updatePhase2WithOcrSchedule(event.threadId(), confirmedSchedule);

            // 5. 사용자에게 알림 (옵션)
            notifyUser(event.threadId(), event.userId(), confirmedSchedule);

        } catch (Exception e) {
            log.error("OCR 이벤트 처리 실패 - threadId: {}, imageUrl: {}",
                    event.threadId(), event.imageUrl(), e);
            // 실패 시 Phase Manager에 에러 전달
            // phaseManager.notifyOcrError(event.threadId(), e.getMessage());
            log.error("OCR 에러 알림: threadId={}, error={}", event.threadId(), e.getMessage());
        }
    }

    // 병렬 처리를 위한 비동기 메서드
    @Async
    public CompletableFuture<List<ConfirmedSchedule>> processMultipleImages(
            List<ImageOcrQueuedEvent> events
    ) {
        var futures = events.stream()
                .map(event -> CompletableFuture.supplyAsync(() -> {
                    try {
                        var request = new ImageUrlRequest(
                                event.imageUrl(),
                                event.threadId(),
                                event.userId(),
                                "image/jpeg"
                        );
                        var ocrResult = processOCRFunction.apply(request);
                        var parser = findParser(ocrResult.documentType());
                        if (parser != null) {
                            var ocrText = new OCRText(
                                    ocrResult.extractedText(),
                                    event.threadId(),
                                    event.userId(),
                                    event.imageUrl()
                            );
                            return parser.parse(ocrText, event.imageUrl());
                        }
                    } catch (Exception e) {
                        log.error("이미지 처리 실패 - {}", event.imageUrl(), e);
                    }
                    return null;
                }))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(schedule -> schedule != null)
                        .toList());
    }

    private DocumentParser findParser(com.compass.domain.chat.model.enums.DocumentType documentType) {
        return documentParsers.stream()
                .filter(parser -> parser.canParse(documentType))
                .findFirst()
                .orElse(null);
    }

    private void notifyUser(String threadId, String userId, ConfirmedSchedule schedule) {
        // 웹소켓이나 SSE로 사용자에게 알림
        var message = String.format(
                "📋 %s 정보를 확인했습니다.\n" +
                "- 일정: %s\n" +
                "- 시간: %s ~ %s\n" +
                "- 장소: %s",
                schedule.documentType().getDescription(),
                schedule.title(),
                schedule.startTime(),
                schedule.endTime(),
                schedule.location()
        );

        // TODO: WebSocket/SSE 구현 시 실제 알림 전송
        log.info("사용자 알림 - threadId: {}, message: {}", threadId, message);
    }
}