package com.compass.domain.chat.service.external;

import com.compass.domain.chat.model.enums.DocumentType;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.ImageContext;
import com.google.cloud.vision.v1.ImageSource;
import com.google.protobuf.ByteString;
import com.google.api.gax.retrying.RetrySettings;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.threeten.bp.Duration;

@Slf4j
@Component
public class OCRClient {

    private static final Pattern FLIGHT_CODE = Pattern.compile("\\b[A-Z]{2}\\d{2,4}\\b");
    private static final Pattern AIRLINE_KEYWORD = Pattern.compile("BOARDING PASS|E-TICKET", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOTEL_KEYWORD = Pattern.compile("hotel|check[- ]?in|check[- ]?out|room|reservation|confirmation", Pattern.CASE_INSENSITIVE);
    private static final List<String> LANGUAGE_HINTS = List.of("ko", "en", "ja", "zh");
    private static final int RATE_LIMIT_PER_MINUTE = 1800;
    private static final int MAX_ATTEMPTS = 4;
    private static final int MIN_ACCEPTABLE_LENGTH = 50;
    private static final long INITIAL_BACKOFF_MS = 200L;
    private static final long MAX_BACKOFF_MS = 2_000L;
    private static final long RATE_LIMIT_WAIT_MS = 5_000L;
    private static final long CACHE_TTL_MILLIS = java.time.Duration.ofMinutes(5).toMillis();
    private static final int MAX_CACHE_ENTRIES = 1_000;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> evictionQueue = new ConcurrentLinkedQueue<>();
    private final Semaphore rateLimiter = new Semaphore(RATE_LIMIT_PER_MINUTE, true);
    private final ScheduledExecutorService rateResetScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        var thread = new Thread(r, "ocr-rate-reset");
        thread.setDaemon(true);
        return thread;
    });

    private ImageAnnotatorClient client;

    @PostConstruct
    void init() {
        try {
            log.info("OCRClient 초기화 시도 중...");
            var settingsBuilder = ImageAnnotatorSettings.newBuilder();
            RetrySettings retrySettings = settingsBuilder.batchAnnotateImagesSettings().getRetrySettings().toBuilder()
                    .setTotalTimeout(Duration.ofSeconds(30))
                    .build();
            settingsBuilder
                    .batchAnnotateImagesSettings()
                    .setRetrySettings(retrySettings);
            client = ImageAnnotatorClient.create(settingsBuilder.build());
            log.info("OCRClient 초기화 성공");
        } catch (IOException e) {
            log.warn("Vision API 클라이언트 초기화 실패 - OCR 기능 비활성화: {}", e.getMessage());
            client = null; // null로 설정하여 OCR 기능 비활성화
        }
        rateResetScheduler.scheduleAtFixedRate(this::resetRateLimiter, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
        rateResetScheduler.shutdownNow();
    }

    public String extractText(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("이미지 데이터가 비어 있습니다.");
        }
        var cacheKey = "bytes:" + hashBytes(data);
        var image = Image.newBuilder().setContent(ByteString.copyFrom(data)).build();
        return annotate(image, cacheKey);
    }

    public String extractTextFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 필요합니다.");
        }
        var cacheKey = "url:" + imageUrl.trim();
        var image = Image.newBuilder()
                .setSource(ImageSource.newBuilder().setImageUri(imageUrl).build())
                .build();
        return annotate(image, cacheKey);
    }

    public DocumentType detectDocument(String text) {
        if (text == null || text.isBlank()) {
            return DocumentType.UNKNOWN;
        }
        if (AIRLINE_KEYWORD.matcher(text).find() || FLIGHT_CODE.matcher(text).find()) {
            return DocumentType.FLIGHT_RESERVATION;
        }
        if (HOTEL_KEYWORD.matcher(text).find()) {
            return DocumentType.HOTEL_RESERVATION;
        }
        return DocumentType.UNKNOWN;
    }

    private String annotate(Image image, String cacheKey) {
        if (client == null) {
            log.warn("OCRClient가 초기화되지 않음 - OCR 기능 비활성화됨");
            return "";
        }
        var cached = getCached(cacheKey);
        if (cached != null) {
            return cached;
        }
        long backoff = INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            acquirePermit();
            try {
                var request = buildRequest(image);
                var response = client.batchAnnotateImages(List.of(request)).getResponses(0);
                var text = parseResponse(response);
                if (text.length() > MIN_ACCEPTABLE_LENGTH || attempt == MAX_ATTEMPTS) {
                    cache(cacheKey, text);
                    return text;
                }
                log.debug("OCR 텍스트 길이가 짧아 재시도합니다. attempt={}", attempt);
            } catch (Exception ex) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException("OCR 처리 중 오류가 발생했습니다.", ex);
                }
                log.warn("OCR 호출 실패 - attempt: {}", attempt, ex);
            }
            sleep(backoff);
            backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
        }
        return "";
    }

    private AnnotateImageRequest buildRequest(Image image) {
        var feature = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();
        var context = ImageContext.newBuilder()
                .addAllLanguageHints(LANGUAGE_HINTS)
                .build();
        return AnnotateImageRequest.newBuilder()
                .setImage(image)
                .addFeatures(feature)
                .setImageContext(context)
                .build();
    }

    private String parseResponse(AnnotateImageResponse response) {
        if (response.hasError()) {
            log.warn("OCR 오류: {}", response.getError().getMessage());
            return "";
        }
        var annotation = response.getFullTextAnnotation();
        return annotation == null ? "" : annotation.getText().trim();
    }

    private void acquirePermit() {
        try {
            if (!rateLimiter.tryAcquire(RATE_LIMIT_WAIT_MS, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Vision API 요청 한도를 초과했습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Vision API 요청 대기 중 인터럽트", e);
        }
    }

    private void resetRateLimiter() {
        int permitsToRelease = RATE_LIMIT_PER_MINUTE - rateLimiter.availablePermits();
        if (permitsToRelease > 0) {
            rateLimiter.release(permitsToRelease);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OCR 재시도 대기 중 인터럽트", e);
        }
    }

    private String getCached(String cacheKey) {
        if (cacheKey == null) {
            return null;
        }
        var entry = cache.get(cacheKey);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAt() < System.currentTimeMillis()) {
            cache.remove(cacheKey);
            return null;
        }
        return entry.text();
    }

    private void cache(String cacheKey, String text) {
        if (cacheKey == null || text == null) {
            return;
        }
        cache.put(cacheKey, new CacheEntry(text, System.currentTimeMillis() + CACHE_TTL_MILLIS));
        evictionQueue.add(cacheKey);
        if (cache.size() > MAX_CACHE_ENTRIES) {
            var evictKey = evictionQueue.poll();
            if (evictKey != null) {
                cache.remove(evictKey);
            }
        }
    }

    private String hashBytes(byte[] data) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(data);
            var builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시를 초기화할 수 없습니다.", e);
        }
    }

    private record CacheEntry(String text, long expiresAt) {}
}
