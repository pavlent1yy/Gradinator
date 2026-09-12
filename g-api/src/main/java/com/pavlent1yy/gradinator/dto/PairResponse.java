package com.pavlent1yy.gradinator.dto;

import com.pavlent1yy.gradinator.model.CellData;

public record PairResponse(int pairNumber, CellData numerator, CellData denominator, boolean hasChanges) {}
