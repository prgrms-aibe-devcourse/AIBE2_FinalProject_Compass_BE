package com.compass.domain.media.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 썸네일 생성 서비스
 * 300x300 WebP 포맷 썸네일을 생성합니다.
 */
@Slf4j
@Service
public class ThumbnailService {

    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 300;
    private static final String THUMBNAIL_FORMAT = "webp";
    private static final String THUMBNAIL_PREFIX = "thumbnail_";

    /**
     * MultipartFile로부터 300x300 WebP 썸네일을 생성합니다.
     *
     * @param file 원본 이미지 파일
     * @return 썸네일 이미지 바이트 배열
     * @throws IOException 이미지 처리 중 오류 발생 시
     */
    public byte[] generateThumbnail(MultipartFile file) throws IOException {
        return generateThumbnail(file, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
    }

    /**
     * MultipartFile로부터 지정된 크기의 WebP 썸네일을 생성합니다.
     *
     * @param file 원본 이미지 파일
     * @param width 썸네일 너비
     * @param height 썸네일 높이
     * @return 썸네일 이미지 바이트 배열
     * @throws IOException 이미지 처리 중 오류 발생 시
     */
    public byte[] generateThumbnail(MultipartFile file, int width, int height) throws IOException {
        log.info("썸네일 생성 시작 - 파일: {}, 크기: {}x{}", file.getOriginalFilename(), width, height);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Thumbnailator를 사용하여 썸네일 생성
            Thumbnails.of(file.getInputStream())
                    .size(width, height)
                    .crop(Positions.CENTER)  // 중앙을 기준으로 크롭
                    .outputFormat(THUMBNAIL_FORMAT)
                    .outputQuality(0.85f)    // WebP 압축 품질 (85%)
                    .toOutputStream(outputStream);

            byte[] thumbnailData = outputStream.toByteArray();

            log.info("썸네일 생성 완료 - 파일: {}, 원본 크기: {} bytes, 썸네일 크기: {} bytes",
                    file.getOriginalFilename(), file.getSize(), thumbnailData.length);

            return thumbnailData;

        } catch (IOException e) {
            log.error("썸네일 생성 실패 - 파일: {}, 에러: {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new IOException("썸네일 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 바이트 배열로부터 썸네일을 생성합니다.
     *
     * @param imageData 원본 이미지 바이트 배열
     * @param originalFilename 원본 파일명 (로깅용)
     * @return 썸네일 이미지 바이트 배열
     * @throws IOException 이미지 처리 중 오류 발생 시
     */
    public byte[] generateThumbnailFromBytes(byte[] imageData, String originalFilename) throws IOException {
        log.info("바이트 배열로부터 썸네일 생성 시작 - 파일: {}, 크기: {} bytes",
                originalFilename, imageData.length);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Thumbnailator를 사용하여 썸네일 생성
            Thumbnails.of(inputStream)
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .crop(Positions.CENTER)
                    .outputFormat(THUMBNAIL_FORMAT)
                    .outputQuality(0.85f)
                    .toOutputStream(outputStream);

            byte[] thumbnailData = outputStream.toByteArray();

            log.info("바이트 배열 썸네일 생성 완료 - 파일: {}, 썸네일 크기: {} bytes",
                    originalFilename, thumbnailData.length);

            return thumbnailData;

        } catch (IOException e) {
            log.error("바이트 배열 썸네일 생성 실패 - 파일: {}, 에러: {}", originalFilename, e.getMessage(), e);
            throw new IOException("썸네일 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 썸네일 파일명을 생성합니다.
     *
     * @param originalFilename 원본 파일명
     * @return 썸네일 파일명
     */
    public String generateThumbnailFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = THUMBNAIL_FORMAT;

        return String.format("%s%s_%s.%s", THUMBNAIL_PREFIX, timestamp, uuid, extension);
    }

    /**
     * 이미지 파일인지 확인합니다.
     *
     * @param contentType MIME 타입
     * @return 이미지 파일 여부
     */
    public boolean isImageFile(String contentType) {
        return contentType != null &&
               (contentType.startsWith("image/jpeg") ||
                contentType.startsWith("image/png") ||
                contentType.startsWith("image/webp") ||
                contentType.startsWith("image/gif"));
    }

    /**
     * 썸네일 정보를 포함한 메타데이터를 생성합니다.
     *
     * @param thumbnailUrl 썸네일 S3 URL
     * @param thumbnailFilename 썸네일 파일명
     * @return 썸네일 메타데이터 맵
     */
    public java.util.Map<String, Object> createThumbnailMetadata(String thumbnailUrl, String thumbnailFilename) {
        return java.util.Map.of(
            "url", thumbnailUrl,
            "filename", thumbnailFilename,
            "size", THUMBNAIL_WIDTH + "x" + THUMBNAIL_HEIGHT,
            "format", THUMBNAIL_FORMAT,
            "createdAt", LocalDateTime.now().toString()
        );
    }
}
