package com.compass.domain.media.controller;

import com.compass.domain.media.dto.MediaDto;
import com.compass.domain.media.dto.MediaGetResponse;
import com.compass.domain.media.dto.MediaUploadResponse;
import com.compass.domain.media.service.MediaService;
import com.compass.config.jwt.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;
    
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
            @RequestParam(value = "metadata", required = false) Map<String, Object> metadata,
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = getUserIdFromToken(authHeader);
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
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = getUserIdFromToken(authHeader);
        log.info("파일 조회 요청 - 사용자: {}, 미디어 ID: {}", userId, id);
        
        MediaGetResponse response = mediaService.getMediaById(id, userId);
        
        // 캐싱 헤더 생성은 서비스에서 처리
        HttpHeaders headers = mediaService.createMediaHeaders(response);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(response);
    }
    
    @GetMapping("/list")
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
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<MediaDto.ListResponse>> getMediaList(
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = getUserIdFromToken(authHeader);
        log.info("파일 목록 조회 요청 - 사용자: {}", userId);
        
        List<MediaDto.ListResponse> response = mediaService.getMediaListByUser(userId);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
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
            @RequestHeader("Authorization") String authHeader) {
        
        Long userId = getUserIdFromToken(authHeader);
        log.info("파일 삭제 요청 - 사용자: {}, 미디어 ID: {}", userId, id);
        
        mediaService.deleteFile(id, userId);
        
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/health")
    @Operation(summary = "미디어 서비스 상태 확인", description = "미디어 서비스가 정상 작동하는지 확인합니다.")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Media service is running");
    }
    
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtTokenProvider.getUsername(token);
            // JWT에서 이메일을 가져온 후, UserRepository에서 사용자 ID를 조회
            return mediaService.getUserIdByEmail(email);
        }
        throw new IllegalArgumentException("Authorization header is missing or invalid");
    }
}