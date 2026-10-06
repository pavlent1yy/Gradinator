package com.pavlent1yy.gcore.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "В ссылке нет токена сброса")
    private String token;

    @NotBlank(message = "Пароль обязателен")
    private String newPassword;

    @NotBlank(message = "Подтверждение пароля обязательно")
    private String confirmPassword;

    @AssertTrue(message = "Пароли не совпадают")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
