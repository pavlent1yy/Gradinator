package com.pavlent1yy.gradinator.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

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


}
