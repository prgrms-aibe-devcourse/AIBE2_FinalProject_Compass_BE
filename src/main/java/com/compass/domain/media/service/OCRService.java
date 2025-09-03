package com.compass.domain.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * OCR(Optical Character Recognition) 서비스
 * 이미지에서 텍스트를 추출하는 기능을 제공
 * Tesseract OCR 엔진을 사용하여 다국어 텍스트 인식 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OCRService {

    private final MediaService mediaService;

    @Value("${app.ocr.tesseract.path:/usr/bin/tesseract}")
    private String tesseractPath;

    @Value("${app.ocr.tesseract.datapath:/usr/share/tesseract-ocr/4.00/tessdata}")
    private String tesseractDataPath;

    @Value("${app.ocr.languages:kor+eng}")
    private String ocrLanguages;

    @Value("${app.ocr.temp.dir:${java.io.tmpdir}/ocr}")
    private String tempDirectory;

    @Value("${app.ocr.enabled:true}")
    private boolean ocrEnabled;

    // OCR 처리 가능한 이미지 MIME 타입
    private static final List<String> SUPPORTED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/bmp", "image/tiff", "image/gif"
    );

    /**
     * 이미지 파일에서 텍스트 추출 (비동기)
     * 
     * @param fileUuid 파일 UUID
     * @param imageBytes 이미지 바이트 배열
     * @param mimeType 이미지 MIME 타입
     * @return 추출된 텍스트
     */
    @Async
    public CompletableFuture<String> extractTextFromImageAsync(UUID fileUuid, byte[] imageBytes, String mimeType) {
        log.info("OCR 비동기 처리 시작 - 파일 UUID: {}, MIME 타입: {}", fileUuid, mimeType);
        
        try {
            String extractedText = extractTextFromImage(imageBytes, mimeType);
            
            // OCR 결과를 데이터베이스에 업데이트
            boolean updated = mediaService.updateOcrText(fileUuid, extractedText);
            if (updated) {
                log.info("OCR 처리 완료 및 DB 업데이트 성공 - 파일 UUID: {}", fileUuid);
            } else {
                log.warn("OCR 처리는 완료되었으나 DB 업데이트 실패 - 파일 UUID: {}", fileUuid);
            }
            
            return CompletableFuture.completedFuture(extractedText);
            
        } catch (Exception e) {
            log.error("OCR 비동기 처리 실패 - 파일 UUID: {}", fileUuid, e);
            return CompletableFuture.completedFuture("");
        }
    }

    /**
     * 이미지 파일에서 텍스트 추출 (동기)
     * 
     * @param imageBytes 이미지 바이트 배열
     * @param mimeType 이미지 MIME 타입
     * @return 추출된 텍스트
     * @throws IOException 이미지 처리 중 오류 발생 시
     */
    public String extractTextFromImage(byte[] imageBytes, String mimeType) throws IOException {
        if (!ocrEnabled) {
            log.info("OCR 기능이 비활성화되어 있습니다.");
            return "";
        }
        
        if (!isSupportedImageType(mimeType)) {
            log.warn("지원하지 않는 이미지 타입: {}", mimeType);
            return "";
        }
        
        log.info("OCR 텍스트 추출 시작 - MIME 타입: {}, 이미지 크기: {} bytes", mimeType, imageBytes.length);
        
        Path tempImageFile = null;
        try {
            // 임시 디렉토리 생성
            createTempDirectoryIfNotExists();
            
            // 임시 이미지 파일 생성
            tempImageFile = createTempImageFile(imageBytes, mimeType);
            
            // 이미지 전처리 (선택적)
            Path preprocessedImageFile = preprocessImage(tempImageFile);
            
            // Tesseract OCR 실행
            String extractedText = runTesseractOCR(preprocessedImageFile);
            
            // 텍스트 후처리
            String cleanedText = postprocessText(extractedText);
            
            log.info("OCR 텍스트 추출 완료 - 추출된 텍스트 길이: {} 문자", cleanedText.length());
            return cleanedText;
            
        } catch (Exception e) {
            log.error("OCR 텍스트 추출 실패", e);
            throw new IOException("OCR 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        } finally {
            // 임시 파일 정리
            cleanupTempFiles(tempImageFile);
        }
    }

    /**
     * MultipartFile에서 텍스트 추출
     * 
     * @param file 업로드된 파일
     * @return 추출된 텍스트
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public String extractTextFromMultipartFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }
        
        return extractTextFromImage(file.getBytes(), file.getContentType());
    }

    /**
     * 지원되는 이미지 타입인지 확인
     * 
     * @param mimeType MIME 타입
     * @return 지원 여부
     */
    public boolean isSupportedImageType(String mimeType) {
        return mimeType != null && SUPPORTED_IMAGE_TYPES.contains(mimeType.toLowerCase());
    }

    /**
     * OCR 기능 활성화 여부 확인
     * 
     * @return OCR 활성화 여부
     */
    public boolean isOcrEnabled() {
        return ocrEnabled;
    }

    /**
     * 임시 디렉토리 생성
     */
    private void createTempDirectoryIfNotExists() throws IOException {
        Path tempDir = Path.of(tempDirectory);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
            log.info("OCR 임시 디렉토리 생성: {}", tempDir.toAbsolutePath());
        }
    }

    /**
     * 임시 이미지 파일 생성
     * 
     * @param imageBytes 이미지 바이트 배열
     * @param mimeType MIME 타입
     * @return 임시 파일 경로
     */
    private Path createTempImageFile(byte[] imageBytes, String mimeType) throws IOException {
        String fileExtension = getFileExtensionFromMimeType(mimeType);
        String fileName = "ocr_temp_" + UUID.randomUUID().toString() + "." + fileExtension;
        Path tempFile = Path.of(tempDirectory, fileName);
        
        try (InputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.debug("임시 이미지 파일 생성: {}", tempFile.toAbsolutePath());
        return tempFile;
    }

    /**
     * 이미지 전처리 (품질 향상을 위한 선택적 처리)
     * 
     * @param imagePath 원본 이미지 경로
     * @return 전처리된 이미지 경로
     */
    private Path preprocessImage(Path imagePath) throws IOException {
        try {
            BufferedImage originalImage = ImageIO.read(imagePath.toFile());
            if (originalImage == null) {
                throw new IOException("이미지를 읽을 수 없습니다: " + imagePath);
            }
            
            // 현재는 원본 이미지를 그대로 반환
            // 필요에 따라 이미지 크기 조정, 노이즈 제거, 대비 향상 등의 전처리 로직 추가 가능
            
            return imagePath;
            
        } catch (IOException e) {
            log.warn("이미지 전처리 실패, 원본 이미지 사용: {}", imagePath, e);
            return imagePath;
        }
    }

    /**
     * Tesseract OCR 실행
     * 
     * @param imagePath 이미지 파일 경로
     * @return 추출된 텍스트
     */
    private String runTesseractOCR(Path imagePath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        // Tesseract 명령어 구성
        processBuilder.command(
                tesseractPath,
                imagePath.toAbsolutePath().toString(),
                "stdout",  // 표준 출력으로 결과 출력
                "-l", ocrLanguages,  // 언어 설정
                "--psm", "6",  // Page Segmentation Mode: 단일 텍스트 블록
                "--oem", "3"   // OCR Engine Mode: 기본값
        );
        
        // 환경 변수 설정
        if (tesseractDataPath != null && !tesseractDataPath.isEmpty()) {
            processBuilder.environment().put("TESSDATA_PREFIX", tesseractDataPath);
        }
        
        log.debug("Tesseract 명령어 실행: {}", String.join(" ", processBuilder.command()));
        
        Process process = processBuilder.start();
        
        // 프로세스 완료 대기 (최대 30초)
        boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Tesseract OCR 처리 시간 초과");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            // 에러 스트림에서 오류 메시지 읽기
            String errorMessage = new String(process.getErrorStream().readAllBytes());
            throw new IOException("Tesseract OCR 실행 실패 (종료 코드: " + exitCode + "): " + errorMessage);
        }
        
        // 표준 출력에서 결과 읽기
        String result = new String(process.getInputStream().readAllBytes());
        log.debug("Tesseract OCR 결과 길이: {} 문자", result.length());
        
        return result;
    }

    /**
     * 추출된 텍스트 후처리
     * 
     * @param rawText 원본 텍스트
     * @return 정제된 텍스트
     */
    private String postprocessText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }
        
        // 기본적인 텍스트 정제
        String cleanedText = rawText
                .trim()  // 앞뒤 공백 제거
                .replaceAll("\\r\\n|\\r|\\n", " ")  // 줄바꿈을 공백으로 변환
                .replaceAll("\\s+", " ")  // 연속된 공백을 하나로 통합
                .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");  // 유효하지 않은 문자 제거
        
        log.debug("텍스트 후처리 완료 - 원본: {} 문자, 정제 후: {} 문자", rawText.length(), cleanedText.length());
        
        return cleanedText;
    }

    /**
     * MIME 타입에서 파일 확장자 추출
     * 
     * @param mimeType MIME 타입
     * @return 파일 확장자
     */
    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return "png";
        
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/bmp" -> "bmp";
            case "image/tiff" -> "tiff";
            case "image/gif" -> "gif";
            default -> "png";
        };
    }

    /**
     * 임시 파일 정리
     * 
     * @param tempFiles 정리할 임시 파일들
     */
    private void cleanupTempFiles(Path... tempFiles) {
        for (Path tempFile : tempFiles) {
            if (tempFile != null && Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                    log.debug("임시 파일 삭제: {}", tempFile.toAbsolutePath());
                } catch (IOException e) {
                    log.warn("임시 파일 삭제 실패: {}", tempFile.toAbsolutePath(), e);
                }
            }
        }
    }
}