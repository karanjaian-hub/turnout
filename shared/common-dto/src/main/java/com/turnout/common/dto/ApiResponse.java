package com.turnout.common.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(1, message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(1, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(0, message, null);
    }
}
