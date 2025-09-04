package com.compass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

@Profile("test") // "test" 프로필이 활성화될 때만 이 설정을 사용합니다.
@Configuration
public class EmbeddedRedisConfig {

    private static RedisServer redisServer;

    // 생성자를 통해 포트 번호를 주입받습니다.
    public EmbeddedRedisConfig(@Value("${spring.data.redis.port}") int redisPort) {
        // synchronized 블록으로 여러 테스트가 동시에 실행되어도 딱 한 번만 서버가 실행되도록 보장합니다.
        synchronized (EmbeddedRedisConfig.class) {
            if (redisServer == null) {
                redisServer = new RedisServer(redisPort);
                redisServer.start();
                // JVM이 종료될 때 Redis 서버도 함께 종료되도록 설정합니다.
                Runtime.getRuntime().addShutdownHook(new Thread(redisServer::stop));
            }
        }
    }

}