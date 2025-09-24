package com.compass.domain.chat.collection.service; // 이 패키지 경로가 올바른지 다시 한번 확인해주세요.

import com.compass.domain.chat.collection.service.validator.TravelInfoValidator;
import com.compass.domain.chat.model.request.TravelFormSubmitRequest;
import com.compass.domain.chat.parser.service.TravelInfoParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationBasedCollectorTest {

    @Mock
    private TravelInfoParser parser;

    @Mock
    private TravelInfoValidator validator;

    private ConversationBasedCollector conversationBasedCollector;

    @BeforeEach
    void setUp() {
        conversationBasedCollector = new ConversationBasedCollector(parser, validator);
    }

    @Test
    @DisplayName("collect - TravelInfoParser의 parse 메서드를 정확히 호출하여 책임을 위임한다")
    void collect_shouldDelegateToParser() {
        // given: 사용자 입력과 현재 정보
        String userInput = "제주도로 갈래요";
        var currentInfo = new TravelFormSubmitRequest("user-123", null, null, null, null, null, null, null);
        var expectedParsedInfo = new TravelFormSubmitRequest("user-123", List.of("제주도"), null, null, null, null, null, null);

        // Mocking: parser.parse()가 호출되면, 예상되는 파싱 결과를 반환하도록 설정
        when(parser.parse(userInput, currentInfo)).thenReturn(expectedParsedInfo);

        // when: collect 메서드 호출
        TravelFormSubmitRequest result = conversationBasedCollector.collect(userInput, currentInfo);

        // then: parser.parse()가 호출되었고, 그 결과가 그대로 반환되었는지 검증
        verify(parser).parse(userInput, currentInfo);
        assertThat(result).isSameAs(expectedParsedInfo);
    }

    @Test
    @DisplayName("validate - TravelInfoValidator의 validate 메서드를 정확히 호출하여 책임을 위임한다")
    void validate_shouldDelegateToValidator() {
        // given: 검증할 여행 정보 객체
        var info = new TravelFormSubmitRequest("user-123", List.of("부산"), null, null, null, null, null, null);

        // when: validate 메서드 호출
        conversationBasedCollector.validate(info);

        // then: validator.validate()가 정확히 1번, 올바른 인자와 함께 호출되었는지 검증
        verify(validator).validate(info);
    }
}