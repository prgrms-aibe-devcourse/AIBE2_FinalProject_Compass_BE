package com.compass.domain.chat.service;

import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.entity.TravelCandidate.TimeBlock;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * CSV 파일 임포트 서비스
 * Pre-Stage: CSV 파일로부터 여행지 데이터를 읽어 DB에 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CSVImportService {

    private final TravelCandidateRepository travelCandidateRepository;

    // CSV 파일이 저장된 디렉토리
    private static final String CSV_DIRECTORY = "/Users/kmj/Documents/GitHub/AIBE2_FinalProject_Compass_BE/list/";

    /**
     * 모든 CSV 파일 임포트
     */
    @Transactional
    public int importAllCSVFiles() {
        log.info("=== CSV 임포트 시작 ===");

        // 기존 데이터 모두 삭제
        clearAllData();

        int totalImported = 0;

        try (Stream<Path> paths = Files.list(Paths.get(CSV_DIRECTORY))) {
            // 하위 디렉토리는 제외하고 현재 디렉토리의 CSV 파일만 찾음
            List<Path> csvFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".csv"))
                .toList();

            log.info("발견된 CSV 파일 수: {}", csvFiles.size());

            for (Path csvFile : csvFiles) {
                int imported = importCSVFile(csvFile.toString());
                totalImported += imported;
                log.info("파일 {} 임포트 완료: {}개", csvFile.getFileName(), imported);
            }
        } catch (IOException e) {
            log.error("CSV 파일 디렉토리 읽기 실패: {}", e.getMessage(), e);
        }

        log.info("=== CSV 임포트 완료: 총 {}개 데이터 저장 ===", totalImported);
        return totalImported;
    }

    /**
     * 특정 CSV 파일 임포트
     */
    @Transactional
    public int importCSVFile(String filePath) {
        log.info("CSV 파일 임포트 시작: {}", filePath);

        List<TravelCandidate> candidates = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> allRows = reader.readAll();

            // 헤더 스킵
            boolean isFirstRow = true;
            for (String[] row : allRows) {
                if (isFirstRow) {
                    isFirstRow = false;
                    log.info("헤더 스킵: {}", String.join(", ", row));
                    continue;
                }

                // parseCSVRow에서 region과 subRegion은 사용하지 않으므로 null 전달
                TravelCandidate candidate = parseCSVRow(row, null, null);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }

            // 배치 저장
            List<TravelCandidate> saved = travelCandidateRepository.saveAll(candidates);
            log.info("{}개 데이터 저장 완료", saved.size());

            return saved.size();

        } catch (IOException | CsvException e) {
            log.error("CSV 파일 읽기 실패: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * CSV 행을 TravelCandidate 엔티티로 변환
     */
    private TravelCandidate parseCSVRow(String[] row, String region, String subRegion) {
        try {
            // CSV 형식 체크 (최소 6개 컬럼 필요)
            if (row.length >= 6) {
                // CSV 컬럼 순서:
                // 0: 시간 블록, 1: #(번호), 2: 이름, 3: 지역, 4: 주소, 5: 카테고리, 6: 구글맵 주소(선택)
                return parseDbCsvRow(row);
            } else {
                log.warn("CSV 행의 컬럼 수가 부족합니다: {} 개", row.length);
                return null;
            }
        } catch (Exception e) {
            log.warn("CSV 행 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * db.csv 형식 파싱
     */
    private TravelCandidate parseDbCsvRow(String[] row) {
        // CSV 컬럼 순서:
        // 0: 시간 블록, 1: #(번호), 2: 이름, 3: 지역, 4: 주소, 5: 카테고리, 6: 구글맵 주소(선택)

        // 로그로 데이터 확인
        log.debug("CSV 행 파싱: {}", String.join(" | ", row));

        // 시간블록 파싱
        String timeBlockStr = row[0];
        TimeBlock timeBlock = parseTimeBlockFromString(timeBlockStr);

        // 주소에서 구 정보 추출
        String address = row[4];
        String subRegion = "";
        if (address.contains("종로구")) subRegion = "종로구";
        else if (address.contains("중구")) subRegion = "중구";
        else if (address.contains("강남구")) subRegion = "강남구";
        else if (address.contains("마포구")) subRegion = "마포구";
        else if (address.contains("용산구")) subRegion = "용산구";
        else if (address.contains("성동구")) subRegion = "성동구";
        else if (address.contains("광진구")) subRegion = "광진구";
        else if (address.contains("관악구")) subRegion = "관악구";
        else if (address.contains("영등포구")) subRegion = "영등포구";
        else if (address.contains("서초구")) subRegion = "서초구";
        else if (address.contains("강서구")) subRegion = "강서구";
        else if (address.contains("성북구")) subRegion = "성북구";

        // 카테고리에서 괄호 내용을 description으로 사용
        String category = row[5];
        String description = "";
        if (category.contains("(")) {
            int startIdx = category.indexOf("(");
            int endIdx = category.indexOf(")");
            if (endIdx > startIdx) {
                description = category.substring(startIdx + 1, endIdx);
                category = category.substring(0, startIdx).trim();
            }
        }

        // 더 구체적인 설명 추가
        if (description.isEmpty()) {
            description = String.format("%s의 대표 %s", row[3], category);
        }

        TravelCandidate candidate = TravelCandidate.builder()
            .placeId(generatePlaceId())
            .name(row[2])  // 이름
            .region(row[3])  // 지역
            .subRegion(subRegion)
            .category(category)
            .timeBlock(timeBlock)
            .address(address)  // 실제 주소
            .latitude(37.5172 + (Math.random() - 0.5) * 0.2)  // 서울 중심 좌표 + 랜덤
            .longitude(127.0473 + (Math.random() - 0.5) * 0.2)  // 서울 중심 좌표 + 랜덤
            .rating(4.0 + Math.random())  // 4.0~5.0 사이 랜덤
            .reviewCount((int)(Math.random() * 1000) + 100)  // 100~1100 사이 랜덤
            .priceLevel((int)(Math.random() * 3) + 1)  // 1~3 사이 랜덤
            .description(description)
            .isActive(true)
            .build();

        return candidate;
    }

    /**
     * 기존 CSV 형식 파싱
     */
    private TravelCandidate parseOldCsvRow(String[] row, String region, String subRegion) {
        // 시간블록 파싱
        String timeBlockStr = row[0];
        TimeBlock timeBlock = parseTimeBlockFromString(timeBlockStr);

        // 세부위치를 subRegion으로 사용
        String subRegionFromCsv = row.length > 3 ? row[3] : subRegion;

        TravelCandidate candidate = TravelCandidate.builder()
            .placeId(generatePlaceId())  // Google Place ID 대신 UUID 생성
            .name(row[1])
            .region(region)
            .subRegion(subRegionFromCsv)
            .category(row[2])
            .timeBlock(timeBlock)
            .address(region + " " + subRegionFromCsv)  // 임시 주소
            .latitude(37.5172 + (Math.random() - 0.5) * 0.1)  // 서울 중심 좌표 + 랜덤
            .longitude(127.0473 + (Math.random() - 0.5) * 0.1)  // 서울 중심 좌표 + 랜덤
            .rating(4.0 + Math.random())  // 4.0~5.0 사이 랜덤
            .reviewCount((int)(Math.random() * 1000) + 100)  // 100~1100 사이 랜덤
            .priceLevel((int)(Math.random() * 3) + 1)  // 1~3 사이 랜덤
            .description(row.length > 4 ? row[4] : null)  // 비고란 활용
            .isActive(true)
            .build();

        return candidate;
    }

    /**
     * 시간블록 문자열에서 TimeBlock enum으로 변환
     */
    private TimeBlock parseTimeBlockFromString(String timeBlockStr) {
        if (timeBlockStr == null || timeBlockStr.isEmpty()) {
            return TimeBlock.MORNING_ACTIVITY;
        }

        // "아침식사(08:00-10:00)" 형태에서 시간블록 추출
        if (timeBlockStr.contains("아침식사")) {
            return TimeBlock.BREAKFAST;
        } else if (timeBlockStr.contains("오전일과")) {
            return TimeBlock.MORNING_ACTIVITY;
        } else if (timeBlockStr.contains("점심식사")) {
            return TimeBlock.LUNCH;
        } else if (timeBlockStr.contains("오후일과")) {
            return TimeBlock.AFTERNOON_ACTIVITY;
        } else if (timeBlockStr.contains("저녁식사")) {
            return TimeBlock.DINNER;
        } else if (timeBlockStr.contains("저녁일과")) {
            return TimeBlock.EVENING_ACTIVITY;
        }

        return TimeBlock.MORNING_ACTIVITY;  // 기본값
    }


    /**
     * 영문 지역명을 한글로 매핑
     */
    private String mapEnglishToKorean(String english) {
        // 한글이 포함되어 있으면 한글 추출
        if (english.contains("부산")) return "부산";
        if (english.contains("서울")) return "서울";
        if (english.contains("인천")) return "인천";
        if (english.contains("대구")) return "대구";
        if (english.contains("대전")) return "대전";
        if (english.contains("광주")) return "광주";
        if (english.contains("울산")) return "울산";
        if (english.contains("제주")) return "제주";
        if (english.contains("경주")) return "경주";
        if (english.contains("강릉")) return "강릉";
        if (english.contains("전주")) return "전주";
        if (english.contains("여수")) return "여수";
        if (english.contains("속초")) return "속초";
        if (english.contains("안동")) return "안동";
        if (english.contains("통영")) return "통영";

        return switch (english.toLowerCase()) {
            case "seoul" -> "서울";
            case "busan" -> "부산";
            case "incheon" -> "인천";
            case "daegu" -> "대구";
            case "daejeon" -> "대전";
            case "gwangju" -> "광주";
            case "ulsan" -> "울산";
            case "jeju" -> "제주";
            case "gyeongju" -> "경주";
            case "gangneung" -> "강릉";
            case "jeonju" -> "전주";
            case "yeosu" -> "여수";
            case "sokcho" -> "속초";
            case "andong" -> "안동";
            case "tongyeong" -> "통영";
            default -> english;
        };
    }

    /**
     * 기존 데이터 모두 삭제
     */
    @Transactional
    public void clearAllData() {
        log.info("기존 TravelCandidate 데이터 모두 삭제");
        travelCandidateRepository.deleteAllData();
        log.info("데이터 삭제 완료");
    }

    /**
     * Place ID 생성 (UUID 사용)
     */
    private String generatePlaceId() {
        return "csv_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 문자열을 Double로 파싱
     */
    private Double parseDouble(String value) {
        if (value == null || value.isEmpty() || value.equals("null")) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 문자열을 Integer로 파싱
     */
    private Integer parseInt(String value) {
        if (value == null || value.isEmpty() || value.equals("null")) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}