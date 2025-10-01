package com.compass.domain.chat.controller;

import com.compass.domain.chat.model.enums.DocumentType;
import com.compass.domain.chat.service.external.OCRClient;
import com.compass.domain.chat.service.HotelReservationService;
import com.compass.domain.chat.model.dto.HotelReservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/ocr")
@RequiredArgsConstructor
@CrossOrigin(origins = {"*"}, maxAge = 3600, allowedHeaders = "*")
@Slf4j
public class OCRController {

    private final OCRClient ocrClient;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final HotelReservationService hotelReservationService;

    @Value("${KAKAO_REST_KEY}")
    private String kakaoApiKey;

    /**
     * 이미지 파일을 업로드하여 텍스트 추출
     */
    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractText(
            @RequestParam("image") MultipartFile file,
            @RequestParam(value = "threadId", required = false) String threadId,
            @RequestParam(value = "userId", required = false) String userId) {
        try {
            log.info("OCR 요청 - 파일명: {}, 크기: {} bytes, threadId: {}, userId: {}",
                file.getOriginalFilename(), file.getSize(), threadId, userId);

            // OCR 텍스트 추출
            byte[] imageData = file.getBytes();
            String extractedText = ocrClient.extractText(imageData);

            // 문서 타입 감지
            DocumentType documentType = ocrClient.detectDocument(extractedText);

            // Gemini를 사용하여 호텔 정보 추출
            Map<String, Object> hotelInfo = extractHotelInfoWithAI(extractedText);

            // Phase3 일정에 맞게 분류
            Map<String, Object> phaseClassification = classifyForTravelPhases(hotelInfo);

            // DB에 저장 (threadId와 userId가 있고, 호텔 예약인 경우)
            if (threadId != null && userId != null && documentType == DocumentType.HOTEL_RESERVATION) {
                HotelReservation reservation = mapToHotelReservation(hotelInfo);
                if (reservation != null) {
                    try {
                        hotelReservationService.save(threadId, userId, reservation);
                        log.info("호텔 예약 정보 DB 저장 완료 - threadId: {}, userId: {}", threadId, userId);
                    } catch (Exception e) {
                        log.error("호텔 예약 정보 DB 저장 실패", e);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("extractedText", extractedText);
            response.put("documentType", documentType.toString());
            response.put("hotelInfo", hotelInfo);
            response.put("phaseClassification", phaseClassification);

            log.info("OCR 완료 - 문서 타입: {}, 텍스트 길이: {}", documentType, extractedText.length());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("OCR 처리 중 오류", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * URL에서 이미지를 가져와 텍스트 추출
     */
    @PostMapping("/extract-from-url")
    public ResponseEntity<Map<String, Object>> extractTextFromUrl(@RequestBody Map<String, String> request) {
        try {
            String imageUrl = request.get("imageUrl");
            log.info("OCR 요청 (URL) - {}", imageUrl);

            // OCR 텍스트 추출
            String extractedText = ocrClient.extractTextFromUrl(imageUrl);

            // 문서 타입 감지
            DocumentType documentType = ocrClient.detectDocument(extractedText);

            // Gemini를 사용하여 호텔 정보 추출
            Map<String, Object> hotelInfo = extractHotelInfoWithAI(extractedText);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("extractedText", extractedText);
            response.put("documentType", documentType.toString());
            response.put("hotelInfo", hotelInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("OCR 처리 중 오류", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Gemini AI를 사용하여 호텔 정보 추출
     */
    private Map<String, Object> extractHotelInfoWithAI(String text) {
        Map<String, Object> hotelInfo = new HashMap<>();

        try {
            log.info("Gemini를 사용한 호텔 정보 추출 시작");

            String prompt = """
                다음 텍스트는 OCR로 추출한 호텔 예약서입니다.
                이 텍스트에서 호텔 정보를 추출하여 정확히 아래 JSON 형식으로만 응답해주세요.
                JSON 이외의 다른 텍스트는 포함하지 마세요.

                중요:
                - 체크인/체크아웃 날짜가 명시적으로 없다면, Nights(숙박일수)를 기준으로 오늘 날짜부터 계산하세요.
                - 예: 오늘이 2024-09-25이고 Nights가 2이면, checkIn: "2024-09-25", checkOut: "2024-09-27"
                - STAY INFORMATION 섹션을 자세히 확인하세요.
                - 날짜 형식은 반드시 YYYY-MM-DD로 변환하세요.

                추출할 정보:
                - name: 호텔 이름 (예: "LOTTE HOTEL SEOUL", "인천공항 스카이탑 호텔")
                - address: 호텔 주소 (첫 번째 주소 사용, OCR 오류 수정 필요: Euli-ro->Eulji-ro, Jurgy->Jung-gu)
                - checkIn: 체크인 날짜 (YYYY-MM-DD 형식, 없으면 오늘 날짜)
                - checkInTime: 체크인 시간 (HH:mm 형식, 예: "15:00", 없으면 "15:00")
                - checkOut: 체크아웃 날짜 (YYYY-MM-DD 형식, 없으면 checkIn + nights)
                - checkOutTime: 체크아웃 시간 (HH:mm 형식, 예: "11:00", 없으면 "11:00")
                - reservationNumber: 예약 번호 (#제거)
                - phone: 전화번호 (+기호 포함)
                - roomType: 방 유형
                - guestName: 투숙객 이름 ([YOUR NAME]이면 빈 문자열)
                - nights: 숙박 일수

                JSON 형식:
                {
                  "name": "호텔명",
                  "address": "주소",
                  "checkIn": "YYYY-MM-DD",
                  "checkInTime": "HH:mm",
                  "checkOut": "YYYY-MM-DD",
                  "checkOutTime": "HH:mm",
                  "reservationNumber": "예약번호",
                  "phone": "전화번호",
                  "roomType": "방타입",
                  "guestName": "투숙객명",
                  "nights": "숙박일수"
                }

                OCR 텍스트:
                """ + text;

            // Gemini API 호출
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String aiResponse = response.getResult().getOutput().getContent();

            log.info("Gemini 응답: {}", aiResponse);

            // JSON 파싱
            // JSON 문자열에서 ```json 같은 마크다운 제거
            aiResponse = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();

            // ObjectMapper로 JSON 파싱
            Map<String, Object> parsedInfo = objectMapper.readValue(aiResponse, HashMap.class);

            // null이 아닌 값들만 hotelInfo에 추가
            parsedInfo.forEach((key, value) -> {
                if (value != null && !value.toString().isEmpty() &&
                    !value.toString().equals("[YOUR NAME]") &&
                    !value.toString().equals("정보 없음")) {
                    hotelInfo.put(key, value.toString());
                }
            });

            log.info("AI로 추출된 호텔 정보: {}", hotelInfo);

            // 호텔 좌표 추가
            addHotelCoordinates(hotelInfo);

        } catch (Exception e) {
            log.error("Gemini를 사용한 호텔 정보 추출 실패, 기본 파싱 사용", e);
            // 실패 시 기본 파싱 로직 사용
            Map<String, Object> basicInfo = extractHotelInfoBasic(text);
            addHotelCoordinates(basicInfo);
            return basicInfo;
        }

        return hotelInfo;
    }

    /**
     * 기본 호텔 정보 추출 (Gemini 실패 시 fallback)
     */
    private Map<String, Object> extractHotelInfoBasic(String text) {
        Map<String, Object> hotelInfo = new HashMap<>();

        // 호텔 이름 추출
        Pattern hotelNamePattern = Pattern.compile("([A-Za-z]+\\s+HOTEL\\s+[A-Za-z]+|[가-힣A-Za-z\\s]+(?:호텔|Hotel|HOTEL))");
        Matcher hotelNameMatcher = hotelNamePattern.matcher(text);
        if (hotelNameMatcher.find()) {
            String hotelName = hotelNameMatcher.group(1).trim();
            if (!hotelName.contains("VOUCHER") && !hotelName.equals("HOTEL")) {
                hotelInfo.put("name", hotelName);
            }
        }

        // 전화번호 추출
        Pattern phonePattern = Pattern.compile("(?:Tel|전화|Phone)[:\\s]*\\+?([0-9-]+)");
        Matcher phoneMatcher = phonePattern.matcher(text);
        if (phoneMatcher.find()) {
            hotelInfo.put("phone", phoneMatcher.group(1));
        }

        return hotelInfo;
    }

    /**
     * 호텔 주소를 통해 좌표를 추가하는 메서드
     * 카카오맵 API를 사용하여 실제 좌표를 검색
     */
    private void addHotelCoordinates(Map<String, Object> hotelInfo) {
        String hotelAddress = (String) hotelInfo.get("address");
        String hotelName = (String) hotelInfo.get("name");

        if (hotelAddress == null || hotelAddress.isEmpty()) {
            log.warn("호텔 주소가 없습니다. 기본 좌표 사용");
            hotelInfo.put("lat", 37.5665);
            hotelInfo.put("lng", 126.9780);
            return;
        }

        try {
            // 카카오맵 API 호출
            String encodedAddress = java.net.URLEncoder.encode(hotelAddress, "UTF-8");
            String apiUrl = String.format("https://dapi.kakao.com/v2/local/search/address.json?query=%s", encodedAddress);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(apiUrl))
                .header("Authorization", "KakaoAK " + kakaoApiKey)
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // JSON 파싱
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.body());
                com.fasterxml.jackson.databind.JsonNode documents = root.get("documents");

                if (documents != null && documents.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode firstResult = documents.get(0);
                    double lng = firstResult.get("x").asDouble();
                    double lat = firstResult.get("y").asDouble();

                    hotelInfo.put("lat", lat);
                    hotelInfo.put("lng", lng);
                    log.info("카카오맵 API로 좌표 검색 성공: {} -> lat: {}, lng: {}", hotelAddress, lat, lng);
                } else {
                    // 주소 검색 실패 시 키워드 검색 시도
                    searchByKeyword(hotelInfo, hotelName + " " + hotelAddress);
                }
            } else {
                log.error("카카오맵 API 응답 오류: {}", response.statusCode());
                setDefaultCoordinates(hotelInfo);
            }
        } catch (Exception e) {
            log.error("카카오맵 API 호출 중 오류", e);
            setDefaultCoordinates(hotelInfo);
        }
    }

    /**
     * Phase3 여행 일정에 맞게 OCR 결과를 분류
     */
    private Map<String, Object> classifyForTravelPhases(Map<String, Object> hotelInfo) {
        Map<String, Object> classification = new HashMap<>();

        try {
            String prompt = """
                다음은 OCR로 추출한 호텔 예약 정보입니다.
                이 정보를 여행 일정 Phase에 맞게 분류해주세요.

                호텔 정보:
                - 호텔명: %s
                - 체크인: %s %s
                - 체크아웃: %s %s
                - 주소: %s

                여행 일정 구조:
                1. Day 1 시작: 체크인 날짜의 아침 일정부터 시작
                2. Day 중간: 각 날짜의 일정들
                3. Day 마지막: 체크아웃 날짜 오전 일정 후 호텔 체크아웃

                다음 JSON 형식으로 분류해주세요:
                {
                  "checkInDay": "체크인이 포함될 Day 번호 (1부터 시작)",
                  "checkInTime": "체크인 시간 (HH:mm)",
                  "checkOutDay": "체크아웃이 포함될 Day 번호",
                  "checkOutTime": "체크아웃 시간 (HH:mm)",
                  "totalDays": "총 여행 일수",
                  "fixedSchedules": [
                    {
                      "day": "Day 번호",
                      "time": "시간 (HH:mm)",
                      "type": "HOTEL_CHECKIN 또는 HOTEL_CHECKOUT",
                      "location": "장소명",
                      "address": "주소",
                      "latitude": 위도,
                      "longitude": 경도
                    }
                  ]
                }
                """.formatted(
                    hotelInfo.getOrDefault("name", ""),
                    hotelInfo.getOrDefault("checkIn", ""),
                    hotelInfo.getOrDefault("checkInTime", "15:00"),
                    hotelInfo.getOrDefault("checkOut", ""),
                    hotelInfo.getOrDefault("checkOutTime", "11:00"),
                    hotelInfo.getOrDefault("address", "")
                );

            ChatResponse response = chatModel.call(new Prompt(prompt));
            String aiResponse = response.getResult().getOutput().getContent();

            // JSON 파싱
            aiResponse = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            classification = objectMapper.readValue(aiResponse, HashMap.class);

            // 좌표 정보 추가
            if (hotelInfo.containsKey("lat") && hotelInfo.containsKey("lng")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> schedules =
                    (List<Map<String, Object>>) classification.get("fixedSchedules");
                if (schedules != null) {
                    for (Map<String, Object> schedule : schedules) {
                        schedule.put("latitude", hotelInfo.get("lat"));
                        schedule.put("longitude", hotelInfo.get("lng"));
                    }
                }
            }

            log.info("Phase 분류 완료: {}", classification);

        } catch (Exception e) {
            log.error("Phase 분류 실패", e);
            // 기본 분류 반환
            classification.put("checkInDay", 1);
            classification.put("checkInTime", hotelInfo.getOrDefault("checkInTime", "15:00"));
            classification.put("checkOutDay", 2);
            classification.put("checkOutTime", hotelInfo.getOrDefault("checkOutTime", "11:00"));
            classification.put("totalDays", 2);
        }

        return classification;
    }

    // hotelInfo Map을 HotelReservation 객체로 변환
    private HotelReservation mapToHotelReservation(Map<String, Object> hotelInfo) {
        try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            return new HotelReservation(
                (String) hotelInfo.get("name"),
                (String) hotelInfo.get("address"),
                hotelInfo.get("checkIn") != null ? LocalDate.parse((String) hotelInfo.get("checkIn"), dateFormatter) : LocalDate.now(),
                hotelInfo.get("checkInTime") != null ? LocalTime.parse((String) hotelInfo.get("checkInTime"), timeFormatter) : LocalTime.of(15, 0),
                hotelInfo.get("checkOut") != null ? LocalDate.parse((String) hotelInfo.get("checkOut"), dateFormatter) : LocalDate.now().plusDays(1),
                hotelInfo.get("checkOutTime") != null ? LocalTime.parse((String) hotelInfo.get("checkOutTime"), timeFormatter) : LocalTime.of(11, 0),
                (String) hotelInfo.get("roomType"),
                hotelInfo.get("guests") != null ? Integer.parseInt(hotelInfo.get("guests").toString()) : 2,
                (String) hotelInfo.get("reservationNumber"),
                hotelInfo.get("price") != null ? Double.parseDouble(hotelInfo.get("price").toString()) : 0.0,
                hotelInfo.get("nights") != null ? Integer.parseInt(hotelInfo.get("nights").toString()) : 1,
                hotelInfo.get("lat") != null ? Double.parseDouble(hotelInfo.get("lat").toString()) : null,
                hotelInfo.get("lng") != null ? Double.parseDouble(hotelInfo.get("lng").toString()) : null,
                (String) hotelInfo.get("guestName"),
                (String) hotelInfo.get("phone")
            );
        } catch (Exception e) {
            log.error("HotelReservation 변환 중 오류", e);
            return null;
        }
    }

    /**
     * 키워드로 장소 검색 (주소 검색 실패 시 fallback)
     */
    private void searchByKeyword(Map<String, Object> hotelInfo, String keyword) {
        try {
            String encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8");
            String apiUrl = String.format("https://dapi.kakao.com/v2/local/search/keyword.json?query=%s", encodedKeyword);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(apiUrl))
                .header("Authorization", "KakaoAK " + kakaoApiKey)
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.body());
                com.fasterxml.jackson.databind.JsonNode documents = root.get("documents");

                if (documents != null && documents.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode firstResult = documents.get(0);
                    double lng = firstResult.get("x").asDouble();
                    double lat = firstResult.get("y").asDouble();

                    hotelInfo.put("lat", lat);
                    hotelInfo.put("lng", lng);
                    log.info("카카오맵 키워드 검색으로 좌표 찾음: {} -> lat: {}, lng: {}", keyword, lat, lng);
                } else {
                    setDefaultCoordinates(hotelInfo);
                }
            } else {
                setDefaultCoordinates(hotelInfo);
            }
        } catch (Exception e) {
            log.error("카카오맵 키워드 검색 중 오류", e);
            setDefaultCoordinates(hotelInfo);
        }
    }

    /**
     * 기본 좌표 설정 (서울 시청)
     */
    private void setDefaultCoordinates(Map<String, Object> hotelInfo) {
        hotelInfo.put("lat", 37.5665);
        hotelInfo.put("lng", 126.9780);
        log.info("기본 좌표 사용 (서울 시청)");
    }
}