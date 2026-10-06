package com.pavlent1yy.gcore.controller;

import com.pavlent1yy.gcore.customExceptions.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new com.pavlent1yy.gcore.service.AuthCookieService());

    @Test
    void scheduleNotFoundIs404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleScheduleNotFound(new ScheduleNotFoundException("нет данных"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("error", "нет данных");
    }

    @Test
    void invalidVerificationTokenIs400() {
        ResponseEntity<Map<String, String>> response =
                handler.handleInvalidVerificationToken(new InvalidVerificationTokenException("истёк"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "истёк");
    }

    @Test
    void badUserRequestsAre400WithMessage() {
        assertThat(handler.handleBadUserRequest(new PasswordIsIncorrect("неверный")).getBody())
                .containsEntry("error", "неверный");
        assertThat(handler.handleBadUserRequest(new GroupNotFoundException("нет группы")).getStatusCode().value())
                .isEqualTo(400);
    }

    @Test
    void emailDeliveryIs503WithoutInternalDetails() {
        ResponseEntity<Map<String, String>> response = handler.handleEmailDelivery(
                new EmailDeliveryException("smtp password wrong", new RuntimeException()));

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().get("error")).doesNotContain("smtp");
    }
}
