package com.compass.domain.chat.service;

import com.compass.domain.intent.Intent;

public interface FollowUpQuestionService {
    /**
     * CHAT2: 사용자의 의도가 불분명할 때, 의도를 명확히 하기 위한 꼬리 질문을 생성합니다.
     *
     * @param originalMessage 사용자의 원본 메시지
     * @param probableIntent 가장 확률이 높은 것으로 추정되는 의도 (null일 수 있음)
     * @return 사용자에게 전달될 꼬리 질문 문자열
     */
    String askForClarification(String originalMessage, Intent probableIntent);
}
