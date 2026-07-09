package com.pavlent1yy.gradinator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Changes {
    LocalDate date;
    List<PairSlot> changedPairs;


    @Override
    public String toString() {
        return "Changes: " +
                "date:" + date + "\nchangedPairs=" + changedPairs + '}';
    }
}
