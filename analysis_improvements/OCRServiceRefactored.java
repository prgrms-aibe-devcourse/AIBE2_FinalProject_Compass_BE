package com.compass.domain.media.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OCRServiceRefactored {
    
    private final GoogleVisionClientFactory visionClientFactory;
    
    public Map<String, Object> extractTextFromImage(MultipartFile file) throws IOException {
        log.info("OCR 텍스트 추출 시작 - 파일명: {}, 크기: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        return processOCR(file.getBytes(), file.getOriginalFilename());
    }
    
    public Map<String, Object> extractTextFromBytes(byte[] imageBytes, String filename) throws IOException {
        log.info("OCR 텍스트 추출 시작 (바이트 배열) - 파일명: {}, 크기: {} bytes", 
                filename, imageBytes.length);
        
        return processOCR(imageBytes, filename);
    }
    
    // Extract common OCR processing logic
    private Map<String, Object> processOCR(byte[] imageBytes, String filename) throws IOException {
        try (ImageAnnotatorClient vision = visionClientFactory.createClient()) {
            
            AnnotateImageResponse response = performVisionAPICall(imageBytes, vision);
            return buildOCRResult(response, filename);
            
        } catch (IOException e) {
            log.error("OCR 처리 중 IOException 발생 - 파일: {}", filename, e);
            return createFailureResult("OCR processing failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("OCR 처리 중 예외 발생 - 파일: {}", filename, e);
            return createFailureResult("Unexpected error during OCR: " + e.getMessage());
        }
    }
    
    private AnnotateImageResponse performVisionAPICall(byte[] imageBytes, ImageAnnotatorClient vision) {
        ByteString imgBytes = ByteString.copyFrom(imageBytes);
        Image image = Image.newBuilder().setContent(imgBytes).build();
        
        Feature feature = Feature.newBuilder()
                .setType(Feature.Type.TEXT_DETECTION)
                .build();
        
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();
        
        BatchAnnotateImagesRequest batchRequest = BatchAnnotateImagesRequest.newBuilder()
                .addRequests(request)
                .build();
        
        BatchAnnotateImagesResponse batchResponse = vision.batchAnnotateImages(batchRequest);
        return batchResponse.getResponsesList().get(0);
    }
    
    private Map<String, Object> buildOCRResult(AnnotateImageResponse response, String filename) {
        Map<String, Object> ocrResult = new HashMap<>();
        
        if (response.hasError()) {
            String errorMessage = response.getError().getMessage();
            log.error("Google Vision API 에러 - 파일: {}, 에러: {}", filename, errorMessage);
            return createFailureResult(errorMessage);
        }
        
        if (response.getTextAnnotationsCount() > 0) {
            String extractedText = response.getTextAnnotations(0).getDescription();
            return createSuccessResult(extractedText, filename);
        } else {
            log.info("OCR 결과 없음 - 파일: {}", filename);
            return createEmptyResult();
        }
    }
    
    private Map<String, Object> createSuccessResult(String extractedText, String filename) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("extractedText", extractedText);
        result.put("textLength", extractedText.length());
        result.put("confidence", 1.0); // Simplified confidence calculation
        result.put("processedAt", LocalDateTime.now().toString());
        result.put("wordCount", TextAnalysisUtils.countWords(extractedText));
        result.put("lineCount", TextAnalysisUtils.countLines(extractedText));
        
        log.info("OCR 텍스트 추출 완료 - 파일: {}, 텍스트 길이: {}", filename, extractedText.length());
        return result;
    }
    
    private Map<String, Object> createEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("extractedText", "");
        result.put("textLength", 0);
        result.put("confidence", 0.0);
        result.put("processedAt", LocalDateTime.now().toString());
        result.put("wordCount", 0);
        result.put("lineCount", 0);
        return result;
    }
    
    private Map<String, Object> createFailureResult(String errorMessage) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", errorMessage);
        result.put("processedAt", LocalDateTime.now().toString());
        return result;
    }
}