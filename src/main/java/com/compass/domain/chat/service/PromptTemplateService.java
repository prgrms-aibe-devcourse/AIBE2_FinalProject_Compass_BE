package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.PromptRequest;
import com.compass.domain.chat.dto.PromptResponse;

import java.util.Map;
import java.util.Set;

/**
 * 프롬프트 템플릿 관리 및 사용을 위한 서비스 인터페이스
 */
public interface PromptTemplateService {
    
    /**
     * 템플릿으로부터 프롬프트 생성
     * 
     * @param templateName 사용할 템플릿 이름
     * @param parameters 템플릿에 채울 파라미터들
     * @return 생성된 프롬프트 문자열
     */
    String buildPrompt(String templateName, Map<String, Object> parameters);
    
    /**
     * 컨텍스트 정보를 추가하여 강화된 프롬프트 생성
     * 
     * @param request 템플릿과 파라미터를 포함한 프롬프트 요청
     * @return 생성된 프롬프트와 메타데이터를 포함한 응답
     */
    PromptResponse buildEnrichedPrompt(PromptRequest request);
    
    /**
     * 사용 가능한 템플릿 이름 목록 조회
     * 
     * @return 템플릿 이름 Set
     */
    Set<String> getAvailableTemplates();
    
    /**
     * 템플릿 상세 정보 조회
     * 
     * @param templateName 조회할 템플릿 이름
     * @return 필수 파라미터를 포함한 템플릿 상세 정보
     */
    Map<String, Object> getTemplateDetails(String templateName);
    
    /**
     * 사용자 쿼리에 가장 적합한 템플릿 자동 선택
     * 
     * @param userQuery 사용자 입력 쿼리
     * @param context 추가 컨텍스트 정보
     * @return 추천된 템플릿 이름
     */
    String selectTemplate(String userQuery, Map<String, Object> context);
    
    /**
     * 사용자 입력에서 템플릿에 필요한 파라미터 자동 추출
     * 
     * @param templateName 사용할 템플릿 이름
     * @param userInput 사용자 입력
     * @param context 추가 컨텍스트 정보
     * @return 추출된 파라미터 Map
     */
    Map<String, Object> extractParameters(String templateName, String userInput, Map<String, Object> context);
}