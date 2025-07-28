package com.netra.authrex.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.ZonedDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        List<Error> errors,
        Meta meta
) {
    public static <T> ApiResponse<T> success(T data, String message, String path, String traceId) {
        return new ApiResponse<>(
                true,
                "00",
                message,
                data,
                null,
                new Meta(ZonedDateTime.now(), path, traceId)
        );
    }

    public static <T> ApiResponse<T> error(String code, String message, List<Error> errors, String path, String traceId) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                errors,
                new Meta(ZonedDateTime.now(), path, traceId)
        );
    }

    public record Meta(
            ZonedDateTime timestamp,
            String path,
            String traceId
    ) {}
}
