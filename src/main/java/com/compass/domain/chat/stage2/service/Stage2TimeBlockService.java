package com.compass.domain.chat.stage2.service;

import com.compass.domain.chat.common.constants.StageConstants;
import com.compass.domain.chat.common.utils.DistanceCalculator;
import com.compass.domain.chat.common.utils.PlaceScoreCalculator;
import com.compass.domain.chat.common.utils.TravelPlaceConverter;
import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.stage2.dto.UserSelectionRequest;
import com.compass.domain.chat.stage2.dto.UserSelectionRequest.SelectedPlace;
import com.compass.domain.chat.stage2.dto.SelectedSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// Stage 2: 시간블록 기반 후보지 선정 서비스 (리팩토링 버전)
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Stage2TimeBlockService {

    private final TravelCandidateRepository travelCandidateRepository;
    private final DistanceCalculator distanceCalculator;
    private final PlaceScoreCalculator scoreCalculator;
    private final TravelPlaceConverter placeConverter;

    // 사용자 선택 기반 시간블록별 후보 생성
    public Stage2Response processUserSelection(UserSelectionRequest request) {
        log.info("Stage 2: 시간블록 기반 후보 생성 시작 - threadId: {}", request.threadId());

        Map<Integer, DaySchedule> timeBlocks = new HashMap<>();

        // 1. 확정 일정 (OCR/티켓) 우선 배치
        List<ConfirmedSchedule> confirmedSchedules = getConfirmedSchedules(request.threadId());
        placeConfirmedSchedules(confirmedSchedules, timeBlocks, request.tripDays());

        // 2. 사용자 선택 장소 배치
        List<SelectedSchedule> selectedSchedules = placeUserSelections(request.selectedPlaces(), timeBlocks, request.tripDays());

        // 3. AI 추천 후보 생성
        generateAIRecommendations(timeBlocks, request.selectedPlaces(), request.threadId());

        return Stage2Response.builder()
            .threadId(request.threadId())
            .tripDays(request.tripDays())
            .timeBlocks(timeBlocks)
            .build();
    }

    // 확정 일정 조회 (OCR/티켓 정보)
    private List<ConfirmedSchedule> getConfirmedSchedules(String threadId) {
        // TODO: 실제 구현시 DB에서 조회
        return new ArrayList<>();
    }

    // 확정 일정 배치
    private void placeConfirmedSchedules(List<ConfirmedSchedule> schedules,
                                        Map<Integer, DaySchedule> timeBlocks,
                                        int tripDays) {
        for (ConfirmedSchedule schedule : schedules) {
            int day = calculateDay(schedule.startTime(), tripDays);
            TimeBlock block = getTimeBlock(schedule.startTime());

            ensureDaySchedule(timeBlocks, day);
            TimeBlockCandidates candidates = timeBlocks.get(day).timeBlocks()
                .computeIfAbsent(block, k -> new TimeBlockCandidates(
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
                ));

            candidates.ocrSchedules().add(schedule);
            log.debug("확정 일정 배치: {} -> Day {} {}",
                schedule.title(), day, block);
        }
    }

    // 사용자 선택 장소 배치
    private List<SelectedSchedule> placeUserSelections(List<SelectedPlace> selections,
                                    Map<Integer, DaySchedule> timeBlocks,
                                    int tripDays) {
        List<SelectedSchedule> selectedSchedules = new ArrayList<>();

        // 카테고리별로 적절한 시간블록에 배치
        for (int i = 0; i < selections.size(); i++) {
            SelectedPlace place = selections.get(i);
            int day = (i / 6) + 1; // 6개 시간블록마다 다음 날로
            if (day > tripDays) day = tripDays;

            TimeBlock block = findBestTimeBlock(place.category(),
                timeBlocks.get(day));

            ensureDaySchedule(timeBlocks, day);
            SelectedSchedule selected = createSelectedSchedule(place);

            TimeBlockCandidates candidates = timeBlocks.get(day).timeBlocks()
                .computeIfAbsent(block, k -> new TimeBlockCandidates(
                    new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
                ));

            candidates.userSelected().add(selected);
            selectedSchedules.add(selected);
            log.debug("사용자 선택 배치: {} -> Day {} {}",
                place.name(), day, block);
        }

        return selectedSchedules;
    }

    // AI 추천 후보 생성
    private void generateAIRecommendations(Map<Integer, DaySchedule> timeBlocks,
                                          List<SelectedPlace> userSelections,
                                          String threadId) {
        // 사용자 선택 장소들을 기준점으로 사용
        List<TravelPlace> referencePlaces = placeConverter.fromSelectedPlaces(userSelections);

        for (Map.Entry<Integer, DaySchedule> dayEntry : timeBlocks.entrySet()) {
            int day = dayEntry.getKey();
            DaySchedule daySchedule = dayEntry.getValue();

            for (TimeBlock block : TimeBlock.values()) {
                TimeBlockCandidates candidates = daySchedule.timeBlocks()
                    .computeIfAbsent(block, k -> new TimeBlockCandidates(
                        new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
                    ));

                // 이미 2개 이상 확정되면 스킵
                int confirmedCount = candidates.ocrSchedules().size() + candidates.userSelected().size();
                if (confirmedCount >= StageConstants.TimeBlock.MAX_PLACES_PER_BLOCK) {
                    continue;
                }

                // AI 후보 생성
                List<TravelPlace> aiCandidates = findCandidatesForTimeBlock(
                    block, referencePlaces, threadId
                );

                candidates.aiCandidates().addAll(aiCandidates);
                log.debug("Day {} {} - AI 후보 {}개 생성",
                    day, block, aiCandidates.size());
            }
        }
    }

    // 시간블록에 맞는 후보 찾기
    private List<TravelPlace> findCandidatesForTimeBlock(TimeBlock block,
                                                        List<TravelPlace> references,
                                                        String threadId) {
        // TODO: 실제 구현시 지역 정보 활용
        String region = "서울"; // 임시

        List<TravelCandidate> candidates = travelCandidateRepository.findByRegion(region);

        // 시간블록에 맞는 카테고리 필터링
        List<TravelCandidate> filtered = filterByTimeBlock(candidates, block);

        // 점수 계산 및 정렬
        return filtered.stream()
            .map(c -> new ScoredCandidate(
                placeConverter.fromCandidate(c),
                scoreCalculator.calculateScoreWithMultipleReferences(
                    placeConverter.fromCandidate(c), references
                )
            ))
            .sorted(Comparator.comparing(ScoredCandidate::score).reversed())
            .limit(StageConstants.Limits.PLACES_PER_CLUSTER)
            .map(ScoredCandidate::place)
            .collect(Collectors.toList());
    }

    // 시간블록에 맞는 카테고리 필터링
    private List<TravelCandidate> filterByTimeBlock(List<TravelCandidate> candidates,
                                                   TimeBlock block) {
        return candidates.stream()
            .filter(c -> matchesTimeBlock(c.getCategory(), block))
            .collect(Collectors.toList());
    }

    // 카테고리와 시간블록 매칭
    private boolean matchesTimeBlock(String category, TimeBlock block) {
        if (category == null) return false;

        return switch (block) {
            case BREAKFAST -> category.contains("아침") || category.contains("브런치");
            case LUNCH -> category.contains("점심") || category.contains("맛집");
            case DINNER -> category.contains("저녁") || category.contains("맛집");
            case MORNING_ACTIVITY -> category.contains("관광") || category.contains("명소");
            case AFTERNOON_ACTIVITY -> category.contains("체험") || category.contains("카페");
            case EVENING_ACTIVITY -> category.contains("야경") || category.contains("공연");
        };
    }

    // 카테고리에 맞는 시간블록 찾기
    private TimeBlock findBestTimeBlock(String category, DaySchedule daySchedule) {
        TimeBlock defaultBlock = switch (category) {
            case StageConstants.Category.RESTAURANT -> TimeBlock.LUNCH;
            case StageConstants.Category.CAFE -> TimeBlock.AFTERNOON_ACTIVITY;
            case StageConstants.Category.TOURIST_ATTRACTION -> TimeBlock.MORNING_ACTIVITY;
            case StageConstants.Category.SHOPPING -> TimeBlock.AFTERNOON_ACTIVITY;
            case StageConstants.Category.NIGHT_VIEW -> TimeBlock.EVENING_ACTIVITY;
            default -> TimeBlock.AFTERNOON_ACTIVITY;
        };

        // 이미 꽉 찬 경우 다른 블록 찾기
        if (daySchedule != null) {
            TimeBlockCandidates candidates = daySchedule.timeBlocks().get(defaultBlock);
            if (candidates != null) {
                int confirmedCount = candidates.ocrSchedules().size() + candidates.userSelected().size();
                if (confirmedCount >= StageConstants.TimeBlock.MAX_PLACES_PER_BLOCK) {
                    // 비어있는 블록 찾기
                    for (TimeBlock block : TimeBlock.values()) {
                        TimeBlockCandidates alt = daySchedule.timeBlocks().get(block);
                        if (alt == null) {
                            return block;
                        }
                        int altCount = alt.ocrSchedules().size() + alt.userSelected().size();
                        if (altCount < StageConstants.TimeBlock.MAX_PLACES_PER_BLOCK) {
                            return block;
                        }
                    }
                }
            }
        }

        return defaultBlock;
    }

    // 날짜 계산
    private int calculateDay(LocalDateTime dateTime, int maxDays) {
        // TODO: 실제 구현시 여행 시작일 기준으로 계산
        return 1;
    }

    // 시간블록 결정
    private TimeBlock getTimeBlock(LocalDateTime dateTime) {
        int hour = dateTime.getHour();

        if (hour >= StageConstants.TimeBlock.BREAKFAST_START &&
            hour < StageConstants.TimeBlock.BREAKFAST_END) {
            return TimeBlock.BREAKFAST;
        } else if (hour >= StageConstants.TimeBlock.MORNING_ACTIVITY_START &&
                  hour < StageConstants.TimeBlock.MORNING_ACTIVITY_END) {
            return TimeBlock.MORNING_ACTIVITY;
        } else if (hour >= StageConstants.TimeBlock.LUNCH_START &&
                  hour < StageConstants.TimeBlock.LUNCH_END) {
            return TimeBlock.LUNCH;
        } else if (hour >= StageConstants.TimeBlock.AFTERNOON_ACTIVITY_START &&
                  hour < StageConstants.TimeBlock.AFTERNOON_ACTIVITY_END) {
            return TimeBlock.AFTERNOON_ACTIVITY;
        } else if (hour >= StageConstants.TimeBlock.DINNER_START &&
                  hour < StageConstants.TimeBlock.DINNER_END) {
            return TimeBlock.DINNER;
        } else if (hour >= StageConstants.TimeBlock.EVENING_ACTIVITY_START &&
                  hour < StageConstants.TimeBlock.EVENING_ACTIVITY_END) {
            return TimeBlock.EVENING_ACTIVITY;
        }

        return TimeBlock.AFTERNOON_ACTIVITY; // 기본값
    }

    // DaySchedule 확인 및 생성
    private void ensureDaySchedule(Map<Integer, DaySchedule> timeBlocks, int day) {
        timeBlocks.computeIfAbsent(day, k -> new DaySchedule(
            day, new HashMap<>()
        ));
    }

    // SelectedPlace를 SelectedSchedule로 변환
    private SelectedSchedule createSelectedSchedule(SelectedPlace place) {
        return SelectedSchedule.userSelected(
            place.placeId(),
            place.name(),
            place.category(),
            place.address(),
            place.latitude(),
            place.longitude(),
            place.rating()
        );
    }

    // 점수가 계산된 후보
    private record ScoredCandidate(TravelPlace place, double score) {}

    // 시간블록 열거형
    public enum TimeBlock {
        BREAKFAST(7, 9),
        MORNING_ACTIVITY(9, 12),
        LUNCH(12, 14),
        AFTERNOON_ACTIVITY(14, 18),
        DINNER(18, 20),
        EVENING_ACTIVITY(20, 22);

        public final int startHour;
        public final int endHour;

        TimeBlock(int startHour, int endHour) {
            this.startHour = startHour;
            this.endHour = endHour;
        }
    }

    // 시간블록별 후보
    public record TimeBlockCandidates(
        List<ConfirmedSchedule> ocrSchedules,     // OCR로 확정된 일정
        List<SelectedSchedule> userSelected,      // 사용자 선택 장소
        List<TravelPlace> aiCandidates            // AI 추천 후보
    ) {}

    // 일별 시간블록 일정
    public record DaySchedule(
        int day,
        Map<TimeBlock, TimeBlockCandidates> timeBlocks
    ) {}

    // Stage 2 응답
    public record Stage2Response(
        String threadId,
        int tripDays,
        Map<Integer, DaySchedule> timeBlocks
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String threadId;
            private int tripDays;
            private Map<Integer, DaySchedule> timeBlocks;

            public Builder threadId(String threadId) {
                this.threadId = threadId;
                return this;
            }

            public Builder tripDays(int tripDays) {
                this.tripDays = tripDays;
                return this;
            }

            public Builder timeBlocks(Map<Integer, DaySchedule> timeBlocks) {
                this.timeBlocks = timeBlocks;
                return this;
            }

            public Stage2Response build() {
                return new Stage2Response(threadId, tripDays, timeBlocks);
            }
        }
    }
}