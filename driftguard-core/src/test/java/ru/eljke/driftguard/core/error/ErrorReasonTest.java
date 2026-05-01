package ru.eljke.driftguard.core.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorReasonTest {
    @Test
    void formatsBracePlaceholders() {
        assertEquals(
                "Duplicate detector algorithm: page-hinkley",
                CoreErrorReason.DUPLICATE_ALGORITHM.format("page-hinkley")
        );
    }

    @Test
    void formatsPrintfPlaceholders() {
        ErrorReason reason = new ErrorReason() {
            @Override
            public String code() {
                return "TEST";
            }

            @Override
            public String description() {
                return "Field %s is invalid";
            }
        };

        assertEquals("Field delta is invalid", reason.format("delta"));
    }
}
