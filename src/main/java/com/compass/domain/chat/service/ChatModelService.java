package com.compass.domain.chat.service;

/**
 * Chat Model Service Interface
 * Spring AI를 통한 LLM 통합 인터페이스
 */
public interface ChatModelService {
    
    /**
     * 사용자 메시지를 처리하고 AI 응답을 반환
     * 
     * @param userMessage 사용자 입력 메시지
     * @return AI 모델의 응답
     */
    String generateResponse(String userMessage);
    
    /**
     * 시스템 프롬프트와 함께 사용자 메시지 처리
     * 
     * @param systemPrompt 시스템 프롬프트
     * @param userMessage 사용자 입력 메시지
     * @return AI 모델의 응답
     */
    String generateResponse(String systemPrompt, String userMessage);
    
    /**
     * 대화 컨텍스트를 포함한 응답 생성 (REQ-LLM-006)
     * 
     * @param threadId 채팅 스레드 ID
     * @param userMessage 사용자 입력 메시지
     * @return AI 모델의 응답
     */
    String generateResponseWithContext(String threadId, String userMessage);
    
    /**
     * 시스템 프롬프트와 대화 컨텍스트를 포함한 응답 생성
     * 
     * @param threadId 채팅 스레드 ID
     * @param systemPrompt 시스템 프롬프트
     * @param userMessage 사용자 입력 메시지
     * @return AI 모델의 응답
     */
    String generateResponseWithContext(String threadId, String systemPrompt, String userMessage);
    
    /**
     * 스트리밍 응답 생성 (향후 구현)
     * 
     * @param userMessage 사용자 입력 메시지
     * @param callback 스트리밍 콜백
     */
    default void generateStreamResponse(String userMessage, StreamCallback callback) {
        throw new UnsupportedOperationException("Streaming not implemented yet");
    }
    
    /**
     * 모델 이름 반환
     * 
     * @return 사용 중인 모델 이름
     */
    String getModelName();
    
    /**
     * 스트리밍 응답을 위한 콜백 인터페이스
     */
    interface StreamCallback {
        void onNext(String chunk);
        void onComplete();
        void onError(Throwable error);
    }
}