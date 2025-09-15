package com.compass.domain.chat.entity;

import com.compass.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Follow-up 질문 기록 엔티티
 * CHAT2 팀 핵심 기능 - 사용자 정보 수집을 위한 질문-응답 추적
 */
@Getter
@Entity
@Table(name = "follow_up_questions", indexes = {
    @Index(name = "idx_follow_up_thread", columnList = "thread_id"),
    @Index(name = "idx_follow_up_type", columnList = "question_type"),
    @Index(name = "idx_follow_up_answered", columnList = "is_answered")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FollowUpQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private ChatThread thread;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "question_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private QuestionType questionType;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String options;  // JSON Array: 선택지가 있는 경우

    @Column(name = "is_answered")
    @Builder.Default
    private Boolean isAnswered = false;

    @Column(name = "user_response", columnDefinition = "TEXT")
    private String userResponse;

    @Column(name = "response_timestamp")
    private LocalDateTime responseTimestamp;

    @Column(name = "question_order")
    private Integer questionOrder;  // 질문 순서

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = true;  // 필수 질문 여부

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;  // 재질문 횟수

    @Column(name = "skip_reason", length = 100)
    private String skipReason;  // 건너뛴 이유

    // 편의 메서드
    public void markAsAnswered(String response) {
        this.isAnswered = true;
        this.userResponse = response;
        this.responseTimestamp = LocalDateTime.now();
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public void markAsSkipped(String reason) {
        this.isAnswered = false;
        this.skipReason = reason;
    }

    // Enum 정의
    public enum QuestionType {
        // 필수 정보
        ORIGIN,              // 출발지
        DESTINATION,         // 목적지
        DATES,              // 날짜
        DURATION,           // 기간
        COMPANIONS,         // 동행자
        BUDGET,             // 예산
        TRAVEL_STYLE,       // 여행 스타일
        
        // 추가 정보
        DEPARTURE_TIME,     // 출발 시간
        RETURN_TIME,        // 도착 시간
        ACCOMMODATION,      // 숙소
        INTERESTS,          // 관심사
        FOOD_PREFERENCE,    // 음식 선호
        ACTIVITY_LEVEL,     // 활동 수준
        SPECIAL_NEEDS,      // 특별 요구사항
        
        // 확인 질문
        CONFIRMATION,       // 확인
        CLARIFICATION,      // 명확화
        PREFERENCE,         // 선호도
        
        // 시스템
        GENERAL,           // 일반
        OTHER              // 기타
    }
}