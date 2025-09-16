#!/bin/bash

SRC_ROOT="/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE/src/main/java/com/compass"

echo "📄 Creating skeleton files..."

# PhaseManager
cat > $SRC_ROOT/domain/chat/orchestrator/PhaseManager.java << 'JAVA'
package com.compass.domain.chat.orchestrator;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: 구현 필요
 * 담당: Chat2 개발자
 * 
 * 주요 기능:
 * - 5개 Phase 관리
 * - Phase 전환 로직
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhaseManager {
    // TODO: 구현
}
JAVA

# UnifiedChatController
cat > $SRC_ROOT/domain/chat/controller/UnifiedChatController.java << 'JAVA'
package com.compass.domain.chat.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: 구현 필요
 * 담당: Chat2 개발자
 *
 * 엔드포인트: POST /api/chat/unified
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class UnifiedChatController {
    // TODO: 구현
}
JAVA

# Intent enum
cat > $SRC_ROOT/domain/chat/model/enums/Intent.java << 'JAVA'
package com.compass.domain.chat.model.enums;

/**
 * TODO: 구현 필요
 * 담당: Chat2 개발자
 */
public enum Intent {
    TRAVEL_PLANNING,
    INFORMATION_COLLECTION,
    IMAGE_UPLOAD,
    GENERAL_QUESTION,
    QUICK_INPUT,
    DESTINATION_SEARCH,
    RESERVATION_PROCESSING,
    API_USAGE_CHECK,
    UNKNOWN
}
JAVA

# TravelPhase enum
cat > $SRC_ROOT/domain/chat/model/enums/TravelPhase.java << 'JAVA'
package com.compass.domain.chat.model.enums;

/**
 * TODO: 구현 필요
 * 담당: Chat2 개발자
 */
public enum TravelPhase {
    INITIALIZATION,
    INFORMATION_COLLECTION,
    PLAN_GENERATION,
    FEEDBACK_REFINEMENT,
    COMPLETION
}
JAVA

# FunctionConfiguration
cat > $SRC_ROOT/domain/chat/function/config/FunctionConfiguration.java << 'JAVA'
package com.compass.domain.chat.function.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import lombok.RequiredArgsConstructor;

/**
 * TODO: 구현 필요
 * 담당: Chat2 개발자
 *
 * Function Bean 등록
 */
@Configuration
@RequiredArgsConstructor
public class FunctionConfiguration {
    // TODO: @Bean 메서드들 구현
}
JAVA

# TravelContext
cat > $SRC_ROOT/domain/chat/model/context/TravelContext.java << 'JAVA'
package com.compass.domain.chat.model.context;

import lombok.Data;
import lombok.Builder;

/**
 * TODO: 구현 필요
 * 담당: Chat2 개발자
 *
 * 대화 컨텍스트 관리
 */
@Data
@Builder
public class TravelContext {
    private String threadId;
    private String userId;
    private String currentPhase;
    private Object collectedInfo;
    private Object travelPlan;
    // TODO: 추가 필드 구현
}
JAVA

echo "✅ Core skeleton files created!"

# User 개발자 파일들
echo "📄 Creating User developer files..."

cat > $SRC_ROOT/domain/chat/function/collection/ShowQuickInputFormFunction.java << 'JAVA'
package com.compass.domain.chat.function.collection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TODO: 구현 필요
 * 담당: User 개발자
 *
 * 빠른 입력 폼 표시 Function
 */
@Slf4j
@RequiredArgsConstructor
public class ShowQuickInputFormFunction {
    // TODO: 구현
}
JAVA

# Trip 개발자 파일
echo "📄 Creating Trip developer files..."

cat > $SRC_ROOT/domain/chat/model/request/ChatRequest.java << 'JAVA'
package com.compass.domain.chat.model.request;

import lombok.Data;

/**
 * TODO: 구현 필요
 * 담당: Trip 개발자
 */
@Data
public class ChatRequest {
    private String message;
    private String threadId;
    private String userId;
    private Object metadata;
}
JAVA

cat > $SRC_ROOT/domain/chat/model/response/ChatResponse.java << 'JAVA'
package com.compass.domain.chat.model.response;

import lombok.Data;
import lombok.Builder;

/**
 * TODO: 구현 필요
 * 담당: Trip 개발자
 */
@Data
@Builder
public class ChatResponse {
    private String content;
    private String type;
    private Object data;
    private String nextAction;
}
JAVA

echo "✅ All skeleton files created successfully!"
