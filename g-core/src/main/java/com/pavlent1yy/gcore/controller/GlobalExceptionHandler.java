package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.customExceptions.ScheduleNotFoundException;
import com.pavlent1yy.gcore.customExceptions.EmailDeliveryException;
import com.pavlent1yy.gcore.customExceptions.GroupNotFoundException;
import com.pavlent1yy.gcore.customExceptions.InvalidAbsenceException;
import com.pavlent1yy.gcore.customExceptions.PasswordIsIncorrect;
import com.pavlent1yy.gcore.customExceptions.InvalidVerificationTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @ExceptionHandler(ScheduleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleScheduleNotFound(
            ScheduleNotFoundException ex
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidVerificationToken(
            InvalidVerificationTokenException ex
    ) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({PasswordIsIncorrect.class, GroupNotFoundException.class, InvalidAbsenceException.class})
    public ResponseEntity<Map<String, String>> handleBadUserRequest(
            RuntimeException ex
    ) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleGApiError(RestClientResponseException ex) {
        if (ex.getStatusCode().is4xxClientError()) {
            Object error = gApiErrorMessage(ex);
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", error instanceof String text ? text : "Некорректный запрос к расписанию"));
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Сервис расписания ответил ошибкой"));
    }

    private static Object gApiErrorMessage(RestClientResponseException ex) {
        try {
            Map<?, ?> body = JSON.readValue(ex.getResponseBodyAsString(StandardCharsets.UTF_8), Map.class);
            return body == null ? null : body.get("error");
        } catch (RuntimeException e) {
            return null;
        }
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleGApiUnavailable(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Сервис расписания недоступен"));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, String>> handleEmailDelivery(
            EmailDeliveryException ex
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Письмо не удалось отправить. Попробуй позже"));
    }
}
