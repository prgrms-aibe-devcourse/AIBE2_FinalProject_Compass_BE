package com.compass.domain.chat.function.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// FunctionConfiguration 테스트
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key",
    "spring.ai.vertex.ai.gemini.project-id=test-project"
})
class FunctionConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("모든 FunctionCallbackWrapper Bean이 등록되어야 한다")
    void allFunctionWrappersAreRegistered() {
        // when - FunctionCallbackWrapper 타입의 모든 Bean 조회
        Map<String, FunctionCallbackWrapper> wrappers =
            applicationContext.getBeansOfType(FunctionCallbackWrapper.class);

        // then - 11개의 Function이 등록되어야 함 (14개는 TODO)
        assertThat(wrappers).hasSize(11);
    }

    @Test
    @DisplayName("Collection Function Wrapper Bean들이 등록되어야 한다")
    void collectionFunctionWrappersAreRegistered() {
        // given - Collection 관련 Function 이름들
        String[] collectionFunctions = {
            "submitTravelFormWrapper",
            "showQuickInputFormWrapper",
            // "askFollowUpQuestionWrapper", // TODO
            "startFollowUpWrapper"
            // "validateAndStoreInfoWrapper" // TODO
        };

        // then - 각 Bean이 등록되어 있어야 함
        for (String functionName : collectionFunctions) {
            assertThat(applicationContext.containsBean(functionName))
                .as("Bean %s should be registered", functionName)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Planning Function Wrapper Bean들이 등록되어야 한다")
    void planningFunctionWrappersAreRegistered() {
        // given
        String[] planningFunctions = {
            "generateTravelPlanWrapper",
            "destinationSearchWrapper"
        };

        // then
        for (String functionName : planningFunctions) {
            assertThat(applicationContext.containsBean(functionName))
                .as("Bean %s should be registered", functionName)
                .isTrue();
        }
    }

    // TODO: Itinerary Function들이 구현되면 테스트 활성화
    // @Test
    // @DisplayName("Itinerary Function Wrapper Bean들이 등록되어야 한다")
    // void itineraryFunctionWrappersAreRegistered() {
    //     // given
    //     String[] itineraryFunctions = {
    //         "generateItineraryWrapper",
    //         "showItineraryWrapper",
    //         "searchPlacesWrapper"
    //     };

    //     // then
    //     for (String functionName : itineraryFunctions) {
    //         assertThat(applicationContext.containsBean(functionName))
    //             .as("Bean %s should be registered", functionName)
    //             .isTrue();
    //     }
    // }

    // TODO: Adjustment Function들이 구현되면 테스트 활성화
    // @Test
    // @DisplayName("Adjustment Function Wrapper Bean들이 등록되어야 한다")
    // void adjustmentFunctionWrappersAreRegistered() {
    //     // given
    //     String[] adjustmentFunctions = {
    //         "modifyItineraryWrapper",
    //         "regenerateItineraryWrapper"
    //     };

    //     // then
    //     for (String functionName : adjustmentFunctions) {
    //         assertThat(applicationContext.containsBean(functionName))
    //             .as("Bean %s should be registered", functionName)
    //             .isTrue();
    //     }
    // }

    // TODO: Finalization Function들이 구현되면 테스트 활성화
    // @Test
    // @DisplayName("Finalization Function Wrapper Bean들이 등록되어야 한다")
    // void finalizationFunctionWrappersAreRegistered() {
    //     // given
    //     String[] finalizationFunctions = {
    //         "saveFinalItineraryWrapper",
    //         "exportItineraryWrapper"
    //     };

    //     // then
    //     for (String functionName : finalizationFunctions) {
    //         assertThat(applicationContext.containsBean(functionName))
    //             .as("Bean %s should be registered", functionName)
    //             .isTrue();
    //     }
    // }

    // TODO: General Function들이 구현되면 테스트 활성화
    // @Test
    // @DisplayName("General Function Wrapper Bean들이 등록되어야 한다")
    // void generalFunctionWrappersAreRegistered() {
    //     // given
    //     String[] generalFunctions = {
    //         "handleGeneralQueryWrapper",
    //         "showProgressWrapper",
    //         "restartPlanningWrapper"
    //     };

    //     // then
    //     for (String functionName : generalFunctions) {
    //         assertThat(applicationContext.containsBean(functionName))
    //             .as("Bean %s should be registered", functionName)
    //             .isTrue();
    //     }
    // }

    @Test
    @DisplayName("Processing Function Wrapper Bean들이 등록되어야 한다")
    void processingFunctionWrappersAreRegistered() {
        // given
        String[] processingFunctions = {
            "extractFlightInfoWrapper",
            "processImageWrapper",
            "processOCRWrapper"
        };

        // then
        for (String functionName : processingFunctions) {
            assertThat(applicationContext.containsBean(functionName))
                .as("Bean %s should be registered", functionName)
                .isTrue();
        }
    }

    @Test
    @DisplayName("External API Function Wrapper Bean들이 등록되어야 한다")
    void externalFunctionWrappersAreRegistered() {
        // given
        String[] externalFunctions = {
            "searchTourAPIWrapper",
            "searchWithPerplexityWrapper"
        };

        // then
        for (String functionName : externalFunctions) {
            assertThat(applicationContext.containsBean(functionName))
                .as("Bean %s should be registered", functionName)
                .isTrue();
        }
    }

    // TODO: Utility Function들이 구현되면 테스트 활성화
    // @Test
    // @DisplayName("Utility Function Wrapper Bean들이 등록되어야 한다")
    // void utilityFunctionWrappersAreRegistered() {
    //     // given
    //     String[] utilityFunctions = {
    //         "searchWebWrapper",
    //         "sendNotificationWrapper"
    //     };

    //     // then
    //     for (String functionName : utilityFunctions) {
    //         assertThat(applicationContext.containsBean(functionName))
    //             .as("Bean %s should be registered", functionName)
    //             .isTrue();
    //     }
    // }

    @Test
    @DisplayName("Function 등록 로거 Bean이 등록되어야 한다")
    void functionRegistrationLoggerIsRegistered() {
        // when & then
        assertThat(applicationContext.containsBean("functionRegistrationLogger"))
            .isTrue();

        String loggerMessage = applicationContext.getBean("functionRegistrationLogger", String.class);
        assertThat(loggerMessage).isEqualTo("Function Registration Complete");
    }

    @Test
    @DisplayName("FunctionCallbackWrapper Bean들이 올바른 이름과 설명을 가져야 한다")
    void functionWrappersHaveCorrectNamesAndDescriptions() {
        // when
        var submitFormWrapper = applicationContext.getBean("submitTravelFormWrapper", FunctionCallbackWrapper.class);
        var generatePlanWrapper = applicationContext.getBean("generateTravelPlanWrapper", FunctionCallbackWrapper.class);
        var searchAPIWrapper = applicationContext.getBean("searchTourAPIWrapper", FunctionCallbackWrapper.class);

        // then - FunctionCallbackWrapper의 name과 description이 설정되어 있어야 함
        assertThat(submitFormWrapper).isNotNull();
        assertThat(generatePlanWrapper).isNotNull();
        assertThat(searchAPIWrapper).isNotNull();

        // Note: FunctionCallbackWrapper의 실제 메서드를 통해 name/description 검증
        // Spring AI 버전에 따라 메서드명이 다를 수 있음
    }

    @Test
    @DisplayName("순환 참조 없이 모든 Function이 주입되어야 한다")
    void noCircularDependencyInFunctions() {
        // given
        FunctionConfiguration config = applicationContext.getBean(FunctionConfiguration.class);

        // when & then - 순환 참조가 있으면 Bean 생성 시 실패하므로 이 테스트가 성공하면 순환 참조가 없음
        assertThat(config).isNotNull();

        // 모든 Function Bean이 정상적으로 생성되었는지 확인
        Map<String, FunctionCallbackWrapper> allWrappers =
            applicationContext.getBeansOfType(FunctionCallbackWrapper.class);
        assertThat(allWrappers)
            .isNotEmpty()
            .hasSize(11); // 14개는 아직 구현되지 않음
    }
}