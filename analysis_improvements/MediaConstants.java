package com.compass.domain.media.config;

/**
 * Media domain constants to eliminate magic numbers and improve maintainability
 */
public final class MediaConstants {
    
    // File Size Limits
    public static final long MAX_OCR_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    public static final long MAX_UPLOAD_FILE_SIZE = 100L * 1024 * 1024; // 100MB
    public static final long MAX_DOWNLOAD_SIZE = 100L * 1024 * 1024; // 100MB
    
    // Filename Limits
    public static final int MAX_FILENAME_LENGTH = 255;
    
    // Presigned URL
    public static final int DEFAULT_PRESIGNED_URL_EXPIRATION_MINUTES = 15;
    
    // Cache Headers
    public static final int CACHE_CONTROL_MAX_AGE_MINUTES = 15;
    
    // Thumbnail Configuration
    public static final int THUMBNAIL_WIDTH = 300;
    public static final int THUMBNAIL_HEIGHT = 300;
    public static final String THUMBNAIL_FORMAT = "webp";
    public static final String THUMBNAIL_PREFIX = "thumbnail_";
    public static final float THUMBNAIL_QUALITY = 0.85f;
    
    // Image Processing
    public static final int MAX_IMAGE_WIDTH = 4000;
    public static final int MAX_IMAGE_HEIGHT = 4000;
    public static final double MAX_COMPRESSION_RATIO = 1000.0;
    public static final double MAX_METADATA_RATIO = 0.3;
    
    // S3 Path Structure
    public static final String S3_MEDIA_PREFIX = "media";
    public static final String S3_THUMBNAIL_PREFIX = "thumbnails";
    public static final String S3_PATH_DATE_FORMAT = "yyyy/MM/dd";
    
    // Retry Configuration
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_DELAY_MS = 1000;
    
    // Validation
    public static final int MIN_FILE_HEADER_BYTES = 4;
    public static final int JPEG_HEADER_CHECK_BYTES = 12;
    public static final int PNG_HEADER_CHECK_BYTES = 8;
    public static final int WEBP_HEADER_CHECK_BYTES = 12;
    public static final int GIF_HEADER_CHECK_BYTES = 6;
    
    private MediaConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}