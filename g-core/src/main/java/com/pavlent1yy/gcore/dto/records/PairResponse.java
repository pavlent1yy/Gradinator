package com.pavlent1yy.gcore.dto.records;

public record PairResponse(
        int pairNumber,
        CellData numerator,
        CellData denominator,
        boolean hasChanges
) {}