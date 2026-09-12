package com.pavlent1yy.gcore.dto.records;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}
