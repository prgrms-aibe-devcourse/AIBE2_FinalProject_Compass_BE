package com.compass.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

@Profile("test")
@Configuration
public class EmbeddedRedisConfig {

    private RedisServer redisServer;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @PostConstruct
    public void startRedis() {
        try {
            // 포트가 사용 중인 경우 사용 가능한 포트 찾기
            int availablePort = findAvailablePort(redisPort);
            
            redisServer = RedisServer.builder()
                    .port(availablePort)
                    .setting("maxmemory 64M")  // CI 환경을 위해 메모리 줄임
                    .setting("save \"\"")      // 디스크 저장 비활성화
                    .setting("appendonly no")  // AOF 비활성화
                    .build();
            
            redisServer.start();
            System.out.println("Embedded Redis started on port: " + availablePort);
            
        } catch (Exception e) {
            System.err.println("Failed to start Embedded Redis: " + e.getMessage());
            // CI 환경에서 Redis 시작 실패는 치명적이지 않도록 처리
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stopRedis() {
        try {
            if (redisServer != null && redisServer.isActive()) {
                redisServer.stop();
                System.out.println("Embedded Redis stopped");
            }
        } catch (Exception e) {
            System.err.println("Error stopping Embedded Redis: " + e.getMessage());
        }
    }
    
    private int findAvailablePort(int preferredPort) {
        // 먼저 선호하는 포트 시도
        if (isPortAvailable(preferredPort)) {
            return preferredPort;
        }
        
        // 사용 가능한 포트 찾기
        for (int port = preferredPort + 1; port < preferredPort + 100; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        
        // 모두 실패하면 시스템이 할당하는 포트 사용
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find available port", e);
        }
    }
    
    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
