package com.compass.domain.media.util;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized factory for creating consistent metadata across the media domain
 */
public final class MediaMetadataFactory {
    
    private MediaMetadataFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Creates initial metadata for a newly uploaded file
     */
    public static Map<String, Object> createUploadMetadata(MultipartFile file, Map<String, Object> userMetadata) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Core upload metadata
        metadata.put("uploadedAt", LocalDateTime.now().toString());
        metadata.put("originalSize", file.getSize());
        metadata.put("contentType", file.getContentType());
        metadata.put("processingStatus", "uploaded");
        
        // Image-specific metadata
        if (isImageFile(file.getContentType())) {
            metadata.put("isImage", true);
            metadata.put("imageProcessed", false);
            metadata.put("ocrStatus", "pending");
            metadata.put("thumbnailStatus", "pending");
        }
        
        // User-provided metadata
        if (userMetadata != null && !userMetadata.isEmpty()) {
            // Sanitize and merge user metadata
            Map<String, Object> sanitizedUserMetadata = sanitizeUserMetadata(userMetadata);
            metadata.put("userMetadata", sanitizedUserMetadata);
        }
        
        return metadata;
    }
    
    /**
     * Creates OCR result metadata
     */
    public static Map<String, Object> createOCRMetadata(String extractedText, double confidence, 
                                                       boolean success, String errorMessage) {
        Map<String, Object> ocrMetadata = new HashMap<>();
        ocrMetadata.put("success", success);
        ocrMetadata.put("processedAt", LocalDateTime.now().toString());
        
        if (success && extractedText != null) {
            TextAnalysisUtils.TextStats stats = TextAnalysisUtils.analyzeText(extractedText);
            ocrMetadata.put("extractedText", extractedText);
            ocrMetadata.put("confidence", confidence);
            ocrMetadata.put("textLength", stats.getCharacterCount());
            ocrMetadata.put("wordCount", stats.getWordCount());
            ocrMetadata.put("lineCount", stats.getLineCount());
        } else {
            ocrMetadata.put("extractedText", "");
            ocrMetadata.put("confidence", 0.0);
            ocrMetadata.put("textLength", 0);
            ocrMetadata.put("wordCount", 0);
            ocrMetadata.put("lineCount", 0);
            
            if (errorMessage != null) {
                ocrMetadata.put("error", errorMessage);
            }
        }
        
        return ocrMetadata;
    }
    
    /**
     * Creates thumbnail metadata
     */
    public static Map<String, Object> createThumbnailMetadata(String thumbnailUrl, String filename, 
                                                             int width, int height, String format, 
                                                             boolean success, String errorMessage) {
        Map<String, Object> thumbnailMetadata = new HashMap<>();
        thumbnailMetadata.put("success", success);
        thumbnailMetadata.put("createdAt", LocalDateTime.now().toString());
        
        if (success && thumbnailUrl != null) {
            thumbnailMetadata.put("url", thumbnailUrl);
            thumbnailMetadata.put("filename", filename);
            thumbnailMetadata.put("width", width);
            thumbnailMetadata.put("height", height);
            thumbnailMetadata.put("format", format);
            thumbnailMetadata.put("size", width + "x" + height);
        } else if (errorMessage != null) {
            thumbnailMetadata.put("error", errorMessage);
        }
        
        return thumbnailMetadata;
    }
    
    /**
     * Creates processing status metadata
     */
    public static Map<String, Object> createProcessingStatusMetadata(String stage, String status, 
                                                                   String message, Double progress) {
        Map<String, Object> statusMetadata = new HashMap<>();
        statusMetadata.put("stage", stage);
        statusMetadata.put("status", status);
        statusMetadata.put("updatedAt", LocalDateTime.now().toString());
        
        if (message != null) {
            statusMetadata.put("message", message);
        }
        
        if (progress != null) {
            statusMetadata.put("progress", Math.max(0.0, Math.min(100.0, progress)));
        }
        
        return statusMetadata;
    }
    
    /**
     * Creates security scan metadata
     */
    public static Map<String, Object> createSecurityScanMetadata(boolean passed, String threatType, 
                                                               String description, String fileHash) {
        Map<String, Object> securityMetadata = new HashMap<>();
        securityMetadata.put("scanned", true);
        securityMetadata.put("scannedAt", LocalDateTime.now().toString());
        securityMetadata.put("passed", passed);
        
        if (fileHash != null) {
            securityMetadata.put("fileHash", fileHash);
        }
        
        if (!passed) {
            securityMetadata.put("threatType", threatType);
            securityMetadata.put("description", description);
        }
        
        return securityMetadata;
    }
    
    /**
     * Updates existing metadata with new data while preserving existing structure
     */
    public static Map<String, Object> updateMetadata(Map<String, Object> existingMetadata, 
                                                    String key, Map<String, Object> newData) {
        Map<String, Object> updatedMetadata = existingMetadata != null ? 
                new HashMap<>(existingMetadata) : new HashMap<>();
        
        updatedMetadata.put(key, newData);
        updatedMetadata.put("lastUpdated", LocalDateTime.now().toString());
        
        return updatedMetadata;
    }
    
    /**
     * Merges multiple metadata maps with conflict resolution
     */
    public static Map<String, Object> mergeMetadata(Map<String, Object> base, 
                                                   Map<String, Object>... additionalMaps) {
        Map<String, Object> merged = base != null ? new HashMap<>(base) : new HashMap<>();
        
        for (Map<String, Object> additional : additionalMaps) {
            if (additional != null) {
                merged.putAll(additional);
            }
        }
        
        merged.put("mergedAt", LocalDateTime.now().toString());
        return merged;
    }
    
    /**
     * Creates failure metadata for any processing stage
     */
    public static Map<String, Object> createFailureMetadata(String stage, String errorMessage, 
                                                           String errorCode, Exception exception) {
        Map<String, Object> failureMetadata = new HashMap<>();
        failureMetadata.put("success", false);
        failureMetadata.put("stage", stage);
        failureMetadata.put("failedAt", LocalDateTime.now().toString());
        failureMetadata.put("error", errorMessage);
        
        if (errorCode != null) {
            failureMetadata.put("errorCode", errorCode);
        }
        
        if (exception != null) {
            failureMetadata.put("exceptionType", exception.getClass().getSimpleName());
            // Don't include full stack trace in metadata for security
        }
        
        return failureMetadata;
    }
    
    /**
     * Extracts processing summary from metadata
     */
    public static ProcessingSummary extractProcessingSummary(Map<String, Object> metadata) {
        if (metadata == null) {
            return ProcessingSummary.empty();
        }
        
        boolean hasOCR = metadata.containsKey("ocr");
        boolean hasThumbnail = metadata.containsKey("thumbnail");
        boolean isOCRSuccess = hasOCR && isOperationSuccess((Map<String, Object>) metadata.get("ocr"));
        boolean isThumbnailSuccess = hasThumbnail && isOperationSuccess((Map<String, Object>) metadata.get("thumbnail"));
        
        String overallStatus = determineOverallStatus(hasOCR, hasThumbnail, isOCRSuccess, isThumbnailSuccess);
        
        return ProcessingSummary.builder()
                .overallStatus(overallStatus)
                .hasOCR(hasOCR)
                .hasThumbnail(hasThumbnail)
                .isOCRSuccess(isOCRSuccess)
                .isThumbnailSuccess(isThumbnailSuccess)
                .build();
    }
    
    // Helper methods
    private static boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
    
    private static Map<String, Object> sanitizeUserMetadata(Map<String, Object> userMetadata) {
        Map<String, Object> sanitized = new HashMap<>();
        
        userMetadata.forEach((key, value) -> {
            // Sanitize keys and values to prevent injection
            String sanitizedKey = sanitizeString(key);
            Object sanitizedValue = sanitizeValue(value);
            
            if (sanitizedKey != null && sanitizedValue != null) {
                sanitized.put(sanitizedKey, sanitizedValue);
            }
        });
        
        return sanitized;
    }
    
    private static String sanitizeString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'&]", "").trim();
    }
    
    private static Object sanitizeValue(Object value) {
        if (value instanceof String) {
            return sanitizeString((String) value);
        } else if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        // For complex objects, convert to string and sanitize
        return sanitizeString(String.valueOf(value));
    }
    
    private static boolean isOperationSuccess(Map<String, Object> operationMetadata) {
        return operationMetadata != null && 
               Boolean.TRUE.equals(operationMetadata.get("success"));
    }
    
    private static String determineOverallStatus(boolean hasOCR, boolean hasThumbnail, 
                                               boolean isOCRSuccess, boolean isThumbnailSuccess) {
        if (!hasOCR && !hasThumbnail) {
            return "pending";
        }
        
        boolean allCompleted = (!hasOCR || isOCRSuccess) && (!hasThumbnail || isThumbnailSuccess);
        boolean anyFailed = (hasOCR && !isOCRSuccess) || (hasThumbnail && !isThumbnailSuccess);
        
        if (allCompleted) {
            return "completed";
        } else if (anyFailed) {
            return "partial_failure";
        } else {
            return "processing";
        }
    }
    
    /**
     * Processing summary data class
     */
    public static class ProcessingSummary {
        private final String overallStatus;
        private final boolean hasOCR;
        private final boolean hasThumbnail;
        private final boolean isOCRSuccess;
        private final boolean isThumbnailSuccess;
        
        private ProcessingSummary(String overallStatus, boolean hasOCR, boolean hasThumbnail, 
                                boolean isOCRSuccess, boolean isThumbnailSuccess) {
            this.overallStatus = overallStatus;
            this.hasOCR = hasOCR;
            this.hasThumbnail = hasThumbnail;
            this.isOCRSuccess = isOCRSuccess;
            this.isThumbnailSuccess = isThumbnailSuccess;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static ProcessingSummary empty() {
            return new ProcessingSummary("pending", false, false, false, false);
        }
        
        // Getters
        public String getOverallStatus() { return overallStatus; }
        public boolean hasOCR() { return hasOCR; }
        public boolean hasThumbnail() { return hasThumbnail; }
        public boolean isOCRSuccess() { return isOCRSuccess; }
        public boolean isThumbnailSuccess() { return isThumbnailSuccess; }
        
        public static class Builder {
            private String overallStatus;
            private boolean hasOCR;
            private boolean hasThumbnail;
            private boolean isOCRSuccess;
            private boolean isThumbnailSuccess;
            
            public Builder overallStatus(String overallStatus) {
                this.overallStatus = overallStatus;
                return this;
            }
            
            public Builder hasOCR(boolean hasOCR) {
                this.hasOCR = hasOCR;
                return this;
            }
            
            public Builder hasThumbnail(boolean hasThumbnail) {
                this.hasThumbnail = hasThumbnail;
                return this;
            }
            
            public Builder isOCRSuccess(boolean isOCRSuccess) {
                this.isOCRSuccess = isOCRSuccess;
                return this;
            }
            
            public Builder isThumbnailSuccess(boolean isThumbnailSuccess) {
                this.isThumbnailSuccess = isThumbnailSuccess;
                return this;
            }
            
            public ProcessingSummary build() {
                return new ProcessingSummary(overallStatus, hasOCR, hasThumbnail, isOCRSuccess, isThumbnailSuccess);
            }
        }
    }
}