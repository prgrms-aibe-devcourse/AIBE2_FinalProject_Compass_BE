package com.compass.domain.chat.controller;

import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.service.external.OCRClient;
import com.compass.domain.chat.service.external.S3Client;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR Test", description = "Endpoints that help manual OCR tests during development")
public class OCRTestController {

    private static final String DEFAULT_DIRECTORY = "travel-images";
    private static final String DEFAULT_FILE_NAME = "ocr-test-image";

    private final OCRClient ocrClient;
    private final S3Client s3Client;

    @GetMapping(value = "/page", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Serve an in-browser OCR test page")
    public ResponseEntity<Resource> serveTestPage() {
        Resource resource = new ClassPathResource("static/ocr-test.html");
        if (!resource.exists()) {
            var html = buildInlineTestPage();
            resource = new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }

    @PostMapping(path = "/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Decode Base64 payload and run OCR")
    public ResponseEntity<Map<String, Object>> extractText(@RequestBody Base64ImageRequest request) {
        if (!StringUtils.hasText(request.imageData())) {
            return ResponseEntity.badRequest().body(Map.of("error", "imageData is required"));
        }
        try {
            var imageBytes = decodeBase64Payload(request.imageData());
            var result = ocrClient.extractDetailed(imageBytes);
            var type = ocrClient.detectDocument(result.text());
            log.info("OCR text endpoint processed payload length={} type={} confidence={}% file={}",
                    result.text().length(), type, formatConfidencePercent(result.confidence()), request.fileName());
            return ResponseEntity.ok(buildResponse(result, type, null, null, null));
        } catch (Exception ex) {
            log.error("OCR text endpoint failed", ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "OCR processing failed: " + ex.getMessage()));
        }
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an image via multipart/form-data and run OCR")
    public ResponseEntity<Map<String, Object>> extractTextFromUpload(@RequestParam("file") MultipartFile file,
                                                                     @RequestParam(value = "directory", required = false) String directory,
                                                                     @RequestParam(value = "upload", defaultValue = "false") boolean uploadToS3,
                                                                     @RequestParam(value = "ocrMode", defaultValue = "bytes") String ocrMode) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file is required"));
        }
        try {
            var bytes = file.getBytes();
            S3Client.S3UploadResult uploadResult = null;
            if (uploadToS3) {
                var targetDir = StringUtils.hasText(directory) ? directory : DEFAULT_DIRECTORY;
                uploadResult = s3Client.upload(bytes, targetDir, safeFileName(file.getOriginalFilename()), file.getContentType());
            }

            boolean useUrl = uploadResult != null && "url".equalsIgnoreCase(ocrMode);
            var result = useUrl
                    ? ocrClient.extractDetailedFromUrl(uploadResult.presignedUrl())
                    : ocrClient.extractDetailed(bytes);
            var type = ocrClient.detectDocument(result.text());

            String imageUrl = uploadResult != null ? uploadResult.publicUrl() : null;
            String objectKey = uploadResult != null ? uploadResult.objectKey() : null;
            String presignedUrl = uploadResult != null ? uploadResult.presignedUrl() : null;

            log.info("OCR upload endpoint processed file={} length={} type={} confidence={}% uploaded={} viaUrl={}",
                    file.getOriginalFilename(), result.text().length(), type, formatConfidencePercent(result.confidence()), uploadToS3, useUrl);
            return ResponseEntity.ok(buildResponse(result, type, imageUrl, objectKey, presignedUrl));
        } catch (Exception ex) {
            log.error("OCR multipart endpoint failed", ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "OCR processing failed: " + ex.getMessage()));
        }
    }

    @PostMapping(path = "/full", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Upload Base64 payload to S3 and perform OCR")
    public ResponseEntity<Map<String, Object>> processAndUpload(@RequestBody Base64ImageRequest request) {
        if (!StringUtils.hasText(request.imageData())) {
            return ResponseEntity.badRequest().body(Map.of("error", "imageData is required"));
        }
        try {
            var imageBytes = decodeBase64Payload(request.imageData());
            var fileName = safeFileName(request.fileName());
            var contentType = StringUtils.hasText(request.contentType()) ? request.contentType() : MediaType.IMAGE_PNG_VALUE;
            var uploadResult = s3Client.upload(imageBytes, DEFAULT_DIRECTORY, fileName, contentType);

            boolean useUrl = "url".equalsIgnoreCase(request.ocrMode());
            var result = useUrl
                    ? ocrClient.extractDetailedFromUrl(uploadResult.presignedUrl())
                    : ocrClient.extractDetailed(imageBytes);
            var type = ocrClient.detectDocument(result.text());
            log.info("OCR full endpoint uploaded file={} url={} type={} confidence={}% length={} viaUrl={}",
                    fileName, uploadResult.publicUrl(), type, formatConfidencePercent(result.confidence()), result.text().length(), useUrl);
            return ResponseEntity.ok(buildResponse(result, type, uploadResult.publicUrl(), uploadResult.objectKey(), uploadResult.presignedUrl()));
        } catch (Exception ex) {
            log.error("OCR full endpoint failed", ex);
            return ResponseEntity.internalServerError().body(Map.of("error", "OCR processing failed: " + ex.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Check test endpoints availability")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("ocrService", "available");
        status.put("s3Service", "available");
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }

    private byte[] decodeBase64Payload(String payload) {
        var trimmed = payload.trim();
        int commaIndex = trimmed.indexOf(',');
        if (trimmed.startsWith("data:image") && commaIndex > -1) {
            trimmed = trimmed.substring(commaIndex + 1);
        }
        return Base64.getDecoder().decode(trimmed);
    }

    private Map<String, Object> buildResponse(OCRClient.OcrResult result, DocumentType documentType, String imageUrl, String objectKey, String presignedUrl) {
        Map<String, Object> response = new HashMap<>();
        response.put("extractedText", result.text());
        response.put("documentType", documentType.toString());
        response.put("textLength", result.text().length());
        response.put("confidence", result.confidence());
        response.put("confidencePercent", formatConfidencePercent(result.confidence()));
        if (imageUrl != null) {
            response.put("imageUrl", imageUrl);
        }
        if (objectKey != null) {
            response.put("objectKey", objectKey);
        }
        if (presignedUrl != null) {
            response.put("presignedUrl", presignedUrl);
        }
        response.put("status", "success");
        return response;
    }

    private String formatConfidencePercent(float confidence) {
        return String.format("%.2f", confidence * 100);
    }

    private String safeFileName(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return DEFAULT_FILE_NAME + ".png";
        }
        var sanitized = candidate.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isBlank() ? DEFAULT_FILE_NAME + ".png" : sanitized;
    }

    private String buildInlineTestPage() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <title>OCR Test Console</title>
                    <style>
                        body { font-family: system-ui, -apple-system, sans-serif; margin: 3rem; color: #1f2933; background: #f8fafc; }
                        main { max-width: 720px; margin: 0 auto; }
                        pre { background: #0f172a; color: #f8fafc; padding: 1.5rem; border-radius: 12px; }
                        a { color: #2563eb; }
                        .tip { margin-top: 2rem; font-size: 0.95rem; color: #475569; }
                    </style>
                </head>
                <body>
                    <main>
                        <h1>OCR Test Console</h1>
                        <p>The static resource (<code>static/ocr-test.html</code>) is missing, so this fallback page is shown. Deploy the static HTML or check <code>/src/main/resources/static/ocr-test.html</code>.</p>
                        <div class="tip">
                            <p>Useful endpoints:</p>
                            <ul>
                                <li><code>POST /api/test/ocr/upload</code> - multipart upload + OCR</li>
                                <li><code>POST /api/test/ocr/text</code> - Base64 OCR</li>
                                <li><code>POST /api/test/ocr/full</code> - Base64 + S3 upload</li>
                            </ul>
                        </div>
                        <pre>{
  \"message\": \"Static OCR test page not found.\",
  \"hint\": \"Create the HTML under src/main/resources/static or deploy it with the service.\"
}</pre>
                    </main>
                </body>
                </html>
                """;
    }

    public static final class Base64ImageRequest {
        private final String imageData;
        private final String fileName;
        private final String contentType;
        private final String ocrMode;

        @JsonCreator
        public Base64ImageRequest(@JsonProperty("imageData") String imageData,
                                  @JsonProperty("fileName") String fileName,
                                  @JsonProperty("contentType") String contentType,
                                  @JsonProperty("ocrMode") String ocrMode) {
            this.imageData = imageData;
            this.fileName = fileName;
            this.contentType = contentType;
            this.ocrMode = ocrMode == null ? "bytes" : ocrMode;
        }

        public String imageData() {
            return imageData;
        }

        public String fileName() {
            return fileName;
        }

        public String contentType() {
            return contentType;
        }

        public String ocrMode() {
            return ocrMode;
        }
    }
}
