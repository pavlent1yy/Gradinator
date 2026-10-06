package com.pavlent1yy.gcore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    @Email(message = "Некорректный email")
    @NotBlank(message = "Email обязателен")
    private String email;
}
