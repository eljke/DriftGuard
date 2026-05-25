package ru.eljke.driftguard.demo.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.eljke.driftguard.core.error.DriftGuardException;
import ru.eljke.driftguard.demo.error.DemoErrorReason;

import java.time.Instant;

/**
 * English demo documentation.
 */
@RestControllerAdvice
public class DemoExceptionHandler {
    @ExceptionHandler(DriftGuardException.class)
    public ResponseEntity<ApiErrorResponse> driftGuardException(DriftGuardException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> illegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, DemoErrorReason.REQUEST_FAILED.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> generic(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, DemoErrorReason.REQUEST_FAILED.code(), exception.getMessage(), request);
    }

    private static ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI()
        ));
    }
}


