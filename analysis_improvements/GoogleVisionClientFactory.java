package com.compass.domain.media.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Factory for managing Google Vision API client connections with connection pooling
 */
@Slf4j
@Component
public class GoogleVisionClientFactory {
    
    @Value("${google.vision.client.pool.size:5}")
    private int poolSize;
    
    @Value("${google.vision.client.pool.timeout:30}")
    private int timeoutSeconds;
    
    private BlockingQueue<ImageAnnotatorClient> clientPool;
    private volatile boolean initialized = false;
    
    @PostConstruct
    public void initializePool() {
        try {
            clientPool = new LinkedBlockingQueue<>(poolSize);
            
            // Pre-populate the pool with clients
            for (int i = 0; i < poolSize; i++) {
                ImageAnnotatorClient client = createNewClient();
                clientPool.offer(client);
            }
            
            initialized = true;
            log.info("Google Vision API 클라이언트 풀 초기화 완료 - 풀 크기: {}", poolSize);
            
        } catch (IOException e) {
            log.error("Google Vision API 클라이언트 풀 초기화 실패", e);
            throw new RuntimeException("Failed to initialize Google Vision client pool", e);
        }
    }
    
    /**
     * Gets a client from the pool. Creates a new one if pool is empty.
     * 
     * @return ImageAnnotatorClient instance
     * @throws IOException if client creation fails
     */
    public ImageAnnotatorClient getClient() throws IOException {
        if (!initialized) {
            return createNewClient();
        }
        
        try {
            // Try to get from pool with timeout
            ImageAnnotatorClient client = clientPool.poll();
            if (client != null) {
                return client;
            }
            
            // Pool is empty, create new client
            log.debug("클라이언트 풀이 비어있음, 새 클라이언트 생성");
            return createNewClient();
            
        } catch (Exception e) {
            log.error("클라이언트 풀에서 클라이언트 획득 실패", e);
            throw new IOException("Failed to get client from pool", e);
        }
    }
    
    /**
     * Returns a client to the pool for reuse
     * 
     * @param client the client to return
     */
    public void returnClient(ImageAnnotatorClient client) {
        if (client != null && initialized) {
            boolean offered = clientPool.offer(client);
            if (!offered) {
                // Pool is full, close the client
                log.debug("클라이언트 풀이 가득참, 클라이언트 종료");
                try {
                    client.close();
                } catch (Exception e) {
                    log.warn("클라이언트 종료 중 오류", e);
                }
            }
        }
    }
    
    /**
     * Creates a new ImageAnnotatorClient with optimal settings
     * 
     * @return configured ImageAnnotatorClient
     * @throws IOException if client creation fails
     */
    private ImageAnnotatorClient createNewClient() throws IOException {
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
            
            return ImageAnnotatorClient.create(settings);
            
        } catch (IOException e) {
            log.error("Google Vision API 클라이언트 생성 실패", e);
            throw new IOException("Failed to create Google Vision API client", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (clientPool != null) {
            log.info("Google Vision API 클라이언트 풀 정리 시작");
            
            while (!clientPool.isEmpty()) {
                ImageAnnotatorClient client = clientPool.poll();
                if (client != null) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        log.warn("클라이언트 종료 중 오류", e);
                    }
                }
            }
            
            log.info("Google Vision API 클라이언트 풀 정리 완료");
        }
    }
    
    /**
     * Wrapper for automatic client return to pool
     */
    public interface ClientCallback<T> {
        T execute(ImageAnnotatorClient client) throws Exception;
    }
    
    /**
     * Executes a callback with automatic client management
     * 
     * @param callback the callback to execute
     * @return the result of the callback
     * @throws IOException if an error occurs
     */
    public <T> T executeWithClient(ClientCallback<T> callback) throws IOException {
        ImageAnnotatorClient client = getClient();
        try {
            return callback.execute(client);
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Callback execution failed", e);
        } finally {
            returnClient(client);
        }
    }
}