package com.compass.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

// Function 호출 로깅 Aspect
@Slf4j
@Aspect
@Component
public class FunctionLoggingAspect {

    // 모든 Function 클래스의 apply 메소드를 대상으로 함
    @Pointcut("execution(* com.compass.domain.chat.function..*.apply(..))")
    public void functionApplyMethod() {}

    // Function 실행 전후 로깅
    @Around("functionApplyMethod()")
    public Object logFunctionExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // 시작 로그
        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ LLM Function 호출 시작");
        log.info("║ Function: {}", className);
        log.info("║ Method: {}", methodName);
        log.info("║ 입력 파라미터: {}", Arrays.toString(args));
        log.info("║ 호출 시간: {}", new java.util.Date());
        log.info("╚══════════════════════════════════════════════════════════════");

        // 실행 시간 측정
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Object result = null;
        Exception exception = null;

        try {
            // 실제 Function 실행
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            stopWatch.stop();

            // 종료 로그
            log.info("╔══════════════════════════════════════════════════════════════");
            log.info("║ LLM Function 호출 완료");
            log.info("║ Function: {}", className);
            log.info("║ 실행 시간: {} ms", stopWatch.getTotalTimeMillis());

            if (exception != null) {
                log.error("║ 에러 발생: {}", exception.getMessage());
            } else {
                log.info("║ 성공적으로 완료");
                // 결과가 너무 크지 않으면 로깅
                if (result != null) {
                    String resultStr = result.toString();
                    if (resultStr.length() <= 200) {
                        log.info("║ 반환 값: {}", resultStr);
                    } else {
                        log.info("║ 반환 값: [{}자 - 크기 때문에 생략]", resultStr.length());
                    }
                }
            }
            log.info("╚══════════════════════════════════════════════════════════════");

            // 간략 요약 로그 (분석용)
            if (exception == null) {
                log.debug("[FUNCTION_CALL_SUMMARY] {} | {}ms | SUCCESS",
                    className, stopWatch.getTotalTimeMillis());
            } else {
                log.debug("[FUNCTION_CALL_SUMMARY] {} | {}ms | FAILED: {}",
                    className, stopWatch.getTotalTimeMillis(), exception.getClass().getSimpleName());
            }
        }
    }
}