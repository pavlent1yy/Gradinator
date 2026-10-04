package com.pavlent1yy.gcore.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static RegisterRequest register(String email, String password, String confirm) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);
        request.setConfirmPassword(confirm);
        return request;
    }

    private static <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void validRegistrationPasses() {
        assertThat(validator.validate(register("a@mail.ru", "secret", "secret"))).isEmpty();
    }

    @Test
    void registrationChecksEmailAndPasswordMatch() {
        assertThat(messages(validator.validate(register("not-email", "secret", "other"))))
                .containsExactlyInAnyOrder("Некорректный email", "Пароли не совпадают");
    }

    @Test
    void registrationRequiresAllFields() {
        assertThat(messages(validator.validate(register(" ", null, null))))
                .contains("Email обязателен", "Пароль обязателен", "Подтверждение пароля обязательно");
    }

    @Test
    void loginRequiresValidEmailAndPassword() {
        LoginRequest login = new LoginRequest();
        login.setEmail("bad");
        login.setPassword("");

        assertThat(validator.validate(login)).hasSize(2);
    }

    @Test
    void verificationAndResendRequests() {
        EmailVerificationRequest verify = new EmailVerificationRequest();
        verify.setToken(" ");
        ResendVerificationRequest resend = new ResendVerificationRequest();
        resend.setEmail("a@mail.ru");

        assertThat(validator.validate(verify)).hasSize(1);
        assertThat(validator.validate(resend)).isEmpty();
    }
}
