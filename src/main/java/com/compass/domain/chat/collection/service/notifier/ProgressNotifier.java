package com.compass.domain.chat.collection.service.notifier;

// 정보 수집 진행률 실시간 알림 인터페이스
public interface ProgressNotifier {

    // 특정 대화(thread)의 진행률 변경을 알림
    void notify(String threadId, int progress);

    // 특정 대화의 진행 상황을 구독 (구현 방식에 따라 필요 없을 수 있음)
    void subscribe(String threadId);
}