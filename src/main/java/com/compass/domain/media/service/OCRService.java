package com.compass.domain.media.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OCRService {
    
    /**
     * Google Credentials를 사용하여 ImageAnnotatorClient를 생성합니다.
     */
    private ImageAnnotatorClient createImageAnnotatorClient() throws IOException {
        try {
            // GOOGLE_APPLICATION_CREDENTIALS 환경 변수에서 인증 정보 로드
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            
            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();
            
            return ImageAnnotatorClient.create(settings);
        } catch (IOException e) {
            log.error("Google Vision API 클라이언트 생성 실패: {}", e.getMessage());
            throw new IOException("Failed to create Google Vision API client: " + e.getMessage(), e);
        }
    }
    
    /**
     * Google Vision API를 사용하여 이미지에서 텍스트를 추출합니다.
     *
     * @param file 텍스트를 추출할 이미지 파일
     * @return OCR 결과를 포함한 Map 객체
     * @throws IOException Google Vision API 호출 중 오류 발생 시
     */
    public Map<String, Object> extractTextFromImage(MultipartFile file) throws IOException {
        log.info("OCR 텍스트 추출 시작 - 파일명: {}, 크기: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        Map<String, Object> ocrResult = new HashMap<>();
        
        try (ImageAnnotatorClient vision = createImageAnnotatorClient()) {
            // 이미지 데이터를 ByteString으로 변환
            ByteString imgBytes = ByteString.copyFrom(file.getBytes());
            Image image = Image.newBuilder().setContent(imgBytes).build();
            
            // TEXT_DETECTION 기능 요청
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();
            
            // 이미지 주석 요청 생성
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();
            
            // 배치 요청으로 처리
            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);
            
            BatchAnnotateImagesRequest batchRequest = BatchAnnotateImagesRequest.newBuilder()
                    .addAllRequests(requests)
                    .build();
            
            // Google Vision API 호출
            BatchAnnotateImagesResponse batchResponse = vision.batchAnnotateImages(batchRequest);
            List<AnnotateImageResponse> responses = batchResponse.getResponsesList();
            
            if (responses.isEmpty()) {
                log.warn("Google Vision API 응답이 비어있습니다 - 파일: {}", file.getOriginalFilename());
                ocrResult.put("success", false);
                ocrResult.put("error", "No response from Google Vision API");
                return ocrResult;
            }
            
            AnnotateImageResponse response = responses.get(0);
            
            // 에러 체크
            if (response.hasError()) {
                String errorMessage = response.getError().getMessage();
                log.error("Google Vision API 에러 - 파일: {}, 에러: {}", 
                        file.getOriginalFilename(), errorMessage);
                ocrResult.put("success", false);
                ocrResult.put("error", errorMessage);
                return ocrResult;
            }
            
            // 텍스트 추출 결과 처리
            if (response.getTextAnnotationsCount() > 0) {
                com.google.cloud.vision.v1.EntityAnnotation textAnnotation = response.getTextAnnotations(0);
                String extractedText = textAnnotation.getDescription();
                
                // 결과 저장
                ocrResult.put("success", true);
                ocrResult.put("extractedText", extractedText);
                ocrResult.put("textLength", extractedText.length());
                ocrResult.put("confidence", calculateAverageConfidence(response));
                ocrResult.put("processedAt", java.time.LocalDateTime.now().toString());
                
                // 추가 통계 정보
                ocrResult.put("wordCount", countWords(extractedText));
                ocrResult.put("lineCount", countLines(extractedText));
                
                log.info("OCR 텍스트 추출 완료 - 파일: {}, 텍스트 길이: {}, 단어 수: {}", 
                        file.getOriginalFilename(), extractedText.length(), countWords(extractedText));
                
            } else {
                log.info("OCR 결과 없음 - 파일: {} (이미지에 텍스트가 없거나 인식되지 않음)", 
                        file.getOriginalFilename());
                ocrResult.put("success", true);
                ocrResult.put("extractedText", "");
                ocrResult.put("textLength", 0);
                ocrResult.put("confidence", 0.0);
                ocrResult.put("processedAt", java.time.LocalDateTime.now().toString());
                ocrResult.put("wordCount", 0);
                ocrResult.put("lineCount", 0);
            }
            
        } catch (IOException e) {
            log.error("OCR 처리 중 IOException 발생 - 파일: {}", file.getOriginalFilename(), e);
            ocrResult.put("success", false);
            ocrResult.put("error", "OCR processing failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("OCR 처리 중 예외 발생 - 파일: {}", file.getOriginalFilename(), e);
            ocrResult.put("success", false);
            ocrResult.put("error", "Unexpected error during OCR: " + e.getMessage());
        }
        
        return ocrResult;
    }
    
    /**
     * 바이트 배열로부터 텍스트를 추출합니다.
     *
     * @param imageBytes 이미지 바이트 배열
     * @param filename 파일명 (로깅용)
     * @return OCR 결과를 포함한 Map 객체
     * @throws IOException Google Vision API 호출 중 오류 발생 시
     */
    public Map<String, Object> extractTextFromBytes(byte[] imageBytes, String filename) throws IOException {
        log.info("OCR 텍스트 추출 시작 (바이트 배열) - 파일명: {}, 크기: {} bytes", 
                filename, imageBytes.length);
        
        Map<String, Object> ocrResult = new HashMap<>();
        
        try (ImageAnnotatorClient vision = createImageAnnotatorClient()) {
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image image = Image.newBuilder().setContent(imgBytes).build();
            
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();
            
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();
            
            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);
            
            BatchAnnotateImagesRequest batchRequest = BatchAnnotateImagesRequest.newBuilder()
                    .addAllRequests(requests)
                    .build();
            
            BatchAnnotateImagesResponse batchResponse = vision.batchAnnotateImages(batchRequest);
            List<AnnotateImageResponse> responses = batchResponse.getResponsesList();
            
            if (responses.isEmpty()) {
                log.warn("Google Vision API 응답이 비어있습니다 - 파일: {}", filename);
                ocrResult.put("success", false);
                ocrResult.put("error", "No response from Google Vision API");
                return ocrResult;
            }
            
            AnnotateImageResponse response = responses.get(0);
            
            if (response.hasError()) {
                String errorMessage = response.getError().getMessage();
                log.error("Google Vision API 에러 - 파일: {}, 에러: {}", filename, errorMessage);
                ocrResult.put("success", false);
                ocrResult.put("error", errorMessage);
                return ocrResult;
            }
            
            if (response.getTextAnnotationsCount() > 0) {
                com.google.cloud.vision.v1.EntityAnnotation textAnnotation = response.getTextAnnotations(0);
                String extractedText = textAnnotation.getDescription();
                
                ocrResult.put("success", true);
                ocrResult.put("extractedText", extractedText);
                ocrResult.put("textLength", extractedText.length());
                ocrResult.put("confidence", calculateAverageConfidence(response));
                ocrResult.put("processedAt", java.time.LocalDateTime.now().toString());
                ocrResult.put("wordCount", countWords(extractedText));
                ocrResult.put("lineCount", countLines(extractedText));
                
                log.info("OCR 텍스트 추출 완료 (바이트 배열) - 파일: {}, 텍스트 길이: {}", 
                        filename, extractedText.length());
                
            } else {
                log.info("OCR 결과 없음 (바이트 배열) - 파일: {}", filename);
                ocrResult.put("success", true);
                ocrResult.put("extractedText", "");
                ocrResult.put("textLength", 0);
                ocrResult.put("confidence", 0.0);
                ocrResult.put("processedAt", java.time.LocalDateTime.now().toString());
                ocrResult.put("wordCount", 0);
                ocrResult.put("lineCount", 0);
            }
            
        } catch (IOException e) {
            log.error("OCR 처리 중 IOException 발생 (바이트 배열) - 파일: {}", filename, e);
            ocrResult.put("success", false);
            ocrResult.put("error", "OCR processing failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("OCR 처리 중 예외 발생 (바이트 배열) - 파일: {}", filename, e);
            ocrResult.put("success", false);
            ocrResult.put("error", "Unexpected error during OCR: " + e.getMessage());
        }
        
        return ocrResult;
    }
    
    /**
     * OCR 결과의 평균 신뢰도를 계산합니다.
     */
    private double calculateAverageConfidence(AnnotateImageResponse response) {
        // Google Vision API의 TEXT_DETECTION은 일반적으로 신뢰도를 개별 단어에 제공하지 않으므로
        // 기본값으로 1.0을 반환합니다. 더 정확한 신뢰도가 필요한 경우 DOCUMENT_TEXT_DETECTION을 사용할 수 있습니다.
        return response.getTextAnnotationsCount() > 0 ? 1.0 : 0.0;
    }
    
    /**
     * 텍스트의 단어 수를 계산합니다.
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        
        // 공백, 탭, 줄바꿈으로 분할하여 단어 수 계산
        String[] words = text.trim().split("\\s+");
        return words.length;
    }
    
    /**
     * 텍스트의 줄 수를 계산합니다.
     */
    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        // 줄바꿈 문자로 분할하여 줄 수 계산
        String[] lines = text.split("\r\n|\r|\n");
        return lines.length;
    }
}