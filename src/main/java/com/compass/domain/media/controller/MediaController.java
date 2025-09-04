package com.compass.domain.media.controller;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "미디어 업로드 및 관리 API")
public class MediaController {
    
    private final MediaService mediaService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<MediaUploadResponse> uploadFile(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        log.info("파일 업로드 요청 - 사용자: {}, 파일명: {}, 크기: {}bytes", 
                authentication.getName(), file.getOriginalFilename(), file.getSize());
        
        String userId = authentication.getName();
        MediaUploadResponse response = mediaService.uploadFile(file, userId);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    @Operation(summary = "미디어 서비스 상태 확인", description = "미디어 서비스가 정상 작동하는지 확인합니다.")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Media service is running");
    }
}