package com.pavlent1yy.gcore.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}
