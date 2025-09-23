package com.compass.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

// Spring AI Function Calling 로깅 컴포넌트
@Slf4j
@Component
public class SpringAIFunctionCallLogger implements ApplicationListener<ContextRefreshedEvent> {

    private static final AtomicInteger functionCallCounter = new AtomicInteger(0);

    @Autowired(required = false)
    private ChatModel chatModel;

    @PostConstruct
    public void init() {
        log.info("Spring AI Function Call 로깅 시스템 초기화 - LLM Function Calling 모니터링 시작");
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (chatModel != null) {
            log.info("Spring AI ChatModel 감지됨 - Function Calling 모니터링 활성화");
        } else {
            log.warn("Spring AI ChatModel 없음 - Function Calling 모니터링 비활성화");
        }
    }

    // Function Call 이벤트 로깅 (Spring AI에서 호출 시)
    public static void logFunctionCall(String functionName, Object request, Object response) {
        int callNumber = functionCallCounter.incrementAndGet();

        log.info("[LLM Function Call #{}] Function: {}, Request: {}, Response: {}, Total: {}",
            callNumber, functionName,
            request != null ? request.toString() : "null",
            response != null ? (response.toString().length() <= 200 ? response.toString() : "[" + response.toString().length() + "자 생략]") : "null",
            callNumber);
    }

    // Prompt 실행 전 로깅
    public static void logPromptExecution(Prompt prompt) {
        log.debug("LLM Prompt 실행 - ChatOptions: {}",
            prompt.getOptions() != null ? prompt.getOptions().getClass().getSimpleName() : "Function Calling 비활성화");
    }

    // ChatResponse 후 로깅
    public static void logChatResponse(ChatResponse response) {
        if (response != null && response.getResult() != null) {
            var result = response.getResult();

            if (result.getMetadata() != null && result.getMetadata().containsKey("functions")) {
                log.info("LLM Function 호출 - Functions: {}", result.getMetadata().get("functions").toString());
            }
        }
    }

    // 통계 정보 제공
    public static void printStatistics() {
        log.info("LLM Function Call 통계 - 총 호출 횟수: {}", functionCallCounter.get());
    }
}