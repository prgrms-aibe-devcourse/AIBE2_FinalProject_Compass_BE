package com.compass.domain.chat.engine;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.entity.TravelInfoCollectionState;

/**
 * 질문 플로우 엔진 인터페이스
 * REQ-FOLLOW-001: 순차적 질문 진행을 위한 플로우 엔진
 */
public interface QuestionFlowEngine {
    
    /**
     * 다음 질문 생성
     * @param state 현재 수집 상태
     * @return 다음 질문 DTO
     */
    FollowUpQuestionDto generateNextQuestion(TravelInfoCollectionState state);
    
    /**
     * 응답 처리 및 상태 업데이트
     * @param state 현재 수집 상태
     * @param userResponse 사용자 응답
     * @return 업데이트된 상태
     */
    TravelInfoCollectionState processResponse(TravelInfoCollectionState state, String userResponse);
    
    /**
     * 플로우 완료 여부 확인
     * @param state 현재 수집 상태
     * @return 완료 여부
     */
    boolean isFlowComplete(TravelInfoCollectionState state);
    
    /**
     * 현재 단계 건너뛰기 가능 여부
     * @param state 현재 수집 상태
     * @return 건너뛰기 가능 여부
     */
    boolean canSkipCurrentStep(TravelInfoCollectionState state);
    
    /**
     * 플로우 검증
     * @param state 현재 수집 상태
     * @return 검증 성공 여부
     */
    boolean validateFlow(TravelInfoCollectionState state);
    
    /**
     * 필수 질문 개수 반환
     * @return 필수 질문 개수
     */
    int getRequiredQuestionCount();
    
    /**
     * 현재 진행 단계 반환
     * @param state 현재 수집 상태
     * @return 현재 단계 번호 (1부터 시작)
     */
    int getCurrentStepNumber(TravelInfoCollectionState state);
}