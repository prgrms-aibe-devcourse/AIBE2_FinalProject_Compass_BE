package com.compass.domain.chat.controller;

import com.compass.domain.chat.dto.TemplateStatusDto;
import com.compass.domain.chat.service.MemoryBasedCollectionService;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 메모리 기반 템플릿 수집 컨트롤러
 * DB 접근을 최소화한 효율적인 정보 수집 API
 */
@Slf4j
@RestController
@RequestMapping("/api/chat/template")
@RequiredArgsConstructor
@Tag(name = "Template Collection", description = "메모리 기반 여행 정보 수집 API")
public class TemplateCollectionController {
    
    private final MemoryBasedCollectionService collectionService;
    private final UserRepository userRepository;
    
    /**
     * 정보 수집 시작 (템플릿 초기화)
     */
    @PostMapping("/start")
    @Operation(summary = "정보 수집 시작", description = "새로운 템플릿 기반 정보 수집 세션을 시작합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수집 시작 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<TemplateStatusDto> startCollection(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        
        log.info("Starting template collection for user: {}", userDetails.getUsername());
        
        // 사용자 ID 조회
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다"));
        
        String chatThreadId = request.get("chatThreadId");
        String initialMessage = request.get("initialMessage");
        
        TemplateStatusDto status = collectionService.startCollection(
            user.getId(), 
            chatThreadId, 
            initialMessage
        );
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 템플릿 정보 업데이트 (메모리)
     */
    @PostMapping("/update")
    @Operation(summary = "템플릿 업데이트", description = "사용자 응답으로 템플릿을 업데이트합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "업데이트 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<TemplateStatusDto> updateTemplate(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        
        String sessionId = request.get("sessionId");
        String userResponse = request.get("userResponse");
        
        log.info("Updating template {} with response: {}", sessionId, userResponse);
        
        TemplateStatusDto status = collectionService.updateTemplate(sessionId, userResponse);
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 현재 템플릿 상태 조회
     */
    @GetMapping("/status/{sessionId}")
    @Operation(summary = "템플릿 상태 조회", description = "현재 템플릿 수집 상태를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<TemplateStatusDto> getTemplateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId) {
        
        log.info("Getting template status for session: {}", sessionId);
        
        TemplateStatusDto status = collectionService.getTemplateStatus(sessionId);
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 템플릿 DB 저장 (선택적)
     */
    @PostMapping("/save/{sessionId}")
    @Operation(summary = "템플릿 저장", description = "메모리의 템플릿을 DB에 저장합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "저장 성공"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<Map<String, String>> saveTemplate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId) {
        
        log.info("Saving template to database: {}", sessionId);
        
        collectionService.saveTemplateToDatabase(sessionId);
        
        return ResponseEntity.ok(Map.of(
            "message", "템플릿이 성공적으로 저장되었습니다",
            "sessionId", sessionId
        ));
    }
    
    /**
     * 여행 계획 생성
     */
    @PostMapping("/generate-plan")
    @Operation(summary = "여행 계획 생성", description = "완성된 템플릿으로 여행 계획을 생성합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "계획 생성 성공"),
        @ApiResponse(responseCode = "400", description = "필수 정보 부족"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<Map<String, Object>> generatePlan(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request) {
        
        String sessionId = request.get("sessionId");
        log.info("Generating travel plan for session: {}", sessionId);
        
        try {
            String travelPlan = collectionService.generateTravelPlan(sessionId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "sessionId", sessionId,
                "plan", travelPlan,
                "message", "여행 계획이 성공적으로 생성되었습니다"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "message", "필수 정보가 부족하여 계획을 생성할 수 없습니다"
            ));
        }
    }
    
    /**
     * 세션 취소
     */
    @DeleteMapping("/cancel/{sessionId}")
    @Operation(summary = "세션 취소", description = "정보 수집 세션을 취소하고 템플릿을 삭제합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "취소 성공"),
        @ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음")
    })
    public ResponseEntity<Map<String, String>> cancelSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionId) {
        
        log.info("Cancelling session: {}", sessionId);
        
        collectionService.cancelSession(sessionId);
        
        return ResponseEntity.ok(Map.of(
            "message", "세션이 취소되었습니다",
            "sessionId", sessionId
        ));
    }
    
    /**
     * 샘플 템플릿 조회 (테스트용)
     */
    @GetMapping("/sample")
    @Operation(summary = "샘플 템플릿 조회", description = "테스트용 샘플 템플릿을 반환합니다")
    public ResponseEntity<TemplateStatusDto> getSampleTemplate() {
        log.info("Getting sample template for testing");
        
        // 샘플 데이터 생성
        TemplateStatusDto sample = TemplateStatusDto.builder()
                .sessionId("TIC_SAMPLE")
                .nextQuestion("어디로 여행을 가시나요?")
                .canGeneratePlan(false)
                .completionPercentage(0)
                .inputType("text")
                .helpText("여행하실 목적지를 알려주세요")
                .build();
        
        return ResponseEntity.ok(sample);
    }
}