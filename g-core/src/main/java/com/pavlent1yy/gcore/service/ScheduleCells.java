package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.dto.records.CellData;
import com.pavlent1yy.gcore.dto.records.PairResponse;
import com.pavlent1yy.gcore.enums.WeekType;

final class ScheduleCells {

    private ScheduleCells() {
    }

    static CellData activeCell(PairResponse pair, WeekType weekType) {
        if (weekType == WeekType.DENOMINATOR && isFilled(pair.denominator())) {
            return pair.denominator();
        }
        return isFilled(pair.numerator()) ? pair.numerator() : null;
    }

    private static boolean isFilled(CellData cell) {
        return cell != null && cell.subjects() != null && !cell.isEmpty();
    }
}
