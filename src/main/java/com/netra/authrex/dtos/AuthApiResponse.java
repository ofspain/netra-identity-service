package com.netra.authrex.dtos;

import java.time.LocalDateTime;

public record AuthApiResponse<T>(
        boolean success,
        String message,
        LocalDateTime timestamp,
        T data
) {
    public static <T> AuthApiResponse<T> success(T data, String message) {
        return new AuthApiResponse<>(true, message, LocalDateTime.now(), data);
    }
}
