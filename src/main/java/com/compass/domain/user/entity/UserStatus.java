package com.compass.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    DELETED("DELETED");

    private final String status;
}