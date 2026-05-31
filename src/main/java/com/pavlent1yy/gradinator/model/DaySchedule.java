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
        this.pairs.add(pair);
    }
}
