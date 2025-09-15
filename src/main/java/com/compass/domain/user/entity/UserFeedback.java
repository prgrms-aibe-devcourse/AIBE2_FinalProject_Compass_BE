package com.compass.domain.user.entity;

import com.compass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_feedbacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer satisfaction; // 1-5점 만족도

    @Column(columnDefinition = "TEXT")
    private String comment; // 개선사항

    private Boolean revisitIntent; // 재방문 의향

    @Builder
    public UserFeedback(User user, Integer satisfaction, String comment, Boolean revisitIntent) {
        this.user = user;
        this.satisfaction = satisfaction;
        this.comment = comment;
        this.revisitIntent = revisitIntent;
    }
}