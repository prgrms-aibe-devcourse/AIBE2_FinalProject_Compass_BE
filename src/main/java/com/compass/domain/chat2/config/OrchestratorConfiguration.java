package com.compass.domain.chat2.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrchestratorConfiguration - Function 자동 수집
 * REQ-CHAT2-003: Function Calling 기본 구조 구현
 * 
 * 모든 도메인의 Function을 자동으로 수집하고 관리하는 설정 클래스
 */
@Configuration
@Slf4j
public class OrchestratorConfiguration {
    
    /**
     * 모든 Function을 저장하는 레지스트리
     */
    private final Map<String, Object> functionRegistry = new ConcurrentHashMap<>();
    
    /**
     * Function 레지스트리 Bean 등록
     * 
     * @return Function 레지스트리
     */
    @Bean("functionRegistry")
    public Map<String, Object> functionRegistry() {
        log.info("Function 레지스트리 초기화");
        return functionRegistry;
    }
    
    /**
     * Function 등록 메서드
     * 
     * @param functionName Function 이름
     * @param function Function 객체
     */
    public void registerFunction(String functionName, Object function) {
        log.debug("Function 등록: {}", functionName);
        functionRegistry.put(functionName, function);
    }
    
    /**
     * Function 조회 메서드
     * 
     * @param functionName Function 이름
     * @return Function 객체
     */
    public Object getFunction(String functionName) {
        return functionRegistry.get(functionName);
    }
    
    /**
     * 등록된 모든 Function 목록 조회
     * 
     * @return Function 이름 목록
     */
    public List<String> getRegisteredFunctionNames() {
        return List.copyOf(functionRegistry.keySet());
    }
    
    /**
     * Function 존재 여부 확인
     * 
     * @param functionName Function 이름
     * @return 존재 여부
     */
    public boolean hasFunction(String functionName) {
        return functionRegistry.containsKey(functionName);
    }
    
    /**
     * 등록된 Function 개수 조회
     * 
     * @return Function 개수
     */
    public int getFunctionCount() {
        return functionRegistry.size();
    }
    
    /**
     * Function 레지스트리 초기화
     */
    public void clearRegistry() {
        log.info("Function 레지스트리 초기화");
        functionRegistry.clear();
    }
    
    /**
     * 특정 도메인의 Function들만 조회
     * 
     * @param domainPrefix 도메인 접두사 (예: "trip", "user", "media")
     * @return 해당 도메인의 Function 이름 목록
     */
    public List<String> getFunctionsByDomain(String domainPrefix) {
        return functionRegistry.keySet().stream()
                .filter(name -> name.toLowerCase().contains(domainPrefix.toLowerCase()))
                .toList();
    }
    
    /**
     * Function 등록 상태 로깅
     */
    @Bean
    @DependsOn("functionRegistry")
    public String logFunctionRegistryStatus() {
        log.info("=== Function 레지스트리 상태 ===");
        log.info("등록된 Function 개수: {}", getFunctionCount());
        log.info("등록된 Function 목록: {}", getRegisteredFunctionNames());
        
        // 도메인별 Function 개수 로깅
        log.info("TRIP 도메인 Function: {}", getFunctionsByDomain("trip"));
        log.info("USER 도메인 Function: {}", getFunctionsByDomain("user"));
        log.info("MEDIA 도메인 Function: {}", getFunctionsByDomain("media"));
        log.info("CHAT1 도메인 Function: {}", getFunctionsByDomain("chat"));
        log.info("CHAT2 도메인 Function: {}", getFunctionsByDomain("chat2"));
        
        return "Function 레지스트리 초기화 완료";
    }
}
