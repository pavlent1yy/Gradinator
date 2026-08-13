package com.pavlent1yy.gcore.dto;

public record PairResponse(
        int pairNumber,
        CellData numerator,
        CellData denominator,
        boolean hasChanges
) {}