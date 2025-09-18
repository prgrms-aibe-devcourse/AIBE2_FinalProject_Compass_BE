package com.compass.domain.chat.exception;

import com.compass.domain.chat.model.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// 채팅 도메인 전역 예외 처리기
@Slf4j
@RestControllerAdvice(basePackages = "com.compass.domain.chat")
public class ChatExceptionHandler {

    // 요청 검증 실패
    @ExceptionHandler(InvalidChatRequestException.class)
    public ResponseEntity<ChatResponse> handleInvalidRequest(
            InvalidChatRequestException ex, WebRequest request) {

        log.warn("잘못된 요청: {}, path={}",
            ex.getMessage(), request.getDescription(false));

        return ResponseEntity.badRequest()
            .body(buildErrorResponse("INVALID_REQUEST", ex.getMessage()));
    }

    // 메서드 인자 검증 실패 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ChatResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        var errors = new HashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("검증 실패: errors={}, path={}",
            errors, request.getDescription(false));

        var message = "입력값 검증 실패: " +
            errors.entrySet().stream()
                .map(e -> e.getKey() + " - " + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("알 수 없는 오류");

        return ResponseEntity.badRequest()
            .body(buildErrorResponse("VALIDATION_FAILED", message));
    }

    // 필수 헤더 누락
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ChatResponse> handleMissingHeader(
            MissingRequestHeaderException ex, WebRequest request) {

        log.warn("필수 헤더 누락: {}, path={}",
            ex.getHeaderName(), request.getDescription(false));

        return ResponseEntity.badRequest()
            .body(buildErrorResponse("MISSING_HEADER",
                "필수 헤더가 누락되었습니다: " + ex.getHeaderName()));
    }

    // 인증 실패
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ChatResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.error("인증 실패: {}, path={}",
            ex.getMessage(), request.getDescription(false));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(buildErrorResponse("AUTHENTICATION_FAILED",
                "인증이 필요합니다"));
    }

    // 권한 부족
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ChatResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        log.error("권한 부족: {}, path={}",
            ex.getMessage(), request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(buildErrorResponse("ACCESS_DENIED",
                "해당 리소스에 대한 권한이 없습니다"));
    }

    // IllegalArgumentException 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ChatResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.error("잘못된 인자: {}, path={}",
            ex.getMessage(), request.getDescription(false));

        return ResponseEntity.badRequest()
            .body(buildErrorResponse("ILLEGAL_ARGUMENT", ex.getMessage()));
    }

    // IllegalStateException 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ChatResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {

        log.error("잘못된 상태: {}, path={}",
            ex.getMessage(), request.getDescription(false));

        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(buildErrorResponse("ILLEGAL_STATE", ex.getMessage()));
    }

    // NullPointerException 처리 (개발 중 도움)
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ChatResponse> handleNullPointer(
            NullPointerException ex, WebRequest request) {

        log.error("NPE 발생: path={}", request.getDescription(false), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildErrorResponse("INTERNAL_ERROR",
                "내부 처리 중 오류가 발생했습니다"));
    }

    // 기타 모든 예외 (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponse> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("예상치 못한 오류: type={}, message={}, path={}",
            ex.getClass().getSimpleName(),
            ex.getMessage(),
            request.getDescription(false),
            ex);

        // 프로덕션에서는 상세 메시지 숨기기
        var message = isProduction()
            ? "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            : "오류: " + ex.getMessage();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildErrorResponse("INTERNAL_ERROR", message));
    }

    // 에러 응답 생성
    private ChatResponse buildErrorResponse(String errorCode, String message) {
        var errorData = Map.of(
            "errorCode", errorCode,
            "timestamp", LocalDateTime.now().toString(),
            "message", message
        );

        return ChatResponse.builder()
            .content(message)
            .type("ERROR")
            .data(errorData)
            .nextAction("RETRY")
            .requiresConfirmation(false)
            .build();
    }

    // 프로덕션 환경 체크
    private boolean isProduction() {
        var profile = System.getProperty("spring.profiles.active");
        return "prod".equals(profile) || "production".equals(profile);
    }
}