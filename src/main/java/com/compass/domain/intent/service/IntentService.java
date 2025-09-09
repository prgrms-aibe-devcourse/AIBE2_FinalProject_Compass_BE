package com.compass.domain.intent.service;

import com.compass.domain.intent.Intent;

/**
 * 사용자 메시지의 의도를 분류하는 서비스
 */
public interface IntentService {

    /**
     * 메시지를 분석하여 의도를 분류합니다.
     *
     * @param message 사용자 메시지
     * @return 분류된 의도 (Intent Enum)
     */
    Intent classifyIntent(String message);
}
