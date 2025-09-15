package com.compass.domain.chat2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UnifiedChatRequest - 통합 채팅 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedChatRequest {

    /**
     * 사용자 메시지 (필수)
     */
    @NotBlank(message = "메시지는 필수입니다")
    private String message;

    /**
     * 대화 스레드 ID (선택)
     * null인 경우 새로운 스레드 생성
     */
    private String threadId;

    /**
     * 이미지 URL (선택)
     * OCR 처리가 필요한 경우
     */
    private String imageUrl;

    /**
     * 컨텍스트 정보 (선택)
     * 이전 대화나 추가 정보
     */
    private String context;
}