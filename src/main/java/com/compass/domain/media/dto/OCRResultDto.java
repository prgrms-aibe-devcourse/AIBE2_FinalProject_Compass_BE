package com.compass.domain.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OCR 처리 결과")
public class OCRResultDto {
    
    @Schema(description = "OCR 처리 성공 여부", example = "true")
    private Boolean success;
    
    @Schema(description = "추출된 텍스트", example = "여행 일정표")
    private String extractedText;
    
    @Schema(description = "텍스트 길이", example = "25")
    private Integer textLength;
    
    @Schema(description = "OCR 신뢰도 (0.0 ~ 1.0)", example = "0.95")
    private Double confidence;
    
    @Schema(description = "단어 수", example = "5")
    private Integer wordCount;
    
    @Schema(description = "줄 수", example = "3")
    private Integer lineCount;
    
    @Schema(description = "OCR 처리 시각", example = "2024-01-01T12:00:00")
    private String processedAt;
    
    @Schema(description = "오류 메시지 (실패 시)", example = "OCR 결과가 없습니다.")
    private String error;
    
    @Schema(description = "추가 메타데이터")
    private Map<String, Object> metadata;
    
    public static OCRResultDto from(Map<String, Object> ocrResult) {
        return OCRResultDto.builder()
                .success((Boolean) ocrResult.get("success"))
                .extractedText((String) ocrResult.get("extractedText"))
                .textLength((Integer) ocrResult.get("textLength"))
                .confidence((Double) ocrResult.get("confidence"))
                .wordCount((Integer) ocrResult.get("wordCount"))
                .lineCount((Integer) ocrResult.get("lineCount"))
                .processedAt((String) ocrResult.get("processedAt"))
                .error((String) ocrResult.get("error"))
                .metadata(ocrResult)
                .build();
    }
}