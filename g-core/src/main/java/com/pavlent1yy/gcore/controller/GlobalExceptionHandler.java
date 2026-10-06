package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.customExceptions.EmailNotVerifiedException;
import com.pavlent1yy.gcore.customExceptions.InvalidRefreshTokenException;
import com.pavlent1yy.gcore.customExceptions.ScheduleNotFoundException;
import com.pavlent1yy.gcore.customExceptions.UserAlreadyExistsException;
import com.pavlent1yy.gcore.service.AuthCookieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.pavlent1yy.gcore.customExceptions.EmailDeliveryException;
import com.pavlent1yy.gcore.customExceptions.GroupNotFoundException;
import com.pavlent1yy.gcore.customExceptions.InvalidAbsenceException;
import com.pavlent1yy.gcore.customExceptions.InvalidResetTokenException;
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
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AuthCookieService authCookieService;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @ExceptionHandler(ScheduleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleScheduleNotFound(
            ScheduleNotFoundException ex
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Неверный email или пароль"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, String>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", ex.getMessage(), "code", "EMAIL_NOT_VERIFIED"));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearRefreshCookie().toString())
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError field && field.getDefaultMessage() != null
                        ? field.getDefaultMessage()
                        : error.getDefaultMessage())
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse("Некорректные данные");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidVerificationToken(
            InvalidVerificationTokenException ex
    ) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler({PasswordIsIncorrect.class, GroupNotFoundException.class, InvalidAbsenceException.class, InvalidResetTokenException.class})
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
