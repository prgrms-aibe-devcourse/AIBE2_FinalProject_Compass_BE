package com.compass.domain.chat.service;

import com.compass.domain.chat.dto.FollowUpQuestionDto;
import com.compass.domain.chat.dto.TravelInfoStatusDto;
import com.compass.domain.chat.dto.TripPlanningRequest;
import com.compass.domain.chat.engine.QuestionFlowEngine;
import com.compass.domain.chat.entity.ChatThread;
import com.compass.domain.chat.entity.TravelInfoCollectionState;
import com.compass.domain.chat.repository.ChatThreadRepository;
import com.compass.domain.chat.repository.TravelInfoCollectionRepository;
import com.compass.domain.user.entity.User;
import com.compass.domain.user.enums.Role;
import com.compass.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

/**
 * TravelInfoCollectionService 단위 테스트
 * REQ-FOLLOW-002: 정보 수집 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("TravelInfoCollectionService 테스트")
class TravelInfoCollectionServiceTest {
    
    @Mock
    private TravelInfoCollectionRepository collectionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private ChatThreadRepository chatThreadRepository;
    
    @Mock
    private FollowUpQuestionGenerator questionGenerator;
    
    @Mock
    private NaturalLanguageParsingService parsingService;
    
    @Mock
    private QuestionFlowEngine flowEngine;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private TravelInfoCollectionService service;
    
    private User testUser;
    private ChatThread testThread;
    private TravelInfoCollectionState testState;
    
    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@test.com")
                .nickname("테스트유저")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
        
        testThread = new ChatThread();
        testThread.setId("thread-1");
        testThread.setUser(testUser);
        
        testState = TravelInfoCollectionState.builder()
                .id(1L)
                .user(testUser)
                .chatThread(testThread)
                .sessionId("TIC_TEST1234")
                .currentStep(TravelInfoCollectionState.CollectionStep.INITIAL)
                .isCompleted(false)
                .destinationCollected(false)
                .datesCollected(false)
                .durationCollected(false)
                .companionsCollected(false)
                .budgetCollected(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    @Test
    @DisplayName("새로운 정보 수집 세션을 시작할 수 있다")
    void testStartInfoCollection() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatThreadRepository.findById("thread-1")).thenReturn(Optional.of(testThread));
        when(collectionRepository.findFirstByUserAndIsCompletedFalseOrderByCreatedAtDesc(testUser))
                .thenReturn(Optional.empty());
        when(collectionRepository.save(any(TravelInfoCollectionState.class))).thenReturn(testState);
        
        FollowUpQuestionDto expectedQuestion = FollowUpQuestionDto.builder()
                .sessionId("TIC_TEST1234")
                .primaryQuestion("어디에서 출발하시나요? 🛫")
                .currentStep(TravelInfoCollectionState.CollectionStep.ORIGIN)
                .build();
        when(flowEngine.generateNextQuestion(any(TravelInfoCollectionState.class)))
                .thenReturn(expectedQuestion);
        
        // When
        FollowUpQuestionDto result = service.startInfoCollection(1L, "thread-1", "제주도 여행 가고 싶어요");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPrimaryQuestion()).contains("어디에서 출발");
        verify(collectionRepository).save(any(TravelInfoCollectionState.class));
        verify(flowEngine).generateNextQuestion(any(TravelInfoCollectionState.class));
    }
    
    @Test
    @DisplayName("기존 미완료 세션이 있으면 계속 사용한다")
    void testContinueExistingSession() {
        // Given
        testState.setCreatedAt(LocalDateTime.now().minusHours(1)); // 아직 만료되지 않음
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(collectionRepository.findFirstByUserAndIsCompletedFalseOrderByCreatedAtDesc(testUser))
                .thenReturn(Optional.of(testState));
        
        FollowUpQuestionDto expectedQuestion = FollowUpQuestionDto.builder()
                .sessionId("TIC_TEST1234")
                .primaryQuestion("어디로 여행을 가시나요?")
                .currentStep(TravelInfoCollectionState.CollectionStep.DESTINATION)
                .build();
        when(flowEngine.generateNextQuestion(testState)).thenReturn(expectedQuestion);
        
        // When
        FollowUpQuestionDto result = service.startInfoCollection(1L, null, null);
        
        // Then
        assertThat(result).isNotNull();
        verify(collectionRepository, never()).save(any(TravelInfoCollectionState.class));
        verify(flowEngine).generateNextQuestion(testState);
    }
    
    @Test
    @DisplayName("후속 응답을 처리하고 다음 질문을 생성한다")
    void testProcessFollowUpResponse() {
        // Given
        when(collectionRepository.findBySessionId("TIC_TEST1234")).thenReturn(Optional.of(testState));
        when(collectionRepository.save(any(TravelInfoCollectionState.class))).thenReturn(testState);
        
        FollowUpQuestionDto expectedQuestion = FollowUpQuestionDto.builder()
                .sessionId("TIC_TEST1234")
                .primaryQuestion("언제 여행을 가시나요?")
                .currentStep(TravelInfoCollectionState.CollectionStep.DATES)
                .build();
        when(flowEngine.processResponse(any(TravelInfoCollectionState.class), anyString()))
                .thenReturn(testState);
        when(flowEngine.isFlowComplete(any(TravelInfoCollectionState.class)))
                .thenReturn(false);
        when(flowEngine.generateNextQuestion(any(TravelInfoCollectionState.class)))
                .thenReturn(expectedQuestion);
        
        // When
        FollowUpQuestionDto result = service.processFollowUpResponse("TIC_TEST1234", "제주도 2박3일로 가려고 해요");
        
        // Then
        assertThat(result).isNotNull();
        verify(flowEngine).processResponse(any(TravelInfoCollectionState.class), anyString());
        verify(collectionRepository).save(any(TravelInfoCollectionState.class));
        verify(flowEngine).generateNextQuestion(any(TravelInfoCollectionState.class));
    }
    
    @Test
    @DisplayName("완료된 세션에 대한 후속 응답 처리 시 예외가 발생한다")
    void testProcessFollowUpResponseForCompletedSession() {
        // Given
        testState.setCompleted(true);
        when(collectionRepository.findBySessionId("TIC_TEST1234")).thenReturn(Optional.of(testState));
        
        // When & Then
        assertThatThrownBy(() -> 
                service.processFollowUpResponse("TIC_TEST1234", "추가 응답"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 완료된 수집 세션입니다");
    }
    
    @Test
    @DisplayName("모든 정보가 수집되면 수집을 완료할 수 있다")
    void testCompleteCollection() {
        // Given
        testState.setOrigin("서울");
        testState.setOriginCollected(true);
        testState.setDestination("제주도");
        testState.setDestinationCollected(true);
        testState.setStartDate(LocalDate.now().plusDays(7));
        testState.setEndDate(LocalDate.now().plusDays(9));
        testState.setDatesCollected(true);
        testState.setDurationNights(2);
        testState.setDurationCollected(true);
        testState.setNumberOfTravelers(2);
        testState.setCompanionType("couple");
        testState.setCompanionsCollected(true);
        testState.setBudgetLevel("moderate");
        testState.setBudgetCollected(true);
        
        when(collectionRepository.findBySessionId("TIC_TEST1234")).thenReturn(Optional.of(testState));
        when(collectionRepository.save(any(TravelInfoCollectionState.class))).thenReturn(testState);
        
        // When
        TripPlanningRequest result = service.completeCollection("TIC_TEST1234");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDestination()).isEqualTo("제주도");
        assertThat(result.getNumberOfTravelers()).isEqualTo(2);
        assertThat(testState.isCompleted()).isTrue();
        verify(collectionRepository).save(testState);
    }
    
    @Test
    @DisplayName("필수 정보가 부족하면 완료할 수 없다")
    void testCompleteCollectionWithMissingInfo() {
        // Given
        testState.setDestination("제주도");
        testState.setDestinationCollected(true);
        // 다른 정보는 수집되지 않음
        
        when(collectionRepository.findBySessionId("TIC_TEST1234")).thenReturn(Optional.of(testState));
        
        // When & Then
        assertThatThrownBy(() -> 
                service.completeCollection("TIC_TEST1234"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("다음 정보가 필요합니다");
    }
    
    @Test
    @DisplayName("수집 상태를 조회할 수 있다")
    void testGetCollectionStatus() {
        // Given
        testState.setDestination("제주도");
        testState.setDestinationCollected(true);
        when(collectionRepository.findBySessionId("TIC_TEST1234")).thenReturn(Optional.of(testState));
        
        // When
        TravelInfoStatusDto result = service.getCollectionStatus("TIC_TEST1234");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo("TIC_TEST1234");
        assertThat(result.getCompletionPercentage()).isEqualTo(14); // 1/7 = 14.29 -> 14%
        assertThat(result.getFieldStatus().isDestinationCollected()).isTrue();
    }
    
    @Test
    @DisplayName("특정 정보를 업데이트할 수 있다")
    void testUpdateSpecificInfo() {
        // Given
        when(collectionRepository.findBySessionId("TIC_TEST1234")).thenReturn(Optional.of(testState));
        when(collectionRepository.save(any(TravelInfoCollectionState.class))).thenReturn(testState);
        
        // When
        TravelInfoStatusDto result = service.updateSpecificInfo("TIC_TEST1234", "destination", "부산");
        
        // Then
        assertThat(result).isNotNull();
        assertThat(testState.getDestination()).isEqualTo("부산");
        assertThat(testState.isDestinationCollected()).isTrue();
        verify(collectionRepository).save(testState);
    }
    
    @Test
    @DisplayName("세션을 취소할 수 있다")
    void testCancelCollection() {
        // Given
        when(collectionRepository.findBySessionId("TIC_TEST1234")).thenReturn(Optional.of(testState));
        when(collectionRepository.save(any(TravelInfoCollectionState.class))).thenReturn(testState);
        
        // When
        service.cancelCollection("TIC_TEST1234");
        
        // Then
        assertThat(testState.isCompleted()).isTrue();
        assertThat(testState.getCompletedAt()).isNotNull();
        verify(collectionRepository).save(testState);
    }
    
    @Test
    @DisplayName("존재하지 않는 세션 조회 시 예외가 발생한다")
    void testGetCollectionStatusNotFound() {
        // Given
        when(collectionRepository.findBySessionId("INVALID")).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> 
                service.getCollectionStatus("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("세션을 찾을 수 없습니다: INVALID");
    }
    
    @Test
    @DisplayName("날짜 정보로 기간을 자동 계산한다")
    void testAutoCalculateDuration() {
        // Given
        when(collectionRepository.findBySessionId("TIC_TEST1234")).thenReturn(Optional.of(testState));
        when(collectionRepository.save(any(TravelInfoCollectionState.class))).thenReturn(testState);
        
        // When
        service.updateSpecificInfo("TIC_TEST1234", "startdate", "2024-01-01");
        service.updateSpecificInfo("TIC_TEST1234", "enddate", "2024-01-03");
        
        // Then
        assertThat(testState.getDurationNights()).isEqualTo(2);
        assertThat(testState.isDurationCollected()).isTrue();
    }
}