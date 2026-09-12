package com.pavlent1yy.gradinator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PairSlot {
    private int pairNumber;
    private CellData numerator;
    private CellData denominator;
}
