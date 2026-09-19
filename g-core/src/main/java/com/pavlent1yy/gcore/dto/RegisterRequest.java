package com.pavlent1yy.gcore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @Email(message = "Некорректный email")
    @NotBlank(message = "Email обязателен")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;

    @NotBlank(message = "Подтверждение пароля обязательно")
    private String confirmPassword;

    private String group;

    private String department;

    @AssertTrue(message = "Пароли не совпадают")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}
