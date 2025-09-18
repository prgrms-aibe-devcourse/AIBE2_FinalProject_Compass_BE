package com.compass.domain.collection.service;

import com.compass.domain.collection.dto.TravelInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// 구조화된 폼 제출을 통해 여행 정보를 수집하는 구현체
// 요구사항 2.3.1: TravelInfoCollector의 구현체로서, 제출된 폼 데이터를 TravelInfo 객체로 변환.
@Component("formBasedCollector")
public class FormBasedCollector implements TravelInfoCollector {

    // 폼 제출 데이터를 담는 내부 DTO
    public record FormData(
            List<String> destinations,
            LocalDate startDate,
            LocalDate endDate,
            Integer budget,
            String companions,
            String travelStyle
    ) {}

    /**
     * 제출된 폼 데이터(FormData)를 TravelInfo 객체로 변환합니다.
     * @param input FormData 타입의 객체여야 합니다.
     * @return 변환된 TravelInfo 객체
     * @throws IllegalArgumentException 입력이 FormData 타입이 아닐 경우 발생
     */
    @Override
    public TravelInfo collect(Object input) {
        if (!(input instanceof FormData formData)) {
            // 입력 타입이 올바르지 않을 경우, 명확한 예외를 발생시킵니다.
            throw new IllegalArgumentException("Input for FormBasedCollector must be of type FormData");
        }

        // FormData의 필드를 사용하여 TravelInfo 객체를 생성하고 반환합니다.
        // record의 불변성을 활용하여 간단하게 새 객체를 생성합니다.
        return new TravelInfo(
                formData.destinations(),
                formData.startDate(),
                formData.endDate(),
                formData.budget(),
                formData.companions(),
                formData.travelStyle()
        );
    }
}
