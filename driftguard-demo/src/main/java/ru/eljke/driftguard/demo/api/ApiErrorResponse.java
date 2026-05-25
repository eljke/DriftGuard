package ru.eljke.driftguard.demo.api;

import java.time.Instant;

/**
 * English demo documentation.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
}


