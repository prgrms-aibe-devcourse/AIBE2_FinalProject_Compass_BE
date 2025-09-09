package com.compass.domain.agent;

/**
 * 특정 의도를 처리하는 에이전트의 공통 인터페이스
 */
public interface Agent {

    /**
     * 주어진 메시지를 처리하고 응답을 생성합니다.
     *
     * @param message 사용자 메시지
     * @return 에이전트가 생성한 응답 문자열
     */
    String execute(String message);

}
