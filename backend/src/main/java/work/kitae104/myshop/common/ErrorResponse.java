package work.kitae104.myshop.common;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(int status, String message, Map<String, String> errors, Instant timestamp) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Map.of(), Instant.now());
    }

    public static ErrorResponse of(int status, String message, Map<String, String> errors) {
        return new ErrorResponse(status, message, errors, Instant.now());
    }
}
