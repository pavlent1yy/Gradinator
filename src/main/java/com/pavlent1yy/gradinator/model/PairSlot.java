package com.pavlent1yy.gradinator.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PairSlot {
    private int pairNumber;
    private CellData numerator;
    private CellData denominator;
}
