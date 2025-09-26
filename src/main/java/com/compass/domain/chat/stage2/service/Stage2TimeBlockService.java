package com.compass.domain.chat.stage2.service;

import com.compass.domain.chat.common.constants.StageConstants;
import com.compass.domain.chat.common.utils.DistanceCalculator;
import com.compass.domain.chat.common.utils.PlaceScoreCalculator;
import com.compass.domain.chat.common.utils.TravelPlaceConverter;
import com.compass.domain.chat.entity.TravelCandidate;
import com.compass.domain.chat.model.TravelPlace;
import com.compass.domain.chat.model.dto.ConfirmedSchedule;
import com.compass.domain.chat.model.context.TravelContext;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.orchestrator.ContextManager;
import com.compass.domain.chat.repository.TravelCandidateRepository;
import com.compass.domain.chat.stage2.dto.UserSelectionRequest;
import com.compass.domain.chat.stage2.dto.UserSelectionRequest.SelectedPlace;
import com.compass.domain.chat.stage2.dto.SelectedSchedule;
import com.compass.domain.chat.service.TravelInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final ContextManager contextManager;
    private final TravelInfoService travelInfoService;

    // 사용자 선택 기반 시간블록별 후보 생성
    public Stage2Response processUserSelection(UserSelectionRequest request) {
        log.info("Stage 2: 시간블록 기반 후보 생성 시작 - threadId: {}", request.threadId());

        Map<Integer, DaySchedule> timeBlocks = new HashMap<>();

        List<ConfirmedSchedule> confirmedSchedules = getConfirmedSchedules(request.threadId());
        TravelFormSubmitRequest travelInfo = travelInfoService.loadTravelInfo(request.threadId());
        LocalDate tripStartDate = resolveTripStartDate(travelInfo, confirmedSchedules);
        List<String> regions = resolveRegions(travelInfo, request.selectedPlaces());

        // 1. 확정 일정 (OCR/티켓) 우선 배치
        placeConfirmedSchedules(confirmedSchedules, timeBlocks, request.tripDays(), tripStartDate);

        // 2. 사용자 선택 장소 배치
        List<SelectedSchedule> selectedSchedules = placeUserSelections(request.selectedPlaces(), timeBlocks, request.tripDays());

        // 3. AI 추천 후보 생성
        generateAIRecommendations(timeBlocks, request.selectedPlaces(), request.threadId(), regions);

        return Stage2Response.builder()
            .threadId(request.threadId())
            .tripDays(request.tripDays())
            .timeBlocks(timeBlocks)
            .build();
    }

    // 확정 일정 조회 (OCR/티켓 정보)
    private List<ConfirmedSchedule> getConfirmedSchedules(String threadId) {
        return contextManager.getContext(threadId)
            .map(TravelContext::getOcrConfirmedSchedules)
            .map(ArrayList::new)
            .orElseGet(ArrayList::new);
    }

    // 확정 일정 배치
    private void placeConfirmedSchedules(List<ConfirmedSchedule> schedules,
                                        Map<Integer, DaySchedule> timeBlocks,
                                        int tripDays,
                                        LocalDate tripStartDate) {
        for (ConfirmedSchedule schedule : schedules) {
            int day = calculateDay(schedule.startTime(), tripStartDate, tripDays);
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
                                          String threadId,
                                          List<String> regions) {
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
                    block, referencePlaces, threadId, regions
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
                                                        String threadId,
                                                        List<String> regions) {
        if (regions.isEmpty()) {
            log.debug("AI 후보 생성을 건너뜀 - region 정보를 찾지 못했습니다 (threadId={})", threadId);
            return List.of();
        }

        TravelCandidate.TimeBlock entityTimeBlock = mapToCandidateTimeBlock(block);

        int fetchLimit = StageConstants.Limits.PLACES_PER_CLUSTER * Math.max(regions.size(), 1);

        List<TravelCandidate> filtered = travelCandidateRepository.findActiveByRegionsAndTimeBlock(
            regions,
            entityTimeBlock,
            fetchLimit
        );

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
    private int calculateDay(LocalDateTime dateTime, LocalDate tripStartDate, int maxDays) {
        if (maxDays <= 0) {
            return 1;
        }
        if (tripStartDate != null) {
            long diff = ChronoUnit.DAYS.between(tripStartDate, dateTime.toLocalDate());
            int day = (int) diff + 1;
            if (day < 1) {
                return 1;
            }
            return Math.min(day, maxDays);
        }
        return Math.min(1, maxDays);
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

    private TravelCandidate.TimeBlock mapToCandidateTimeBlock(TimeBlock block) {
        return switch (block) {
            case BREAKFAST -> TravelCandidate.TimeBlock.BREAKFAST;
            case MORNING_ACTIVITY -> TravelCandidate.TimeBlock.MORNING_ACTIVITY;
            case LUNCH -> TravelCandidate.TimeBlock.LUNCH;
            case AFTERNOON_ACTIVITY -> TravelCandidate.TimeBlock.AFTERNOON_ACTIVITY;
            case DINNER -> TravelCandidate.TimeBlock.DINNER;
            case EVENING_ACTIVITY -> TravelCandidate.TimeBlock.EVENING_ACTIVITY;
        };
    }

    private LocalDate resolveTripStartDate(TravelFormSubmitRequest travelInfo,
                                           List<ConfirmedSchedule> confirmedSchedules) {
        if (travelInfo.travelDates() != null && travelInfo.travelDates().startDate() != null) {
            return travelInfo.travelDates().startDate();
        }

        return confirmedSchedules.stream()
            .map(schedule -> schedule.startTime().toLocalDate())
            .sorted()
            .findFirst()
            .orElse(null);
    }

    private List<String> resolveRegions(TravelFormSubmitRequest travelInfo,
                                        List<SelectedPlace> selections) {
        if (travelInfo.destinations() != null && !travelInfo.destinations().isEmpty()) {
            return new ArrayList<>(travelInfo.destinations());
        }

        if (selections == null || selections.isEmpty()) {
            return List.of();
        }

        return selections.stream()
            .map(SelectedPlace::placeId)
            .map(this::lookupRegionByPlaceId)
            .flatMap(Optional::stream)
            .distinct()
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Optional<String> lookupRegionByPlaceId(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return Optional.empty();
        }
        return travelCandidateRepository.findFirstByPlaceId(placeId)
            .map(TravelCandidate::getRegion);
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
