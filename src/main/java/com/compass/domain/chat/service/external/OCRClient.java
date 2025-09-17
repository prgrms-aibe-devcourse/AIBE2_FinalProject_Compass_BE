package com.compass.domain.chat.service.external;

import com.compass.domain.chat.model.enums.DocumentType;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageSource;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OCRClient {

    private static final Pattern FLIGHT_CODE = Pattern.compile("\\b[A-Z]{2}\\d{2,4}\\b");
    private static final Pattern HOTEL_KEYWORD = Pattern.compile("hotel|check[- ]?in|check[- ]?out|room|reservation", Pattern.CASE_INSENSITIVE);

    public String extractText(byte[] data) {
        try (var client = ImageAnnotatorClient.create()) {
            var request = buildRequest(data);
            var response = client.batchAnnotateImages(List.of(request)).getResponses(0);
            return parseResponse(response);
        } catch (IOException e) {
            throw new IllegalStateException("OCR 처리 중 오류가 발생했습니다.", e);
        }
    }

    public String extractTextFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 필요합니다.");
        }
        try (var client = ImageAnnotatorClient.create()) {
            var request = buildRequest(imageUrl);
            var response = client.batchAnnotateImages(List.of(request)).getResponses(0);
            return parseResponse(response);
        } catch (IOException e) {
            throw new IllegalStateException("OCR 처리 중 오류가 발생했습니다.", e);
        }
    }

    public DocumentType detectDocument(String text) {
        if (text == null || text.isBlank()) {
            return DocumentType.UNKNOWN;
        }
        // 항공권 예약 패턴 확인
        if (FLIGHT_CODE.matcher(text).find()) {
            return DocumentType.FLIGHT_RESERVATION;
        }
        // 호텔 예약 키워드 확인
        if (HOTEL_KEYWORD.matcher(text).find()) {
            return DocumentType.HOTEL_RESERVATION;
        }
        return DocumentType.UNKNOWN;
    }

    private AnnotateImageRequest buildRequest(byte[] data) {
        var image = Image.newBuilder().setContent(ByteString.copyFrom(data)).build();
        var feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        return AnnotateImageRequest.newBuilder()
                .setImage(image)
                .addFeatures(feature)
                .build();
    }

    private AnnotateImageRequest buildRequest(String imageUrl) {
        // Vision API는 URL만으로도 이미지를 읽을 수 있다
        var image = Image.newBuilder()
                .setSource(ImageSource.newBuilder().setImageUri(imageUrl).build())
                .build();
        var feature = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        return AnnotateImageRequest.newBuilder()
                .setImage(image)
                .addFeatures(feature)
                .build();
    }

    private String parseResponse(AnnotateImageResponse response) {
        if (response.hasError()) {
            log.warn("OCR 오류: {}", response.getError().getMessage());
            return "";
        }
        var annotation = response.getFullTextAnnotation();
        return annotation == null ? "" : annotation.getText();
    }
}
