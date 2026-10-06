package com.pavlent1yy.gcore.service;

import com.pavlent1yy.gcore.dto.records.CellData;
import com.pavlent1yy.gcore.dto.records.PairResponse;
import com.pavlent1yy.gcore.enums.WeekType;

import java.util.Locale;

final class ScheduleCells {

    private ScheduleCells() {
    }

    static CellData activeCell(PairResponse pair, WeekType weekType) {
        if (weekType == WeekType.DENOMINATOR && isFilled(pair.denominator())) {
            return pair.denominator();
        }
        return isFilled(pair.numerator()) ? pair.numerator() : null;
    }

    static boolean isCancelled(CellData cell) {
        return cell != null
                && cell.subjects() != null
                && !cell.subjects().isEmpty()
                && cell.subjects().stream().allMatch(ScheduleCells::isCancelledSubject);
    }

    private static boolean isCancelledSubject(String subject) {
        String cleaned = subject == null
                ? ""
                : subject.replaceAll("[\\s❗❕!]+", "").toLowerCase(Locale.ROOT);
        return cleaned.equals("снято") || cleaned.equals("снята");
    }

    private static boolean isFilled(CellData cell) {
        return cell != null && cell.subjects() != null && !cell.isEmpty();
    }
}
