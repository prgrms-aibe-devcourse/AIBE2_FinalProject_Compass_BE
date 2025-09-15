package com.compass.domain.user.dto;

import com.compass.domain.user.entity.UserFeedback;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserFeedbackDto {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "Satisfaction score is required.")
        @Min(value = 1, message = "Satisfaction must be at least 1.")
        @Max(value = 5, message = "Satisfaction must be at most 5.")
        private Integer satisfaction;

        @Size(max = 1000, message = "Comment must be less than 1000 characters.")
        private String comment;

        private Boolean revisitIntent;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String message;
    }
}