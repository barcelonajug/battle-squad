package org.barcelonajug.battlecontender.controller;

import com.openai.errors.RateLimitException;
import com.openai.errors.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final JsonMapper JSON = new JsonMapper();

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, String>> handleClientError(HttpClientErrorException ex) {
        log.warn("Client error touching Arena API: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        String detail = arenaErrorDetail(ex.getResponseBodyAsString());
        if (detail.toLowerCase(Locale.ROOT).contains("already submitted")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Already submitted", "detail", detail, "code", "ALREADY_SUBMITTED"));
        }
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", "Arena request failed", "detail", detail));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, String>> handleServerError(HttpServerErrorException ex) {
        log.error("Server error from Arena API: {}", ex.getResponseBodyAsString());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "The upstream Arena Server encountered an error."));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleArenaConnectionError(ResourceAccessException ex) {
        log.error("Unable to reach Arena API", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Arena unavailable", "detail", "Could not reach the Arena. Please try again."));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleOpenAiAuthentication(UnauthorizedException ex) {
        log.error("OpenAI rejected the configured API key");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "OpenAI authentication failed",
                        "detail", "OpenAI rejected the configured API key. Check OPENAI_API_KEY and restart the app."));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> handleOpenAiRateLimit(RateLimitException ex) {
        log.warn("OpenAI rate limit reached");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "OpenAI rate limited",
                        "detail", "OpenAI is rate limiting requests. Please try again later."));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> handleTodo(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "Not Implemented", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Invalid squad", "detail", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unexpected error", "detail", "The request failed. Check the server logs for details."));
    }

    private String arenaErrorDetail(String responseBody) {
        try {
            JsonNode body = JSON.readTree(responseBody);
            String detail = body.path("detail").asString("");
            if (!detail.isBlank()) {
                return detail;
            }
        } catch (JacksonException ex) {
            log.debug("Arena error body is not JSON", ex);
        }
        return "The Arena rejected the request.";
    }
}
