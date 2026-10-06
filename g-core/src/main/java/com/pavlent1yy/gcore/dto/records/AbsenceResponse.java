package com.pavlent1yy.gcore.dto.records;

import com.pavlent1yy.gcore.enums.AbsenceType;

import java.time.LocalDate;

public record AbsenceResponse(
        Long id,
        LocalDate date,
        int pairNumber,
        AbsenceType type,
        String subject,
        int hours
) {}
