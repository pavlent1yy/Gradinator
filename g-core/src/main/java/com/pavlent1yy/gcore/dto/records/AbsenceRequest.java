package com.pavlent1yy.gcore.dto.records;

import com.pavlent1yy.gcore.enums.AbsenceType;

import java.time.LocalDate;

public record AbsenceRequest(
        LocalDate date,
        Integer pairNumber,
        AbsenceType type,
        String subject
) {}
