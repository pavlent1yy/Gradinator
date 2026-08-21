package com.pavlent1yy.gcore.dto;

import com.pavlent1yy.gcore.enums.Role;

public record UserResponse(
        Long id,
        String email,
        Role role
) {}
