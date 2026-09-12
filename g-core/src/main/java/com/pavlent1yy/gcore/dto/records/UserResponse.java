package com.pavlent1yy.gcore.dto.records;

import com.pavlent1yy.gcore.enums.Role;

public record UserResponse(
        Long id,
        String email,
        String group,
        String department,
        Role role
) {}
