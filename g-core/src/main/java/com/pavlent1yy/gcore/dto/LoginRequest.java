package com.pavlent1yy.gcore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @Email(message = "Некорректный email")
    @NotBlank(message = "Укажи email")
    private String email;

    @NotBlank(message = "Укажи пароль")
    private String password;
}
