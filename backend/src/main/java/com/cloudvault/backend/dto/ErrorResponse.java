package com.cloudvault.backend.dto;

import java.time.Instant;

/**
 * Consistent error response body returned by all exception handlers.
 */
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {

    /**
     * Factory method for creating an ErrorResponse with the current timestamp.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(
                Instant.now().toString(),
                status,
                error,
                message,
                path
        );
    }
}
