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
        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ Spring AI Function Call 로깅 시스템 초기화");
        log.info("║ LLM Function Calling 이벤트를 모니터링합니다");
        log.info("╚══════════════════════════════════════════════════════════════");
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

        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ [LLM Function Calling Event #{}]", callNumber);
        log.info("║ Function Name: {}", functionName);
        log.info("║ Request: {}", request != null ? request.toString() : "null");

        if (response != null) {
            String responseStr = response.toString();
            if (responseStr.length() <= 200) {
                log.info("║ Response: {}", responseStr);
            } else {
                log.info("║ Response: [{}자 - 너무 길어서 생략]", responseStr.length());
            }
        }

        log.info("║ Total Function Calls: {}", callNumber);
        log.info("╚══════════════════════════════════════════════════════════════");
    }

    // Prompt 실행 전 로깅
    public static void logPromptExecution(Prompt prompt) {
        log.debug("╔══════════════════════════════════════════════════════════════");
        log.debug("║ LLM Prompt 실행");

        if (prompt.getOptions() != null) {
            log.debug("║ ChatOptions 설정됨: {}", prompt.getOptions().getClass().getSimpleName());
        } else {
            log.debug("║ Function Calling 비활성화 상태");
        }

        log.debug("╚══════════════════════════════════════════════════════════════");
    }

    // ChatResponse 후 로깅
    public static void logChatResponse(ChatResponse response) {
        if (response != null && response.getResult() != null) {
            var result = response.getResult();

            if (result.getMetadata() != null && result.getMetadata().containsKey("functions")) {
                log.info("╔══════════════════════════════════════════════════════════════");
                log.info("║ LLM이 Function을 호출했습니다!");
                log.info("║ 호출된 Functions: {}", result.getMetadata().get("functions").toString());
                log.info("╚══════════════════════════════════════════════════════════════");
            }
        }
    }

    // 통계 정보 제공
    public static void printStatistics() {
        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ LLM Function Call 통계");
        log.info("║ 총 Function 호출 횟수: {}", functionCallCounter.get());
        log.info("╚══════════════════════════════════════════════════════════════");
    }
}