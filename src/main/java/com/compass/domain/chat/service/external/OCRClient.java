package com.compass.domain.chat.service.external;

import com.compass.domain.chat.model.enums.DocumentType;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageContext;
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
    private static final Pattern AIRLINE_KEYWORD = Pattern.compile("BOARDING PASS|E-TICKET", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOTEL_KEYWORD = Pattern.compile("hotel|check[- ]?in|check[- ]?out|room|reservation|confirmation", Pattern.CASE_INSENSITIVE);
    private static final List<String> LANGUAGE_HINTS = List.of("ko", "en", "ja", "zh");

    public String extractText(byte[] data) {
        var image = Image.newBuilder().setContent(ByteString.copyFrom(data)).build();
        return annotate(image);
    }

    public String extractTextFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("이미지 URL이 필요합니다.");
        }
        var image = Image.newBuilder()
                .setSource(ImageSource.newBuilder().setImageUri(imageUrl).build())
                .build();
        return annotate(image);
    }

    public DocumentType detectDocument(String text) {
        if (text == null || text.isBlank()) {
            return DocumentType.UNKNOWN;
        }
        if (AIRLINE_KEYWORD.matcher(text).find() || FLIGHT_CODE.matcher(text).find()) {
            return DocumentType.FLIGHT_RESERVATION;
        }
        if (HOTEL_KEYWORD.matcher(text).find()) {
            return DocumentType.HOTEL_RESERVATION;
        }
        return DocumentType.UNKNOWN;
    }

    private String annotate(Image image) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try (var client = ImageAnnotatorClient.create()) {
                var request = buildRequest(image);
                var response = client.batchAnnotateImages(List.of(request)).getResponses(0);
                var text = parseResponse(response);
                if (text.length() > 50 || attempt == 2) {
                    return text;
                }
                log.debug("OCR 텍스트 길이가 짧아 재시도합니다. attempt={}", attempt);
                Thread.sleep(150L * attempt);
            } catch (IOException e) {
                throw new IllegalStateException("OCR 처리 중 오류가 발생했습니다.", e);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("OCR 재시도 중 인터럽트", ie);
            }
        }
        return "";
    }

    private AnnotateImageRequest buildRequest(Image image) {
        var feature = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();
        var context = ImageContext.newBuilder()
                .addAllLanguageHints(LANGUAGE_HINTS)
                .build();
        return AnnotateImageRequest.newBuilder()
                .setImage(image)
                .addFeatures(feature)
                .setImageContext(context)
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
