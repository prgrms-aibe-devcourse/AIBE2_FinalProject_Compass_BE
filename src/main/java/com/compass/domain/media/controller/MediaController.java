package com.compass.domain.media.controller;

import com.compass.domain.media.entity.FileStatus;
import com.compass.domain.media.entity.Media;
import com.compass.domain.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * MEDIA 도메인의 REST API 컨트롤러
 * 파일 업로드, 조회, 삭제 등의 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    /**
     * 서비스 상태 확인
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Media Service");
        return response;
    }

    /**
     * 파일 업로드
     * 
     * @param file 업로드할 파일
     * @param userId 사용자 ID (헤더 또는 인증에서 추출)
     * @return 업로드된 파일 정보
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("파일 업로드 요청 - 사용자: {}, 파일명: {}", userId, file.getOriginalFilename());
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("파일이 비어있습니다."));
            }
            
            Media uploadedMedia = mediaService.uploadFile(file, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "파일 업로드가 완료되었습니다.");
            response.put("data", createMediaResponse(uploadedMedia));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("파일 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("파일 업로드 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * UUID로 파일 정보 조회
     * 
     * @param fileUuid 파일 UUID
     * @param userId 사용자 ID (권한 확인용)
     * @return 파일 정보
     */
    @GetMapping("/{fileUuid}")
    public ResponseEntity<Map<String, Object>> getFileInfo(
            @PathVariable UUID fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("파일 정보 조회 - UUID: {}, 사용자: {}", fileUuid, userId);
        
        Optional<Media> mediaOpt = mediaService.findByUuid(fileUuid);
        if (mediaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Media media = mediaOpt.get();
        // 권한 확인: 본인의 파일만 조회 가능
        if (!media.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("파일에 대한 접근 권한이 없습니다."));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", createMediaResponse(media));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자별 파일 목록 조회 (페이징)
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 파일 목록
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getUserFiles(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("사용자 파일 목록 조회 - 사용자: {}, 페이지: {}", userId, pageable.getPageNumber());
        
        Page<Media> mediaPage = mediaService.findByUserId(userId, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mediaPage.map(this::createMediaResponse));
        response.put("totalElements", mediaPage.getTotalElements());
        response.put("totalPages", mediaPage.getTotalPages());
        response.put("currentPage", mediaPage.getNumber());
        response.put("size", mediaPage.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 파일 상태별 조회
     * 
     * @param userId 사용자 ID
     * @param status 파일 상태
     * @return 해당 상태의 파일 목록
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getFilesByStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable FileStatus status) {
        
        log.info("파일 상태별 조회 - 사용자: {}, 상태: {}", userId, status);
        
        List<Media> mediaList = mediaService.findByUserIdAndStatus(userId, status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mediaList.stream().map(this::createMediaResponse).toList());
        response.put("count", mediaList.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 파일 삭제 (소프트 삭제)
     * 
     * @param fileUuid 파일 UUID
     * @param userId 사용자 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{fileUuid}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable UUID fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("파일 삭제 요청 - UUID: {}, 사용자: {}", fileUuid, userId);
        
        boolean deleted = mediaService.deleteFile(fileUuid, userId);
        
        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "파일이 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 파일 영구 삭제
     * 
     * @param fileUuid 파일 UUID
     * @param userId 사용자 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{fileUuid}/permanent")
    public ResponseEntity<Map<String, Object>> permanentDeleteFile(
            @PathVariable UUID fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("파일 영구 삭제 요청 - UUID: {}, 사용자: {}", fileUuid, userId);
        
        boolean deleted = mediaService.permanentDeleteFile(fileUuid, userId);
        
        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "파일이 영구 삭제되었습니다.");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Presigned URL 생성 (파일 다운로드용)
     * 
     * @param fileUuid 파일 UUID
     * @param userId 사용자 ID
     * @return Presigned URL
     */
    @GetMapping("/{fileUuid}/download")
    public ResponseEntity<Map<String, Object>> getDownloadUrl(
            @PathVariable UUID fileUuid,
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("다운로드 URL 요청 - UUID: {}, 사용자: {}", fileUuid, userId);
        
        Optional<String> presignedUrlOpt = mediaService.generatePresignedUrl(fileUuid, userId);
        
        if (presignedUrlOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("downloadUrl", presignedUrlOpt.get());
            response.put("expiresIn", "1시간");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 사용자 파일 통계 조회
     * 
     * @param userId 사용자 ID
     * @return 파일 통계 정보
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserFileStats(
            @RequestHeader("X-User-Id") Long userId) {
        
        log.info("사용자 파일 통계 조회 - 사용자: {}", userId);
        
        Long totalSize = mediaService.getTotalFileSizeByUser(userId);
        Long fileCount = mediaService.getFileCountByUser(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", fileCount);
        stats.put("totalSize", totalSize);
        stats.put("totalSizeFormatted", formatFileSize(totalSize));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);
        
        return ResponseEntity.ok(response);
    }

    /**
     * OCR 텍스트 업데이트 (내부 API)
     * 
     * @param fileUuid 파일 UUID
     * @param ocrText OCR 텍스트
     * @return 업데이트 결과
     */
    @PutMapping("/{fileUuid}/ocr")
    public ResponseEntity<Map<String, Object>> updateOcrText(
            @PathVariable UUID fileUuid,
            @RequestBody Map<String, String> request) {
        
        String ocrText = request.get("ocrText");
        log.info("OCR 텍스트 업데이트 - UUID: {}", fileUuid);
        
        boolean updated = mediaService.updateOcrText(fileUuid, ocrText);
        
        if (updated) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OCR 텍스트가 업데이트되었습니다.");
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Media 엔티티를 응답용 Map으로 변환
     */
    private Map<String, Object> createMediaResponse(Media media) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", media.getId());
        response.put("fileUuid", media.getFileUuid());
        response.put("originalFilename", media.getOriginalFilename());
        response.put("fileSize", media.getFileSize());
        response.put("fileSizeFormatted", formatFileSize(media.getFileSize()));
        response.put("mimeType", media.getMimeType());
        response.put("fileStatus", media.getFileStatus());
        response.put("s3Url", media.getS3Url());
        response.put("ocrText", media.getOcrText());
        response.put("ocrProcessed", media.getOcrProcessed());
        response.put("createdAt", media.getCreatedAt());
        response.put("updatedAt", media.getUpdatedAt());
        return response;
    }

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    /**
     * 파일 크기를 읽기 쉬운 형태로 포맷
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
}