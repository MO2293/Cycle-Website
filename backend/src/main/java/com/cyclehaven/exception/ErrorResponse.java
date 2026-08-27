package com.cyclehaven.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error body for every failed request.
 *
 * @param fieldErrors per-field validation messages; null unless the failure was a
 *                    bean-validation error.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse validation(int status, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, "Validation Failed", message, path, fieldErrors);
    }
}
