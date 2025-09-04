package com.netra.authrex.dtos;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record AuthApiResponse<T>(
        boolean success,
        String message,
        LocalDateTime timestamp,
        T data
) {
    public static <T> AuthApiResponse<T> success(T data, String message) {
        return new AuthApiResponse<>(true, message, LocalDateTime.now(), data);
    }

//    public static <T> AuthApiResponse<T> success(T data, String message, String path, String traceId) {
//        return new AuthApiResponse<>(
//                true,
//                message,
//                LocalDateTime.now(),
//                data
//                );
//    }
}
