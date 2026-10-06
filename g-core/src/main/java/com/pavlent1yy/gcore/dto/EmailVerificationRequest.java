package com.pavlent1yy.gcore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationRequest {
    @NotBlank(message = "В ссылке нет токена подтверждения")
    private String token;
}
