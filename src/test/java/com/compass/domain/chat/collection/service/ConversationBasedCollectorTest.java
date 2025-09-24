package com.compass.domain.chat.collection.service;

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
        // given
        String userInput = "제주도로 갈래요";
        // ✅ 수정: 생성자에 null 2개 추가
        var currentInfo = new TravelFormSubmitRequest("user-123", null, null, null, null, null, null, null, null, null);
        var expectedParsedInfo = new TravelFormSubmitRequest("user-123", List.of("제주도"), null, null, null, null, null, null, null, null);

        when(parser.parse(userInput, currentInfo)).thenReturn(expectedParsedInfo);

        // when
        TravelFormSubmitRequest result = conversationBasedCollector.collect(userInput, currentInfo);

        // then
        verify(parser).parse(userInput, currentInfo);
        assertThat(result).isSameAs(expectedParsedInfo);
    }

    @Test
    @DisplayName("validate - TravelInfoValidator의 validate 메서드를 정확히 호출하여 책임을 위임한다")
    void validate_shouldDelegateToValidator() {
        // given
        // ✅ 수정: 생성자에 null 2개 추가
        var info = new TravelFormSubmitRequest("user-123", List.of("부산"), null, null, null, null, null, null, null, null);

        // when
        conversationBasedCollector.validate(info);

        // then
        verify(validator).validate(info);
    }
}