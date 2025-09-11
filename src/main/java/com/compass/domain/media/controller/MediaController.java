package com.compass.domain.media.controller;

import com.compass.domain.media.dto.MediaDto;
import com.compass.domain.media.dto.MediaGetResponse;
import com.compass.domain.media.dto.MediaUploadResponse;
import com.compass.domain.media.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "미디어 업로드 및 관리 API")
public class MediaController {
    
    private final MediaService mediaService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "파일 업로드",
        description = "이미지 파일을 업로드합니다. 지원 형식: JPG, PNG, WEBP, GIF (최대 10MB)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "파일 업로드 성공",
            content = @Content(schema = @Schema(implementation = MediaUploadResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 크기 초과"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "403", description = "권한이 없는 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<MediaUploadResponse> uploadFile(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) Map<String, Object> metadata,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("파일 업로드 요청 - 사용자: {}, 파일명: {}, 크기: {}bytes", 
                userId, file.getOriginalFilename(), file.getSize());
        
        MediaDto.UploadRequest request = MediaDto.UploadRequest.builder()
                .file(file)
                .metadata(metadata)
                .build();
        
        MediaUploadResponse response = mediaService.uploadFile(request, userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "파일 조회",
        description = "업로드된 이미지 파일의 정보를 조회하고 서명된 URL을 반환합니다. (15분 만료)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "파일 조회 성공",
            content = @Content(schema = @Schema(implementation = MediaGetResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "403", description = "파일 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<MediaGetResponse> getMedia(
            @Parameter(description = "조회할 미디어 ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("파일 조회 요청 - 사용자: {}, 미디어 ID: {}", userId, id);
        
        MediaGetResponse response = mediaService.getMediaById(id, userId);
        
        // 캐싱 헤더 생성은 서비스에서 처리
        HttpHeaders headers = mediaService.createMediaHeaders(response);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }
    
    @GetMapping("/list")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "사용자 파일 목록 조회",
        description = "현재 사용자가 업로드한 모든 파일의 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "파일 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = MediaDto.ListResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "403", description = "권한이 없는 사용자"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<MediaDto.ListResponse>> getMediaList(
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("파일 목록 조회 요청 - 사용자: {}", userId);
        
        List<MediaDto.ListResponse> response = mediaService.getMediaListByUser(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "파일 삭제",
        description = "업로드된 파일을 삭제합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "파일 삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "403", description = "파일 삭제 권한 없음"),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteMedia(
            @Parameter(description = "삭제할 미디어 ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("파일 삭제 요청 - 사용자: {}, 미디어 ID: {}", userId, id);
        
        mediaService.deleteFile(id, userId);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/ocr")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "OCR 처리",
        description = "기존 이미지 파일에 대해 OCR 텍스트 추출을 수행하고 결과를 메타데이터에 저장합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OCR 처리 성공"),
        @ApiResponse(responseCode = "400", description = "이미지 파일이 아니거나 OCR 처리 실패"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "403", description = "파일 처리 권한 없음"),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> processOCR(
            @Parameter(description = "OCR 처리할 미디어 ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("OCR 처리 요청 - 사용자: {}, 미디어 ID: {}", userId, id);
        
        mediaService.processOCRForMedia(id, userId);
        Map<String, Object> ocrResult = mediaService.getOCRResult(id, userId);
        
        return ResponseEntity.ok(ocrResult);
    }
    
    @GetMapping("/{id}/ocr")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "OCR 결과 조회",
        description = "이미지 파일의 OCR 텍스트 추출 결과를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OCR 결과 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "403", description = "파일 조회 권한 없음"),
        @ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음 또는 OCR 결과 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> getOCRResult(
            @Parameter(description = "OCR 결과를 조회할 미디어 ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        
        Long userId = getUserIdFromAuthentication(authentication);
        log.info("OCR 결과 조회 요청 - 사용자: {}, 미디어 ID: {}", userId, id);
        
        Map<String, Object> ocrResult = mediaService.getOCRResult(id, userId);
        
        return ResponseEntity.ok(ocrResult);
    }

    @GetMapping("/{id}/thumbnail")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "썸네일 조회",
        description = "업로드된 이미지 파일의 썸네일을 조회하고 서명된 URL을 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "썸네일 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "403", description = "파일 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "썸네일이 없거나 파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> getThumbnail(
            @Parameter(description = "썸네일을 조회할 미디어 ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("썸네일 조회 요청 - 사용자: {}, 미디어 ID: {}", userId, id);

        Map<String, Object> thumbnailResult = mediaService.getThumbnailResult(id, userId);

        return ResponseEntity.ok(thumbnailResult);
    }

    @GetMapping("/{id}/thumbnail/url")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "썸네일 URL 조회",
        description = "업로드된 이미지 파일의 썸네일 Presigned URL을 반환합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "썸네일 URL 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "403", description = "파일 접근 권한 없음"),
        @ApiResponse(responseCode = "404", description = "썸네일이 없거나 파일을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Map<String, Object>> getThumbnailUrl(
            @Parameter(description = "썸네일 URL을 조회할 미디어 ID", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        log.info("썸네일 URL 조회 요청 - 사용자: {}, 미디어 ID: {}", userId, id);

        Map<String, Object> thumbnailUrlResult = mediaService.getThumbnailUrlResult(id, userId);

        return ResponseEntity.ok(thumbnailUrlResult);
    }

    @GetMapping("/health")
    @Operation(summary = "미디어 서비스 상태 확인", description = "미디어 서비스가 정상 작동하는지 확인합니다.")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Media service is running");
    }
    
    /**
     * Spring Security Authentication에서 사용자 ID를 추출합니다.
     * 
     * @param authentication Spring Security Authentication 객체
     * @return 사용자 ID
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authentication information is missing");
        }
        
        String email = authentication.getName();
        return mediaService.getUserIdByEmail(email);
    }
    
}