package com.compass.domain.user.dto;

import com.compass.domain.user.entity.UserPreference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

public class UserPreferenceDto {

    @Getter
    public static class UpdateRequest {
        @Valid
        private List<PreferenceItem> preferences;
    }

    @Getter
    public static class PreferenceItem {
        @NotBlank
        private String key;
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        private BigDecimal value;
    }

    @Getter @Builder
    public static class Response {
        private String preferenceKey;
        private BigDecimal preferenceValue;

        public static Response from(UserPreference preference) {
            return Response.builder()
                    .preferenceKey(preference.getPreferenceKey())
                    .preferenceValue(preference.getPreferenceValue())
                    .build();
        }
    }
}