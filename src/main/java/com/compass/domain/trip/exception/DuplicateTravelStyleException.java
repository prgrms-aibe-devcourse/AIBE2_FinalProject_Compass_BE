package com.compass.domain.trip.exception;

import java.util.List;

/**
 * 중복된 여행 스타일 예외
 */
public class DuplicateTravelStyleException extends RuntimeException {

    private final List<String> duplicateStyles;

    public DuplicateTravelStyleException(List<String> duplicateStyles) {
        super("중복된 여행 스타일이 있습니다: " + String.join(", ", duplicateStyles));
        this.duplicateStyles = duplicateStyles;
    }

    public DuplicateTravelStyleException(String duplicateStyle) {
        this(List.of(duplicateStyle));
    }

    public List<String> getDuplicateStyles() {
        return duplicateStyles;
    }
}
