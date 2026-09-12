package com.pavlent1yy.gradinator.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DaySchedule {
    private String day;
    private List<PairSlot> pairs = new ArrayList<>();

    public DaySchedule(String day) {
        this.day = day;
    }

    public void addPair(PairSlot pair) {
        for (PairSlot existing : pairs) {
            if (existing.getPairNumber() == pair.getPairNumber()) {
                if (existing.getNumerator().isEmpty() && !pair.getNumerator().isEmpty()) {
                    existing.setNumerator(pair.getNumerator());
                }
                if (existing.getDenominator() == null && pair.getDenominator() != null) {
                    existing.setDenominator(pair.getDenominator());
                }
                return;
            }
        }
        pairs.add(pair);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(day).append(" ===\n");

        for (PairSlot pair : pairs) {
            sb.append("  [")
                    .append(pair.getPairNumber())
                    .append("] ");

            if (pair.getNumerator() != null) {
                sb.append("Ч: ").append(pair.getNumerator());
            }

            if (pair.getDenominator() != null) {
                sb.append(" | З: ").append(pair.getDenominator());
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
