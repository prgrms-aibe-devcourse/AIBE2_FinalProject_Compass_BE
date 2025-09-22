package com.compass.domain.chat.collection.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CollectorFactoryTest {

    @Mock
    private FormBasedCollector formBasedCollector;
    @Mock
    private ConversationBasedCollector conversationBasedCollector;

    private CollectorFactory collectorFactory;

    @BeforeEach
    void setUp() {
        // 실제 Spring의 동작을 모방하여 Bean 이름과 객체를 매핑
        Map<String, TravelInfoCollector> collectors = Map.of(
                "formBasedCollector", formBasedCollector,
                "conversationBasedCollector", conversationBasedCollector
        );
        collectorFactory = new CollectorFactory(collectors);
    }

    @Test
    @DisplayName("getCollector - 'form' 타입이 주어지면 FormBasedCollector를 반환한다")
    void getCollector_shouldReturnFormBasedCollector_forTypeForm() {
        // when
        TravelInfoCollector collector = collectorFactory.getCollector("form");
        // then
        assertThat(collector).isInstanceOf(FormBasedCollector.class);
    }

    @Test
    @DisplayName("getCollector - 'conversation' 타입이 주어지면 ConversationBasedCollector를 반환한다")
    void getCollector_shouldReturnConversationBasedCollector_forTypeConversation() {
        // when
        TravelInfoCollector collector = collectorFactory.getCollector("conversation");
        // then
        assertThat(collector).isInstanceOf(ConversationBasedCollector.class);
    }

    @Test
    @DisplayName("getCollector - 지원하지 않는 타입이 주어지면 예외를 발생시킨다")
    void getCollector_shouldThrowException_forUnsupportedType() {
        // when & then
        assertThatThrownBy(() -> collectorFactory.getCollector("unsupported"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 수집기 타입입니다: unsupported");
    }
}